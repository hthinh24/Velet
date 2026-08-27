package com.velet.payment.configuaration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "velet.payment.topic";
    public static final String PAYMENT_CREATED_ROUTING_KEY = "payment.payment_created";
    
    public static final String PROCESS_PAYMENT_QUEUE = "velet.payment-worker.process-payment.queue";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue processPaymentQueue() {
        return QueueBuilder.durable(PROCESS_PAYMENT_QUEUE)
                           .withArgument("x-dead-letter-exchange", "velet.payment.dlx")
                           .build();
    }

    @Bean
    public Binding processPaymentBinding() {
        return BindingBuilder.bind(processPaymentQueue())
                             .to(paymentExchange())
                             .with(PAYMENT_CREATED_ROUTING_KEY);
    }
}
