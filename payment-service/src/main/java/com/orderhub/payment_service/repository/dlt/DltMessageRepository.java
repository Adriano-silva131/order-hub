package com.orderhub.payment_service.repository.dlt;

import com.orderhub.payment_service.domain.dlt.DltMessage;
import com.orderhub.payment_service.domain.dlt.DltStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DltMessageRepository extends JpaRepository<DltMessage, UUID> {
    List<DltMessage> findByStatus(DltStatus status);
}
