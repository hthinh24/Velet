package com.velet.payment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velet.payment.dto.event.PaymentCancelledEventPayload;
import com.velet.payment.models.enums.CancelReason;
import com.velet.payment.repository.PaymentCacheRepository;
import com.velet.payment.client.MerchantClient;
import com.velet.payment.client.WalletClient;
import com.velet.payment.dto.client.MerchantMdrResponse;
import com.velet.payment.dto.client.WalletReserveRequest;
import com.velet.payment.dto.client.WalletReserveResponse;
import com.velet.payment.dto.event.PaymentConfirmedEventPayload;
import com.velet.payment.exception.AppException;
import com.velet.payment.exception.ErrorCode;
import com.velet.payment.models.Outbox;
import com.velet.payment.models.Payment;
import com.velet.payment.models.enums.AggregateType;
import com.velet.payment.models.enums.EventType;
import com.velet.payment.models.enums.PaymentStatus;
import com.velet.payment.repository.OutboxRepository;
import com.velet.payment.repository.PaymentRepository;
import com.velet.payment.service.PaymentService;
import com.velet.payment.utils.MdrCalculator;
import com.velet.payment.utils.TraceContextCapture;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.velet.payment.dto.event.PaymentCreatedEventPayload;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentCacheRepository paymentCacheRepository;
    private final OutboxRepository outboxRepository;
    private final WalletClient walletClient;
    private final MerchantClient merchantClient;
    private final MdrCalculator mdrCalculator;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    private final TraceContextCapture traceContextCapture;

    @Override
    @Observed(name = "payment.process", contextualName = "payment.process")
    public void insertAndProcessPayment(PaymentCreatedEventPayload payload) {
        Payment payment;
        try {
            payment = paymentRepository.saveAndFlush(
                    Payment.builder()
                           .idempotencyKey(payload.idempotencyKey())
                           .userId(payload.userWalletId())
                           .merchantId(payload.merchantWalletId())
                           .originalPrice(payload.originalPrice())
                           .status(PaymentStatus.IN_PROGRESS)
                           .build()
            );
        } catch (DataIntegrityViolationException e) {
            log.warn("payment.process.duplicate idempotencyKey={} — skipping", payload.idempotencyKey());
            throw new AppException(ErrorCode.DUPLICATE_PAYMENT);
        }

        log.info("payment.process.start paymentId={} idempotencyKey={}",
                 payment.getId(), payload.idempotencyKey());
        processPayment(payment);
    }

    @Override
    @Observed(name = "payment.process", contextualName = "payment.process")
    public void findAndProcessPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.IN_PROGRESS) {
            log.debug("payment.process.skip paymentId={} (locked or already processed)", paymentId);
            return;
        }
        log.info("payment.process.start paymentId={}", paymentId);
        processPayment(payment);
    }

    private void processPayment(Payment payment) {
        Long paymentId = payment.getId();
        log.info("payment.process.execute paymentId={}", paymentId);

        MerchantMdrResponse mdrResponse = merchantClient.getMdrRate(payment.getMerchantId());
        BigDecimal mdrRate = mdrResponse.mdrRate();

        long mdrFee = mdrCalculator.computeMdrFee(payment.getOriginalPrice(), mdrRate);
        long merchantNet = mdrCalculator.computeMerchantNet(payment.getOriginalPrice(), 0L, mdrFee);
        long finalPrice = payment.getOriginalPrice(); // no voucher/coin in MVP

        String reserveIdempotencyKey = payment.getIdempotencyKey();
        WalletReserveRequest reserveRequest = new WalletReserveRequest(
                String.valueOf(payment.getUserId()),
                String.valueOf(payment.getMerchantId()),
                BigDecimal.valueOf(finalPrice),
                reserveIdempotencyKey
        );

        WalletReserveResponse reserveResponse;
        try {
            reserveResponse = walletClient.reserve(reserveRequest);
        } catch (AppException ex) {
            log.warn("payment.process.reserve.failed paymentId={} reason={}", paymentId, ex.getMessage());
            return;
        }

        log.info("payment.process.reserve.success paymentId={} walletTxId={}",
                 paymentId, reserveResponse.transactionId());

        Payment confirmedPayment = confirmPaymentWithOptimisticLock(payment, mdrFee, finalPrice, merchantNet);
        paymentCacheRepository.put(confirmedPayment);

        log.info("payment.process.completed paymentId={} finalPrice={} mdrFee={}", paymentId, finalPrice, mdrFee);
    }

    private Payment confirmPaymentWithOptimisticLock(Payment payment, long mdrFee, long finalPrice, long merchantNet) {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setMdrFee(mdrFee);
        payment.setFinalPrice(finalPrice);
        payment.setMerchantNet(merchantNet);
        payment.setCompletedAt(Instant.now());

        PaymentConfirmedEventPayload paymentConfirmedEvent =
                buildPaymentConfirmedEvent(payment, mdrFee, finalPrice, merchantNet);
        String payload = toJson(paymentConfirmedEvent);

        return transactionTemplate.execute(txStatus -> {
            Payment comfirmedPayment = paymentRepository.save(payment);

            outboxRepository.save(Outbox.builder()
                                        .aggregateId(comfirmedPayment.getId())
                                        .aggregateType(AggregateType.PAYMENT)
                                        .eventType(EventType.PAYMENT_CONFIRMED)
                                        .traceParent(traceContextCapture.captureTraceParent())
                                        .payload(payload)
                                        .build());

            return comfirmedPayment;
        });

    }

    private PaymentConfirmedEventPayload buildPaymentConfirmedEvent(Payment payment, long mdrFee, long finalPrice,
                                                                    long merchantNet) {
        return PaymentConfirmedEventPayload.builder()
                                           .aggregateId(payment.getId())
                                           .userWalletId(
                                                   payment.getUserId())
                                           .merchantWalletId(
                                                   payment.getMerchantId())
                                           .finalPrice(finalPrice)
                                           .merchantNet(merchantNet)
                                           .mdrFee(mdrFee)
                                           .systemSubsidy(0L)
                                           .voucherId(null)
                                           .coinAmount(0L)
                                           .voucherFundedBy(null)
                                           .originIdempotencyKey(payment.getIdempotencyKey())
                                           .confirmIdempotencyKey(payment.getIdempotencyKey() + ":confirm")
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

    @Override
    @Transactional
    public void cancelTimedOutPayments(Instant cutoff, int batchSize) {
        List<Payment> payments =
                paymentRepository.findAndLockTimedOutPayments(PaymentStatus.IN_PROGRESS.name(), cutoff, batchSize);
        log.info("payment.cancelled timed-out payments found count={}", payments.size());
        if (payments.isEmpty()) {
            return;
        }

        List<Long> cancelledIds = new ArrayList<>(payments.size());
        List<Outbox> outboxes = new ArrayList<>(payments.size());
        for (Payment payment : payments) {
            Instant now = Instant.now();
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setCancelReason(CancelReason.PAYMENT_TIMEOUT);
            payment.setCancelledAt(now);

            PaymentCancelledEventPayload payload = PaymentCancelledEventPayload.builder()
                                                                               .aggregateId(payment.getId())
                                                                               .userWalletId(payment.getUserId())
                                                                               .merchantWalletId(
                                                                                       payment.getMerchantId())
                                                                               .reason(CancelReason.PAYMENT_TIMEOUT.name())
                                                                               .cancelledAt(now.toEpochMilli())
                                                                               .build();

            outboxes.add(Outbox.builder()
                               .aggregateId(payment.getId())
                               .aggregateType(AggregateType.PAYMENT)
                               .eventType(EventType.PAYMENT_CANCELLED)
                               .payload(toJson(payload))
                               .build());

            cancelledIds.add(payment.getId());
        }

        paymentRepository.saveAll(payments);
        outboxRepository.saveAll(outboxes);

        log.info("payment.cancelled timed-out payments cancelledIds={}", cancelledIds);

        paymentCacheRepository.invalidateAll(cancelledIds);
    }
}
