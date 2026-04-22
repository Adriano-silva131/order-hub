package com.adriano.catalogservice.controller;

import com.adriano.catalogservice.controller.product.ProductController;
import com.adriano.catalogservice.dto.product.ProductResponse;
import com.adriano.catalogservice.exception.product.ProductNotFoundException;
import com.adriano.catalogservice.service.product.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void getAllActive_shouldReturn200WithProductList() throws Exception {
        var product = new ProductResponse("1", "Teclado", "Teclado mecânico", new BigDecimal("350.00"), Map.of(), true);
        when(productService.findAllActive()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].name").value("Teclado"));
    }

    @Test
    void getById_shouldReturn200WhenFound() throws Exception {
        var product = new ProductResponse("1", "Monitor", "4K", new BigDecimal("2500.00"), Map.of(), true);
        when(productService.findById("1")).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Monitor"));
    }

    @Test
    void getById_shouldReturn404WhenNotFound() throws Exception {
        when(productService.findById("99")).thenThrow(new ProductNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_shouldReturn201WithLocationHeader() throws Exception {
        var response = new ProductResponse("2", "Headset", "Headset gamer", new BigDecimal("400.00"), Map.of(), true);
        when(productService.create(any())).thenReturn(response);

        var body = """
                {
                    "name": "Headset",
                    "description": "Headset gamer",
                    "price": 400.00,
                    "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Headset"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/2")));
    }

    @Test
    void create_shouldReturn400WhenNameIsBlank() throws Exception {
        var body = """
                {
                    "name": "",
                    "description": "Desc",
                    "price": 100.00,
                    "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_shouldReturn400WhenPriceIsNegative() throws Exception {
        var body = """
                {
                    "name": "Produto",
                    "description": "Desc",
                    "price": -10.00,
                    "active": true
                }
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
