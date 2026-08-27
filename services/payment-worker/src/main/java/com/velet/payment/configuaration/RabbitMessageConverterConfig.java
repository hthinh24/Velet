package com.velet.payment.configuaration;

import com.velet.payment.dto.event.PaymentCancelledEventPayload;
import com.velet.payment.dto.event.PaymentConfirmedEventPayload;
import com.velet.payment.dto.event.PaymentCreatedEventPayload;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMessageConverterConfig {

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);
        template.setObservationEnabled(true);

        template.addBeforePublishPostProcessors(
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                }
        );

        return template;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<>();

        // Inbound: consumed by payment-worker
        idClassMapping.put("PAYMENT_CREATED", PaymentCreatedEventPayload.class);

        // Outbound: published by payment-worker via outbox → RabbitMQ
        idClassMapping.put("PAYMENT_CONFIRMED", PaymentConfirmedEventPayload.class);
        idClassMapping.put("PAYMENT_CANCELLED", PaymentCancelledEventPayload.class);

        typeMapper.setIdClassMapping(idClassMapping);
        typeMapper.setTrustedPackages("com.velet.payment.dto.event.*");

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
