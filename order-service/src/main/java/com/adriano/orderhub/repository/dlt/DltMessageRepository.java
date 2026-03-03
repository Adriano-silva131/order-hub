package com.adriano.orderhub.repository.dlt;

import com.adriano.orderhub.domain.dlt.DltMessage;
import com.adriano.orderhub.domain.dlt.DltStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DltMessageRepository extends JpaRepository<DltMessage, UUID> {
    List<DltMessage> findByStatus(DltStatus status);
}
