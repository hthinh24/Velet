package com.velet.wallet.configuaration.rabbitmq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentConsumerConfig {
    public static final String PAYMENT_EXCHANGE = "velet.payment.topic";

    public static final String WALLET_PAYMENT_CONFIRMED_QUEUE = "velet.wallet.payment-confirmed.queue";
    public static final String WALLET_PAYMENT_CANCELLED_QUEUE = "velet.wallet.payment-cancelled.queue";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentConfirmedQueue() {
        return QueueBuilder.durable(WALLET_PAYMENT_CONFIRMED_QUEUE)
                           .withArgument("x-dead-letter-exchange", "velet.wallet.dlx")
                           .build();
    }

    @Bean
    public Queue paymentCancelledQueue() {
        return QueueBuilder.durable(WALLET_PAYMENT_CANCELLED_QUEUE)
                           .withArgument("x-dead-letter-exchange", "velet.wallet.dlx")
                           .build();
    }

    @Bean
    public Binding paymentConfirmedBinding() {
        return BindingBuilder.bind(paymentConfirmedQueue())
                             .to(paymentExchange())
                             .with("payment.payment_confirmed");
    }

    @Bean
    public Binding paymentCancelledBinding() {
        return BindingBuilder.bind(paymentCancelledQueue())
                             .to(paymentExchange())
                             .with("payment.payment_cancelled");
    }
}
