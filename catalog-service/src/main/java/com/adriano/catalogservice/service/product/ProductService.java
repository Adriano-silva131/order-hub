package com.adriano.catalogservice.service.product;

import com.adriano.catalogservice.domain.product.Product;
import com.adriano.catalogservice.dto.product.ProductRequest;
import com.adriano.catalogservice.dto.product.ProductResponse;
import com.adriano.catalogservice.exception.product.ForbiddenException;
import com.adriano.catalogservice.exception.product.InsufficientStockException;
import com.adriano.catalogservice.exception.product.ProductNotFoundException;
import com.adriano.catalogservice.mapper.product.ProductMapper;
import com.adriano.catalogservice.repository.product.ProductRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final MongoTemplate mongoTemplate;

    public ProductService(ProductRepository productRepository, ProductMapper mapper, MongoTemplate mongoTemplate) {
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.mongoTemplate = mongoTemplate;
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

    public ProductResponse create(ProductRequest request, String sellerId, List<String> roles) {
        if (!roles.contains("SELLER")) {
            throw new ForbiddenException("Only sellers can create products.");
        }

        Product product = mapper.toEntity(request, sellerId);
        Product savedProduct = productRepository.save(product);
        return mapper.toResponse(savedProduct);
    }

    public void decreaseStock(String id, int quantity) {
        Query query = Query.query(Criteria.where("id").is(id).and("stockQuantity").gte(quantity));
        Update update = new Update().inc("stockQuantity", -quantity);
        Product updated = mongoTemplate.findAndModify(query, update, Product.class);

        if (updated == null) {
            if (!productRepository.existsById(id)) {
                throw new ProductNotFoundException("Product not found with id: " + id);
            }
            throw new InsufficientStockException("Insufficient stock for product " + id);
        }
    }

    public void increaseStock(String id, int quantity) {
        Query query = Query.query(Criteria.where("id").is(id));
        Update update = new Update().inc("stockQuantity", quantity);
        Product updated = mongoTemplate.findAndModify(query, update, Product.class);

        if (updated == null) {
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
    }
}
