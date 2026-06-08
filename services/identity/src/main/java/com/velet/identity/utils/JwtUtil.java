package com.velet.identity.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.velet.identity.configuration.JwtProperty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.util.*;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperty jwtConfig;

    public String generateToken(String userID, Map<String, Object> claims) {
        try {
            Instant now = Instant.now();

            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(jwtConfig.getIssuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(jwtConfig.getExpirationMs())))
                    .subject(userID);

            if (claims != null) {
                claims.forEach(claimsBuilder::claim);
            }
            JWTClaimsSet jwtClaimsSet = claimsBuilder.build();

            SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);

            JWSSigner signer = new MACSigner(jwtConfig.getSecretKey().getBytes());
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Error generating JWT", e);
        }
    }

    public String generateRefreshToken(String userID, Map<String, Object> claims) {
        try {
            Instant now = Instant.now();

            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(jwtConfig.getIssuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(jwtConfig.getRefreshExpirationMs())))
                    .subject(userID);

            if (claims != null) {
                claims.forEach(claimsBuilder::claim);
            }
            JWTClaimsSet jwtClaimsSet = claimsBuilder.build();

            SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);

            JWSSigner signer = new MACSigner(jwtConfig.getSecretKey().getBytes());
            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Error generating refresh JWT", e);
        }
    }

    public String extractUserID(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            log.error("Error extracting username from token", e);
            return null;
        }
    }

    public boolean isTokenValid(String token) {
        try {
            // 1. Blacklist Check
//            if (blacklistService.isBlacklisted(token)) {
//                return false;
//            }

            // 2. Parse & Verify Signature
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtConfig.getSecretKey().getBytes());

            // 3. verify signature
            if (!signedJWT.verify(verifier)) {
                return false;
            }

            // 4. Validate expiration
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();

            boolean isExpired = expiration.before(new Date());
            return !isExpired;

        } catch (JOSEException | ParseException e) {
            log.error("Error validating token", e);
            return false;
        }
    }

    public List<String> extractRoles(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            List<String> roles = (List<String>) signedJWT.getJWTClaimsSet().getClaim("roles");

            if (roles != null) {
                return roles;
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error extracting roles from token", e);
            return Collections.emptyList();
        }
    }

    public long getTokenExpiration(String refreshToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(refreshToken);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expiration = claims.getExpirationTime();
            return expiration.getTime();
        } catch (ParseException e) {
            log.error("Error extracting expiration from token", e);
            throw new RuntimeException("Invalid token", e);
        }
    }
}