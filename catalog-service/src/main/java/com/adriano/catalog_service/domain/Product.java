package com.adriano.catalog_service.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "products")
public class Product {
    @Id
    private String id;

    private String name;
    private String description;
    private BigDecimal price;
    private boolean active;

    private Map<String, Object> attributes = new HashMap<>();

    public Product(String name, String description, BigDecimal price, boolean active) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.active = active;
    }

    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

}
