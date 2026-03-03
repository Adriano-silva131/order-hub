package com.adriano.orderhub.service.dlt;

import com.adriano.orderhub.domain.dlt.DltMessage;
import com.adriano.orderhub.domain.dlt.DltStatus;
import com.adriano.orderhub.repository.dlt.DltMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DltReprocessingService {

    private final DltMessageRepository repository;
    private final KafkaTemplate<String, byte[]> retryKafkaTemplate;

    @Scheduled(fixedDelayString = "${dlt.reprocess.interval:1800000}")
    @Transactional
    public void reprocessPending() {
        List<DltMessage> pending = repository.findByStatus(DltStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        log.info("Reprocessing {} DLT message(s)", pending.size());
        for (DltMessage msg : pending) {
            try {
                ProducerRecord<String, byte[]> record = new ProducerRecord<>(
                        msg.getOriginalTopic(),
                        msg.getMessageKey(),
                        msg.getPayload().getBytes(StandardCharsets.UTF_8)
                );
                if (msg.getEventType() != null) {
                    record.headers().add("event-type", msg.getEventType().getBytes(StandardCharsets.UTF_8));
                }
                retryKafkaTemplate.send(record);
                msg.setStatus(DltStatus.REPROCESSED);
                msg.setReprocessedAt(LocalDateTime.now());
                log.info("Reprocessed DLT message id={} to topic={}", msg.getId(), msg.getOriginalTopic());
            } catch (Exception e) {
                log.error("Failed to reprocess DLT message id={}: {}", msg.getId(), e.getMessage(), e);
            }
        }
    }
}
