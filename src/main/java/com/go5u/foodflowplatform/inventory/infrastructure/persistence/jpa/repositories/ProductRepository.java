package com.go5u.foodflowplatform.inventory.infrastructure.persistence.jpa.repositories;

import com.go5u.foodflowplatform.inventory.domain.model.aggregates.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
