package com.adriano.catalogservice.dto.product;

import java.math.BigDecimal;
import java.util.Map;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        Map<String, Object> attributes
) {
}
