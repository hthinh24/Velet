package com.velet.wallet.infrastructure.consumer.payment;

import com.rabbitmq.client.Channel;
import com.velet.wallet.configuaration.rabbitmq.PaymentConsumerConfig;
import com.velet.wallet.infrastructure.consumer.payment.event.PaymentCancelledEventPayload;
import com.velet.wallet.infrastructure.consumer.payment.event.PaymentConfirmedEventPayload;
import com.velet.wallet.service.application.WalletCacheSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
@RabbitListener(
        queues = {
                PaymentConsumerConfig.WALLET_PAYMENT_CONFIRMED_QUEUE,
                PaymentConsumerConfig.WALLET_PAYMENT_CANCELLED_QUEUE,
        },
        ackMode = "MANUAL")
public class PaymentEventListener {

    private final WalletCacheSyncService walletCacheSyncService;

    @RabbitHandler
    public void onPaymentConfirmed(
            PaymentConfirmedEventPayload event,
            @Header(AmqpHeaders.MESSAGE_ID) String messageId,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        channel.basicAck(tag, false);
    }

    @RabbitHandler
    public void onPaymentCancelled(
            PaymentCancelledEventPayload event,
            @Header(AmqpHeaders.MESSAGE_ID) String messageId,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        channel.basicAck(tag, false);
    }
}