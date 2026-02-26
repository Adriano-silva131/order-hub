package com.adriano.orderhub.integration.catalog.dto;

import java.math.BigDecimal;

public record CatalogProductResponse(
        String id,
        String name,
        BigDecimal price,
        boolean active
) {
}
