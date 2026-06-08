package com.velet.identity.service;

public interface TokenBlacklistService {
    boolean isTokenBlacklisted(String token);
    boolean blacklistToken(String token, long expirationMillis);
}
