package com.go5u.foodflowplatform.inventory.infrastructure.persistence.jpa.repositories;

import com.go5u.foodflowplatform.inventory.domain.model.aggregates.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);
    Optional<Product> findByNameAndUserId(String name, Long userId);
    List<Product> findByUserId(Long userId);
    Optional<Product> findByProductIdAndUserId(Long productId, Long userId);
}
