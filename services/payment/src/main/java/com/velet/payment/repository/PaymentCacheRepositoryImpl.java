package com.velet.payment.repository;

import com.velet.payment.dto.cache.PaymentCacheEntry;
import com.velet.payment.models.Payment;
import com.velet.payment.utils.RedisHashCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCacheRepositoryImpl implements PaymentCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisHashCodec redisHashCodec;

    private static final String PAYMENT_KEY_PREFIX = "payment:status:";
    private static final String PAYMENT_IDEMPOTENCY_KEY_PREFIX = "payment:idempotency:";
    private static final Duration PAYMENT_TTL = Duration.ofSeconds(2 * 60); // 2 minutes

    @Override
    public void put(Payment payment) {
        PaymentCacheEntry entry = PaymentCacheEntry.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus().name())
                .finalPrice(payment.getFinalPrice())
                .cancelledReason(null)        // no cancelled reason field in MVP
                .updatedAt(Instant.now().toString())
                .build();

        String key = PAYMENT_KEY_PREFIX + payment.getId();

        Map<String, String> hash = redisHashCodec.toHash(entry);
        hash.entrySet().removeIf(e -> e.getValue() == null);

        redisTemplate.opsForHash().putAll(key, hash);
        redisTemplate.expire(key, PAYMENT_TTL);

        // Secondary index: idempotencyKey → paymentId
        String idxKey = PAYMENT_IDEMPOTENCY_KEY_PREFIX + payment.getIdempotencyKey();
        redisTemplate.opsForValue().set(idxKey, String.valueOf(payment.getId()), PAYMENT_TTL);

        log.debug("payment.cache.put paymentId={} status={}", payment.getId(), payment.getStatus());
    }

    @Override
    public Optional<PaymentCacheEntry> getById(Long paymentId) {
        String key = PAYMENT_KEY_PREFIX + paymentId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) return Optional.empty();

        try {
            return Optional.of(redisHashCodec.fromHash(entries, PaymentCacheEntry.class));
        } catch (Exception e) {
            log.warn("payment.cache.deserialize.failed paymentId={}", paymentId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<PaymentCacheEntry> getByIdempotencyKey(String idempotencyKey) {
        String idxKey = PAYMENT_IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        Object paymentIdRaw = redisTemplate.opsForValue().get(idxKey);
        if (paymentIdRaw == null) return Optional.empty();

        return getById(Long.parseLong(paymentIdRaw.toString()));
    }
}
