package com.velet.wallet.configuaration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Convention: velet.[service].topic
    public static final String WALLET_EXCHANGE = "velet.wallet.topic";

    // Convention: velet.[consumer_service].[purpose].queue
    public static final String WALLET_CACHE_SYNC_QUEUE = "velet.wallet.cache-sync.queue";


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
                             .with("transaction.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        return template;
    }
}