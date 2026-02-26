package com.adriano.orderhub.repository.order;

import com.adriano.orderhub.domain.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
