package com.shop.timesaleservice.controller;


import com.shop.timesaleservice.domain.Product;
import com.shop.timesaleservice.dto.ProductDto;
import com.shop.timesaleservice.service.v1.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDto.Response> createProduct(@Valid @RequestBody ProductDto.CreateRequest request) {
        Product product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProductDto.Response.from(product));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDto.Response> getProduct(@PathVariable Long productId) {
        Product product = productService.getProduct(productId);
        return ResponseEntity.ok(ProductDto.Response.from(product));
    }

    @GetMapping
    public ResponseEntity<List<ProductDto.Response>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products.stream()
                .map(ProductDto.Response::from)
                .toList());
    }
}
