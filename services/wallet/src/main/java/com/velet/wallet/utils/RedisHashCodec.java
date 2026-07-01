package com.velet.wallet.utils;


import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class provides utility methods to convert Java objects to
 * Redis hash maps and with value always using String type
 * NOTE: Only using for flat object, not support nested object
 */
@Component
public class RedisHashCodec {

    private final ObjectMapper objectMapper;

    public RedisHashCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, String> toHash(Object obj) {
        Map<String, Object> raw = objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
        Map<String, String> hash = new LinkedHashMap<>();
        raw.forEach((k, v) -> hash.put(k, v == null ? null : v.toString()));
        return hash;
    }

    public <T> T fromHash(Map<Object, Object> hash, Class<T> targetType) {
        Map<String, Object> raw = new LinkedHashMap<>();
        hash.forEach((k, v) -> raw.put((String) k, v));

        // Jackson auto convert from String to target type
        return objectMapper.convertValue(raw, targetType);
    }
}