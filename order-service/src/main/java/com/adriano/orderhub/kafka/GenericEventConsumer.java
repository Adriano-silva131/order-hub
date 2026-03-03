package com.adriano.orderhub.kafka;

import com.adriano.orderhub.domain.dlt.DltMessage;
import com.adriano.orderhub.domain.dlt.DltStatus;
import com.adriano.orderhub.repository.dlt.DltMessageRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GenericEventConsumer {

    private final Map<String, EventHandler<?>> handlers;
    private final ObjectMapper objectMapper;
    private final DltMessageRepository dltMessageRepository;

    public GenericEventConsumer(List<EventHandler<?>> handlers, ObjectMapper objectMapper,
                                DltMessageRepository dltMessageRepository) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(EventHandler::eventType, Function.identity()));
        this.objectMapper = objectMapper;
        this.dltMessageRepository = dltMessageRepository;
    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 2000, multiplier = 2.0, maxDelay = 10000),
            kafkaTemplate = "retryKafkaTemplate",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "payment-events", groupId = "${spring.kafka.consumer.group-id}")
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

    @DltHandler
    public void handleDlt(ConsumerRecord<String, byte[]> record) {
        String eventType = extractHeader(record, "event-type");
        String errorMessage = extractHeader(record, "kafka_exception-message");
        log.error(
                "Message exhausted all retries and sent to DLT. topic={}, partition={}, offset={}, event-type={}, error={}",
                record.topic(),
                record.partition(),
                record.offset(),
                eventType,
                errorMessage
        );

        try {
            String originalTopic = extractHeader(record, "kafka_original-topic");
            if (originalTopic == null) {
                originalTopic = record.topic();
            }

            DltMessage dltMessage = DltMessage.builder()
                    .originalTopic(originalTopic)
                    .messageKey(record.key())
                    .eventType(eventType)
                    .payload(record.value() != null ? new String(record.value(), StandardCharsets.UTF_8) : "")
                    .errorMessage(errorMessage)
                    .status(DltStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            dltMessageRepository.save(dltMessage);
            log.info("DLT message persisted to database with id={}, original-topic={}", dltMessage.getId(), originalTopic);
        } catch (Exception e) {
            log.error("Failed to persist DLT message to database: {}", e.getMessage(), e);
        }
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
