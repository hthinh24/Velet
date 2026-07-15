package com.velet.wallet.infrastructure.consumer.wallet;

import com.rabbitmq.client.Channel;
import com.velet.wallet.configuaration.rabbitmq.WalletExchangeConfig;
import com.velet.wallet.infrastructure.consumer.wallet.event.BalanceReservationCreatedEvent;
import com.velet.wallet.infrastructure.consumer.wallet.event.TransactionCancelledEvent;
import com.velet.wallet.infrastructure.consumer.wallet.event.TransferCompletedEvent;
import com.velet.wallet.service.WalletCacheSyncService;
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
                WalletExchangeConfig.WALLET_CACHE_SYNC_QUEUE,
                WalletExchangeConfig.WALLET_CACHE_RESERVATION_QUEUE
        },
        ackMode = "MANUAL")
public class TransactionEventListener {

    private final WalletCacheSyncService walletCacheSyncService;

    @RabbitHandler
    public void onTransferCompleted(
            TransferCompletedEvent event,
            @Header(AmqpHeaders.MESSAGE_ID) String messageId,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("Received transfer_completed event: {}", event);

        Long eventId = Long.parseLong(messageId);
        walletCacheSyncService.syncBalance(eventId, event);

        channel.basicAck(tag, false);
    }

    @RabbitHandler
    public void onBalanceReservationCreated(
            BalanceReservationCreatedEvent event,
            @Header(AmqpHeaders.MESSAGE_ID) String messageId,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("Received balance reservation event: {}", event);

        Long eventId = Long.parseLong(messageId);
        walletCacheSyncService.reserveBalance(eventId, event);

        channel.basicAck(tag, false);
    }

    @RabbitHandler
    public void onPaymentCanceled(
            TransactionCancelledEvent event,
            @Header(AmqpHeaders.MESSAGE_ID) String messageId,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {

        log.info("Received transaction canceled event: {}", event);

        Long eventId = Long.parseLong(messageId);
        walletCacheSyncService.releaseBalance(eventId, event);

        channel.basicAck(tag, false);
    }
}