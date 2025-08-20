package com.shop.timesaleservice.repository;

import com.shop.timesaleservice.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
