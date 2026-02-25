package com.adriano.catalogservice.service.product;

import com.adriano.catalogservice.domain.product.Product;
import com.adriano.catalogservice.dto.product.ProductRequest;
import com.adriano.catalogservice.dto.product.ProductResponse;
import com.adriano.catalogservice.exception.product.ProductNotFoundException;
import com.adriano.catalogservice.mapper.product.ProductMapper;
import com.adriano.catalogservice.repository.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    public ProductService(ProductRepository productRepository, ProductMapper mapper) {
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    public List<ProductResponse> findAllActive() {
        return productRepository.findByActiveTrue().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ProductResponse findById(String id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    public ProductResponse create(ProductRequest request) {
        Product product = mapper.toEntity(request);
        if (request.attributes() != null) {
            product.setAttributes(request.attributes());
        }
        Product savedProduct = productRepository.save(product);
        return mapper.toResponse(savedProduct);
    }
}
