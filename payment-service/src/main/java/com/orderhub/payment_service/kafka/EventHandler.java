package com.orderhub.payment_service.kafka;

public interface EventHandler<T> {
    String eventType();
    Class<T> payloadType();
    void handle(T event);
}
