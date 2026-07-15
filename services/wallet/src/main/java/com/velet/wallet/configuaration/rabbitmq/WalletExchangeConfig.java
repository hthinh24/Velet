package com.velet.wallet.configuaration.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WalletExchangeConfig {

    // Convention: velet.[service].topic
    public static final String WALLET_EXCHANGE = "velet.wallet.topic";

    // Convention: velet.[consumer_service].[purpose]-[source_domain].queue
    public static final String WALLET_CACHE_SYNC_QUEUE = "velet.wallet.cache-sync.queue";
    public static final String WALLET_CACHE_RESERVATION_QUEUE = "velet.wallet.cache-reservation.queue";

    @Bean
    public TopicExchange walletExchange() {
        return new TopicExchange(WALLET_EXCHANGE, true, false);
    }

    @Bean
    public Queue walletCacheSyncQueue() {
        return QueueBuilder.durable(WALLET_CACHE_SYNC_QUEUE)
                           .withArgument("x-dead-letter-exchange", "velet.wallet.dlx")
                           .build();
    }

    @Bean
    public Binding walletCacheSyncBinding() {
        return BindingBuilder.bind(walletCacheSyncQueue())
                             .to(walletExchange())
                             .with("transaction.transfer_completed");
    }

    @Bean
    public Queue walletCacheReservationQueue() {
        return QueueBuilder.durable(WALLET_CACHE_RESERVATION_QUEUE)
                           .withArgument("x-dead-letter-exchange", "velet.wallet.dlx")
                           .build();
    }

    @Bean
    public Binding walletCacheReservationBinding() {
        return BindingBuilder.bind(walletCacheReservationQueue())
                             .to(walletExchange())
                             .with("transaction.balance_reservation_created");
    }

    @Bean
    public Binding walletTransactionCancelBinding() {
        return BindingBuilder.bind(walletCacheReservationQueue())
                             .to(walletExchange())
                             .with("transaction.transaction_cancelled");
    }
}