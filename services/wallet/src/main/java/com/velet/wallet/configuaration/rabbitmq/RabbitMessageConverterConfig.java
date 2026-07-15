package com.velet.wallet.configuaration.rabbitmq;

import com.velet.wallet.infrastructure.consumer.payment.event.PaymentCancelledEventPayload;
import com.velet.wallet.infrastructure.consumer.payment.event.PaymentConfirmedEventPayload;
import com.velet.wallet.infrastructure.consumer.payment.event.PaymentEventType;
import com.velet.wallet.infrastructure.consumer.wallet.event.BalanceReservationCreatedEvent;
import com.velet.wallet.infrastructure.consumer.wallet.event.TransactionCancelledEvent;
import com.velet.wallet.infrastructure.consumer.wallet.event.TransferCompletedEvent;
import com.velet.wallet.models.enums.EventType;
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
        return template;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<>();

        // WALLET
        idClassMapping.put(EventType.TRANSFER_COMPLETED.name(), TransferCompletedEvent.class);
        idClassMapping.put(EventType.BALANCE_RESERVATION_CREATED.name(), BalanceReservationCreatedEvent.class);
        idClassMapping.put(EventType.TRANSACTION_CANCELLED.name(), TransactionCancelledEvent.class);

        // PAYMENT
        idClassMapping.put(PaymentEventType.PAYMENT_CONFIRMED.name(), PaymentConfirmedEventPayload.class);
        idClassMapping.put(PaymentEventType.PAYMENT_CANCELLED.name(), PaymentCancelledEventPayload.class);

        typeMapper.setIdClassMapping(idClassMapping);
        typeMapper.setTrustedPackages("com.velet.wallet.infrastructure.consumer.*");

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
