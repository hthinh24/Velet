package com.velet.payment.infrastructure.consumer;

import com.rabbitmq.client.Channel;
import com.velet.payment.configuaration.RabbitMQConfig;
import com.velet.payment.dto.event.PaymentCreatedEventPayload;
import com.velet.payment.exception.AppException;
import com.velet.payment.exception.ErrorCode;
import com.velet.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedConsumer {

    private final PaymentService paymentService;

    @RabbitListener(
            queues = RabbitMQConfig.PROCESS_PAYMENT_QUEUE,
            ackMode = "MANUAL"
    )
    public void onPaymentCreated(
            PaymentCreatedEventPayload payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("payment.consumer.received idempotencyKey={}", payload.idempotencyKey());
        try {
            paymentService.insertAndProcessPayment(payload);
            channel.basicAck(tag, false);
        } catch (AppException e) {
            if (e.getErrorCode().equals(ErrorCode.DUPLICATE_PAYMENT)) {
                log.warn("payment.process.duplicate idempotencyKey={} — record already exists", payload.idempotencyKey());
                channel.basicAck(tag, false);
                return;
            }
            log.error("payment.process.failed idempotencyKey={} — {}", payload.idempotencyKey(), e.getMessage());
            channel.basicNack(tag, false, false);
        }
    }
}
