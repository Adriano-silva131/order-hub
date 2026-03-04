package com.adriano.orderhub.repository.dlt;

import com.adriano.orderhub.domain.dlt.DltMessage;
import com.adriano.orderhub.domain.dlt.DltStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.UUID;

public interface DltMessageRepository extends JpaRepository<DltMessage, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<DltMessage> findByStatus(DltStatus status);

    boolean existsByOriginalTopicAndMessageKeyAndStatus(String originalTopic, String messageKey, DltStatus status);
}
