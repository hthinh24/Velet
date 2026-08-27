package com.velet.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velet.payment.client.WalletClient;
import com.velet.payment.configuaration.RabbitMQConfig;
import com.velet.payment.dto.cache.PaymentCacheEntry;
import com.velet.payment.dto.client.WalletBalanceResponse;
import com.velet.payment.dto.event.PaymentCreatedEventPayload;
import com.velet.payment.dto.request.CreatePaymentRequest;
import com.velet.payment.dto.response.CreatePaymentResponse;
import com.velet.payment.dto.response.PaymentStatusResponse;
import com.velet.payment.exception.AppException;
import com.velet.payment.exception.ErrorCode;
import com.velet.payment.models.Payment;
import com.velet.payment.models.enums.PaymentStatus;
import com.velet.payment.repository.PaymentCacheRepository;
import com.velet.payment.repository.PaymentRepository;
import com.velet.payment.service.PaymentService;
import com.velet.payment.utils.TraceContextCapture;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentCacheRepository paymentCacheRepository;
    private final WalletClient walletClient;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final TraceContextCapture traceContextCapture;

    private static final long CONFIRM_TIMEOUT_SECONDS = 5L;

    @Override
    @Observed(name = "payment.created", contextualName = "payment.created")
    public CreatePaymentResponse initiatePayment(CreatePaymentRequest request, String idempotencyKey) {
        CreatePaymentResponse existed = getPaymentByIdempotencyKey(idempotencyKey);
        if (existed != null) {
            return existed;
        }

        // Fast-reject: check balance sufficient
        WalletBalanceResponse balance = walletClient.checkBalance(request.userWalletId());
        if (balance.availableBalance() < request.originalPrice()) {
            log.info("payment.fast-reject.insufficient walletId={} available={} required={}",
                     request.userWalletId(), balance.availableBalance(), request.originalPrice());
            throw new AppException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // Build payload and publish to RabbitMQ (blocks until broker ACKs)
        PaymentCreatedEventPayload payload = PaymentCreatedEventPayload.builder()
                                                                       .idempotencyKey(idempotencyKey)
                                                                       .userWalletId(request.userWalletId())
                                                                       .merchantWalletId(request.merchantWalletId())
                                                                       .originalPrice(request.originalPrice())
                                                                       .traceParent(traceContextCapture.captureTraceParent())
                                                                       .build();

        publishPaymentCreatedEvent(payload, idempotencyKey);

        // Cache IN_PROGRESS
        paymentCacheRepository.putPending(idempotencyKey);

        log.info("payment.published idempotencyKey={}", idempotencyKey);

        return CreatePaymentResponse.builder()
                                    .paymentId(null)
                                    .idempotencyKey(idempotencyKey)
                                    .status(PaymentStatus.IN_PROGRESS)
                                    .build();
    }

    @Override
    public PaymentStatusResponse getById(Long id) {
        Optional<PaymentCacheEntry> cached = paymentCacheRepository.getById(id);
        if (cached.isPresent()) {
            return toPaymentStatusResponse(cached.get());
        }

        Payment payment = paymentRepository.findById(id)
                                           .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        paymentCacheRepository.put(payment);
        return toPaymentStatusResponse(payment);
    }

    @Override
    public PaymentStatusResponse getByIdempotencyKey(String key) {
        Optional<PaymentCacheEntry> cached = paymentCacheRepository.getByIdempotencyKey(key);
        if (cached.isPresent()) {
            return toPaymentStatusResponse(cached.get());
        }

        Payment payment = paymentRepository.findByIdempotencyKey(key)
                                           .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        paymentCacheRepository.put(payment);
        return toPaymentStatusResponse(payment);
    }

    private void publishPaymentCreatedEvent(PaymentCreatedEventPayload payload, String idempotencyKey) {
        String body = toJson(payload);

        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.getHeaders().put("__TypeId__", "PAYMENT_CREATED");

        Message message = new Message(body.getBytes(StandardCharsets.UTF_8), props);

        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        log.info("payment.publish idempotencyKey={} correlationId={}", idempotencyKey, correlationId);
        rabbitTemplate.send(RabbitMQConfig.PAYMENT_EXCHANGE, RabbitMQConfig.PAYMENT_CREATED_ROUTING_KEY,
                            message, correlationData);

        awaitBrokerAck(correlationData, idempotencyKey);
    }

    private void awaitBrokerAck(CorrelationData correlationData, String idempotencyKey) {
        try {
            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.ack()) {
                log.error("payment.publish.nack idempotencyKey={} cause={}", idempotencyKey, confirm.reason());
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } catch (TimeoutException e) {
            log.error("payment.publish.timeout idempotencyKey={}", idempotencyKey);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (ExecutionException e) {
            log.error("payment.publish.failed idempotencyKey={}", idempotencyKey, e.getCause());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private PaymentStatusResponse toPaymentStatusResponse(PaymentCacheEntry entry) {
        return PaymentStatusResponse.builder()
                                    .paymentId(entry.paymentId())
                                    .status(PaymentStatus.valueOf(entry.status()))
                                    .finalPrice(entry.finalPrice())
                                    .cancelledReason(entry.cancelledReason())
                                    .build();
    }

    private PaymentStatusResponse toPaymentStatusResponse(Payment payment) {
        return PaymentStatusResponse.builder()
                                    .paymentId(payment.getId())
                                    .status(payment.getStatus())
                                    .finalPrice(payment.getFinalPrice())
                                    .cancelledReason(null)
                                    .build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.info(e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private CreatePaymentResponse getPaymentByIdempotencyKey(String idempotencyKey) {
        Optional<PaymentCacheEntry> cached = paymentCacheRepository.getByIdempotencyKey(idempotencyKey);
        if (cached.isPresent()) {
            PaymentCacheEntry entry = cached.get();
            return CreatePaymentResponse.builder()
                                        .paymentId(entry.paymentId())
                                        .idempotencyKey(idempotencyKey)
                                        .status(PaymentStatus.valueOf(entry.status()))
                                        .build();
        }

        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("payment.idempotent idempotencyKey={} paymentId={}", idempotencyKey, existing.get().getId());
            Payment p = existing.get();
            return CreatePaymentResponse.builder()
                                        .paymentId(p.getId())
                                        .idempotencyKey(idempotencyKey)
                                        .status(p.getStatus())
                                        .build();
        }

        return null;
    }
}
