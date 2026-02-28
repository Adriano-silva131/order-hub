package com.adriano.orderhub.kafka;

public interface EventHandler<T> {
    String eventType();
    Class<T> payloadType();
    void handle(T event);
}
