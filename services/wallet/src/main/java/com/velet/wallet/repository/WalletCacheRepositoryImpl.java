package com.velet.wallet.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velet.wallet.dto.response.WalletInfo;
import com.velet.wallet.exception.AppException;
import com.velet.wallet.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class WalletCacheRepositoryImpl implements WalletCacheRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final RedisScript<Long> walletDeductScript;
    private final RedisScript<Long> walletAddScript;

    private static final Duration LOCK_TTL    = Duration.ofSeconds(10);
    private static final Duration ACCOUNT_TTL = Duration.ofMinutes(10);

    private static final String LOCK_PREFIX    = "lock:wallet:";
    private static final String ACCOUNT_PREFIX = "wallet:";

    @Override
    public Optional<WalletInfo> findAccount(String walletId) {
        String json = redisTemplate.opsForValue().get(ACCOUNT_PREFIX + walletId);
        if (json == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(json, WalletInfo.class));
        } catch (JsonProcessingException e) {
            log.warn("cache.deserialize.failed walletId={}", walletId, e);
            return Optional.empty();
        }
    }

    @Override
    public void saveAccount(String walletId, WalletInfo wallet) {
        try {
            String json = objectMapper.writeValueAsString(wallet);
            redisTemplate.opsForValue().set(ACCOUNT_PREFIX + walletId, json, ACCOUNT_TTL);
        } catch (JsonProcessingException e) {
            log.warn("cache.serialize.failed walletId={}", walletId, e);
        }
    }

    @Override
    public void evictAccount(String walletId) {
        redisTemplate.delete(ACCOUNT_PREFIX + walletId);
    }

    @Override
    public boolean acquireLock(String walletId) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + walletId, "1", LOCK_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void releaseLock(String walletId) {
        redisTemplate.delete(LOCK_PREFIX + walletId);
    }

    @Override
    public boolean deductBalance(Long walletId, Long amount) {
        String cacheKey = ACCOUNT_PREFIX + walletId;

        Long result = redisTemplate.execute(
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
                log.error("Unexpected result from Redis script for deductBalance walletId={} amount={} result={}", walletId, amount, result);
                throw new AppException(ErrorCode.REDIS_EXECUTION_FAILED);
        }
    }

    @Override
    public boolean addBalance(Long walletId, Long amount) {
        String cacheKey = ACCOUNT_PREFIX + walletId;

        Long result = redisTemplate.execute(
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
                log.error("Unexpected result from Redis script for addBalance walletId={} amount={} result={}", walletId, amount, result);
                throw new AppException(ErrorCode.REDIS_EXECUTION_FAILED);
        }
    }
}
