package com.velet.identity.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.jwt")
@Getter
@Setter
public class JwtProperty {
    private String secretKey;
    private String issuer;
    private long expirationMs;
    private long refreshExpirationMs;
}
