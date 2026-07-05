package com.velet.wallet.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velet.wallet.dto.cache.BalanceCounter;
import com.velet.wallet.dto.cache.ReservationRecord;
import com.velet.wallet.dto.response.WalletInfo;
import com.velet.wallet.exception.AppException;
import com.velet.wallet.exception.ErrorCode;
import com.velet.wallet.utils.RedisHashCodec;
import com.velet.wallet.models.BalanceComponents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class WalletCacheRepositoryImpl implements WalletCacheRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> hashRedisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisScript<Long> walletIncrementCounterScript;
    private final RedisHashCodec redisHashCodec;

    private static final Duration LOCK_TTL        = Duration.ofSeconds(10);
    private static final Duration ACCOUNT_TTL     = Duration.ofMinutes(5);
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(5);
    private static final Duration BALANCE_TTL     = Duration.ofMinutes(5);

    private static final String LOCK_PREFIX        = "lock:wallet:";
    private static final String ACCOUNT_PREFIX     = "wallet:";
    private static final String BALANCE_PREFIX     = "wallet:balance:";
    private static final String RESERVATION_PREFIX = "wallet:reserve:idempotency:";

    private static final String FIELD_POSTED_DEBITS   = "posted_debits";
    private static final String FIELD_POSTED_CREDITS  = "posted_credits";
    private static final String FIELD_PENDING_DEBITS  = "pending_debits";
    private static final String FIELD_PENDING_CREDITS = "pending_credits";

    @Override
    public Optional<WalletInfo> findAccount(String walletId) {
        Map<Object, Object> walletMap = hashRedisTemplate.opsForHash().entries(ACCOUNT_PREFIX + walletId);
        if (walletMap == null || walletMap.isEmpty()) return Optional.empty();
        try {
            return Optional.of(redisHashCodec.fromHash(walletMap, WalletInfo.class));
        } catch (Exception e) {
            log.error("cache.deserialize.failed walletId={}", walletId, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void saveAccount(String walletId, WalletInfo wallet) {
        Map<String, String> hash = redisHashCodec.toHash(wallet);
        hashRedisTemplate.opsForHash().putAll(ACCOUNT_PREFIX + walletId, hash);
        hashRedisTemplate.expire(ACCOUNT_PREFIX + walletId, ACCOUNT_TTL);
    }

    @Override
    public void evictAccount(String walletId) {
        hashRedisTemplate.delete(ACCOUNT_PREFIX + walletId);
    }

    @Override
    public boolean acquireLock(String walletId) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                                              .setIfAbsent(LOCK_PREFIX + walletId, "1", LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseLock(String walletId) {
        stringRedisTemplate.delete(LOCK_PREFIX + walletId);
    }

    @Override
    public void increaseWalletBalance(String walletId, String field, BigDecimal amount) {
        String key = ACCOUNT_PREFIX + walletId;

        hashRedisTemplate.opsForHash().increment(key, field, amount.longValueExact());
        hashRedisTemplate.expire(key, ACCOUNT_TTL);
    }

    @Override
    public void incrementCounter(String walletId, String field, long delta) {
        hashRedisTemplate.execute(
                walletIncrementCounterScript,
                Collections.singletonList(BALANCE_PREFIX + walletId),
                field,
                String.valueOf(delta),
                String.valueOf(BALANCE_TTL.getSeconds())
        );
    }

    @Override
    public Optional<BalanceComponents> getCounters(String walletId) {
        String key = BALANCE_PREFIX + walletId;
        List<Object> values = hashRedisTemplate.opsForHash().multiGet(
                key,
                List.of(FIELD_POSTED_DEBITS, FIELD_POSTED_CREDITS, FIELD_PENDING_DEBITS, FIELD_PENDING_CREDITS)
        );
        if (values == null || values.stream().allMatch(v -> v == null)) return Optional.empty();

        return Optional.of(new BalanceComponents(
                toLong(values.get(0)),
                toLong(values.get(1)),
                toLong(values.get(2)),
                toLong(values.get(3))
        ));
    }

    @Override
    public void saveCounters(String walletId, BalanceCounter components) {
        String key = BALANCE_PREFIX + walletId;
        hashRedisTemplate.opsForHash().putAll(key, redisHashCodec.toHash(components));
        hashRedisTemplate.expire(key, BALANCE_TTL);
    }

    @Override
    public boolean reserve(String walletId, BigDecimal amount) {
        String key = ACCOUNT_PREFIX + walletId;
        long amountCents = amount.longValueExact();

        Object availableBalanceObj = hashRedisTemplate.opsForHash().get(key, "availableBalance");
        if (availableBalanceObj == null) throw new AppException(ErrorCode.WALLET_CACHE_MISS);

        long available = ((Number) availableBalanceObj).longValue();
        if (available < amountCents) return false;

        hashRedisTemplate.opsForHash().increment(key, "availableBalance", -amountCents);
        hashRedisTemplate.opsForHash().increment(key, "pendingBalance", amountCents);
        return true;
    }

    @Override
    public void release(String walletId, BigDecimal amount) {
        String key = ACCOUNT_PREFIX + walletId;
        long amountCents = amount.longValueExact();
        hashRedisTemplate.opsForHash().increment(key, "availableBalance", amountCents);
        hashRedisTemplate.opsForHash().increment(key, "pendingBalance", -amountCents);
    }

    @Override
    public Optional<ReservationRecord> getReservationRecord(String idempotencyKey) {
        String key = RESERVATION_PREFIX + idempotencyKey;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, ReservationRecord.class));
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void saveReservationRecord(String idempotencyKey, ReservationRecord record) {
        String key = RESERVATION_PREFIX + idempotencyKey;
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(record), RESERVATION_TTL);
        } catch (JsonProcessingException e) {
            log.error("cache.serialize.failed idempotencyKey={}", idempotencyKey, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateReservationRecordStatus(String idempotencyKey, String newStatus) {
        String key = RESERVATION_PREFIX + idempotencyKey;
        Optional<ReservationRecord> existing = getReservationRecord(idempotencyKey);
        if (existing.isEmpty()) return;

        ReservationRecord record = existing.get();
        ReservationRecord updated = new ReservationRecord(
                newStatus, record.walletId(), record.amount(),
                record.reservedAt(), Instant.now().toEpochMilli()
        );
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(updated));
        } catch (JsonProcessingException e) {
            log.error("cache.serialize.failed idempotencyKey={}", idempotencyKey, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        return Long.parseLong((String) value);
    }
}
