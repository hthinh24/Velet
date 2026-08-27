package com.velet.payment.configuaration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "velet.payment.topic";
    public static final String PAYMENT_CREATED_ROUTING_KEY = "payment.payment_created";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setObservationEnabled(true);

        template.addBeforePublishPostProcessors(
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                }
        );

        template.setMandatory(true);

        template.setReturnsCallback(returned -> log.error(
                "rabbit.message.unroutable exchange={} routingKey={} replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()
        ));

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData == null) return;
            if (ack) {
                log.debug("rabbit.confirm.ack correlationId={}", correlationData.getId());
            } else {
                log.error("rabbit.confirm.nack correlationId={} cause={}", correlationData.getId(), cause);
            }
        });

        return template;
    }
}
