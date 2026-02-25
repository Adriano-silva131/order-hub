package com.adriano.catalogservice.repository.product;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.adriano.catalogservice.domain.product.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, String>{
    List<Product> findByActiveTrue();
}
