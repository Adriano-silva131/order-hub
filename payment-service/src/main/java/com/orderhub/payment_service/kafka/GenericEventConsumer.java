package com.orderhub.payment_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class GenericEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(GenericEventConsumer.class);

    private final Map<String, EventHandler<?>> handlers;
    private final ObjectMapper objectMapper;

    public GenericEventConsumer(List<EventHandler<?>> handlers, ObjectMapper objectMapper) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(EventHandler::eventType, Function.identity()));
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-events", groupId = "payment-group")
    public void consume(ConsumerRecord<String, byte[]> record) {
        if (record.value() == null) {
            log.warn("Skipping record at offset {} — payload is null", record.offset());
            return;
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(record.value());
        } catch (IOException e) {
            log.warn("Skipping record at offset {} — invalid JSON: {}", record.offset(), e.getMessage());
            return;
        }

        String eventType = extractHeader(record, "event-type");
        EventHandler<?> handler = handlers.get(eventType);

        if (handler == null) {
            log.warn("No handler registered for event-type: {}", eventType);
            return;
        }

        dispatch(handler, payload);
    }

    private <T> void dispatch(EventHandler<T> handler, JsonNode payload) {
        T event = objectMapper.convertValue(payload, handler.payloadType());
        handler.handle(event);
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header != null ? new String(header.value()) : null;
    }
}
