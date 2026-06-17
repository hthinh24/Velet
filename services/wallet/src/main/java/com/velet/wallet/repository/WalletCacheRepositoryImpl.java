package com.velet.wallet.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velet.wallet.dto.response.WalletInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class WalletCacheRepositoryImpl implements WalletCacheRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

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
}
