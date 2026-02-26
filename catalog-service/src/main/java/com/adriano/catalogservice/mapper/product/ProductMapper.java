package com.adriano.catalogservice.mapper.product;

import com.adriano.catalogservice.domain.product.Product;
import com.adriano.catalogservice.dto.product.ProductRequest;
import com.adriano.catalogservice.dto.product.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public Product toEntity(ProductRequest request) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.active()
        );
        product.setAttributes(request.attributes());
        return product;
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getAttributes(),
                product.isActive()
        );
    }
}
