package com.velet.payment.configuaration;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.velet.payment.client.feign")
public class FeignConfig {
}
