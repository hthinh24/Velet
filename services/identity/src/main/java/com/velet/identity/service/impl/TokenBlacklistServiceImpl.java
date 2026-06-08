package com.velet.identity.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.velet.identity.service.TokenBlacklistService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "jwt:blk:";

    @Override
    public boolean blacklistToken(String token, long expirationTime) {
        String tokenHash = hashToken(token);
        String key = BLACKLIST_PREFIX + tokenHash;

        long currentTimeMillis = System.currentTimeMillis();
        long timeToLive = (expirationTime - currentTimeMillis) / 1000;

        if (timeToLive < 0) {
            log.warn("Token already expired, cannot blacklist");
            return false;
        }

        redisTemplate.opsForValue().set(key, "1", timeToLive, TimeUnit.SECONDS);
        log.info("Token blacklisted for {} seconds", timeToLive);

        return true;
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        String tokenHash = hashToken(token);
        String key = BLACKLIST_PREFIX + tokenHash;
        return redisTemplate.hasKey(key);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 Algorithm not found", e);
        }
    }
}
