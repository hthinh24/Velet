package com.velet.payment.listener.event;

import org.springframework.context.ApplicationEvent;

public class PaymentCreatedEvent extends ApplicationEvent {

    private final Long paymentId;

    public PaymentCreatedEvent(Object source, Long paymentId) {
        super(source);
        this.paymentId = paymentId;
    }

    public Long getPaymentId() {
        return paymentId;
    }
}
