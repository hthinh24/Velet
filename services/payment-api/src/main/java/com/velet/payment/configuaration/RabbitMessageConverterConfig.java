package com.velet.payment.configuaration;

import com.velet.payment.dto.event.PaymentCreatedEventPayload;
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
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("PAYMENT_CREATED", PaymentCreatedEventPayload.class);

        typeMapper.setIdClassMapping(idClassMapping);
        typeMapper.setTrustedPackages("com.velet.payment.dto.event.*");

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
