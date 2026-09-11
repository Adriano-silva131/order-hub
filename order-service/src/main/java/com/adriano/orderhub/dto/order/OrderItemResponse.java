package com.adriano.orderhub.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
