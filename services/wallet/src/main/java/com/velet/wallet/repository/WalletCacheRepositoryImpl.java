package com.velet.wallet.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velet.wallet.dto.cache.ReservationRecord;
import com.velet.wallet.dto.response.WalletInfo;
import com.velet.wallet.exception.AppException;
import com.velet.wallet.exception.ErrorCode;
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
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class WalletCacheRepositoryImpl implements WalletCacheRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> hashRedisTemplate;
    private final ObjectMapper objectMapper;

    private final RedisScript<Long> walletDeductScript;
    private final RedisScript<Long> walletAddScript;

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration ACCOUNT_TTL = Duration.ofMinutes(10);
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(15);

    private static final String LOCK_PREFIX = "lock:wallet:";
    private static final String WALLET_PREFIX = "wallet:";
    private static final String RESERVATION_PREFIX = "wallet:reserve:idempotency:";

    @Override
    public Optional<WalletInfo> findAccount(String walletId) {
        Map<Object, Object> walletMap = hashRedisTemplate.opsForHash().entries(WALLET_PREFIX + walletId);
        if (walletMap == null || walletMap.isEmpty()) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(objectMapper.writeValueAsString(walletMap), WalletInfo.class));
        } catch (JsonProcessingException e) {
            log.warn("cache.deserialize.failed walletId={}", walletId, e);
            return Optional.empty();
        }
    }

    @Override
    public void saveAccount(String walletId, WalletInfo wallet) {
        Map<String, Object> walletMap = objectMapper.convertValue(wallet, new TypeReference<Map<String, Object>>() {});

        hashRedisTemplate.opsForHash().putAll(WALLET_PREFIX + walletId, walletMap);
        hashRedisTemplate.expire(WALLET_PREFIX + walletId, ACCOUNT_TTL);
    }

    @Override
    public void evictAccount(String walletId) {
        hashRedisTemplate.delete(WALLET_PREFIX + walletId);
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
    public boolean deductBalance(Long walletId, Long amount) {
        String cacheKey = WALLET_PREFIX + walletId;

        Long result = hashRedisTemplate.execute(
                walletDeductScript,
                Collections.singletonList(cacheKey), // KEYS[1]
                String.valueOf(amount)               // ARGV[1]
        );

        if (result == null) {
            log.error("Redis script execution failed for deductBalance walletId={} amount={}", walletId, amount);
            throw new AppException(ErrorCode.REDIS_EXECUTION_FAILED);
        }

        switch (result.intValue()) {
            case 1:
                return true;
            case -1:
                throw new AppException(ErrorCode.WALLET_NOT_FOUND);
            case -2:
                throw new AppException(ErrorCode.WALLET_INACTIVE);
            case -3:
                throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
            default:
                log.error("Unexpected result from Redis script for deductBalance walletId={} amount={} result={}",
                          walletId, amount, result);
                throw new AppException(ErrorCode.REDIS_EXECUTION_FAILED);
        }
    }

    @Override
    public boolean addBalance(Long walletId, Long amount) {
        String cacheKey = WALLET_PREFIX + walletId;

        Long result = hashRedisTemplate.execute(
                walletAddScript,
                Collections.singletonList(cacheKey), // KEYS[1]
                String.valueOf(amount)               // ARGV[1]
        );

        if (result == null) {
            log.error("Redis script execution failed for addBalance walletId={} amount={}", walletId, amount);
            throw new AppException(ErrorCode.REDIS_EXECUTION_FAILED);
        }

        switch (result.intValue()) {
            case 1:
                return true;
            case -1:
                throw new AppException(ErrorCode.WALLET_NOT_FOUND);
            case -2:
                throw new AppException(ErrorCode.WALLET_INACTIVE);
            default:
                log.error("Unexpected result from Redis script for addBalance walletId={} amount={} result={}",
                          walletId, amount, result);
                throw new AppException(ErrorCode.REDIS_EXECUTION_FAILED);
        }
    }

    @Override
    public boolean reserve(String walletId, BigDecimal amount) {
        String key = WALLET_PREFIX + walletId;
        long amountCents = amount.longValueExact();

        Object availableBalanceObj = hashRedisTemplate.opsForHash().get(key, "availableBalance");

        if (availableBalanceObj == null) {
            throw new AppException(ErrorCode.WALLET_CACHE_MISS);
        }

        long available = ((Number) availableBalanceObj).longValue();
        if (available < amountCents) {
            return false;
        }

        hashRedisTemplate.opsForHash().increment(key, "availableBalance", -amountCents);
        hashRedisTemplate.opsForHash().increment(key, "pendingBalance", amountCents);

        return true;
    }

    @Override
    public void release(String walletId, BigDecimal amount) {
        String key = WALLET_PREFIX + walletId;
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
            String json = objectMapper.writeValueAsString(record);
            stringRedisTemplate.opsForValue().set(key, json, RESERVATION_TTL);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ReservationRecord for idempotencyKey={}", idempotencyKey, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void updateReservationRecordStatus(String idempotencyKey, String newStatus) {
        String key = RESERVATION_PREFIX + idempotencyKey;
        Optional<ReservationRecord> existing = getReservationRecord(idempotencyKey);
        if (existing.isEmpty()) return;

        ReservationRecord record = existing.get();
        ReservationRecord updatedRecord = new ReservationRecord(
                newStatus,
                record.walletId(),
                record.amount(),
                record.reservedAt(),
                Instant.now().toEpochMilli()
        );

        try {
            String json = objectMapper.writeValueAsString(updatedRecord);
            stringRedisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize updated ReservationRecord for idempotencyKey={}", idempotencyKey, e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
