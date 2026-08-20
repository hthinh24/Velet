package com.velet.payment.listener.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentCreatedEvent extends ApplicationEvent {

    private final Long paymentId;

    public PaymentCreatedEvent(Object source, Long paymentId) {
        super(source);
        this.paymentId = paymentId;
    }
}
