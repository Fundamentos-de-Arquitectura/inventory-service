package com.go5u.foodflowplatform.inventory.application.internal.queryservices;

import com.go5u.foodflowplatform.inventory.domain.model.aggregates.Product;
import com.go5u.foodflowplatform.inventory.domain.model.queries.GetAllProductsQuery;
import com.go5u.foodflowplatform.inventory.domain.model.queries.GetProductByIdQuery;
import com.go5u.foodflowplatform.inventory.domain.model.queries.GetProductByNameQuery;
import com.go5u.foodflowplatform.inventory.domain.services.ProductQueryService;
import com.go5u.foodflowplatform.inventory.infrastructure.persistence.jpa.repositories.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductRepository productRepository;

    public ProductQueryServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> handle(GetAllProductsQuery query) {
        return productRepository.findByUserId(query.userId());
    }

    @Override
    public Optional<Product> handle(GetProductByIdQuery query) {
        return productRepository.findByProductIdAndUserId(query.productId(), query.userId());
    }

    @Override
    public Optional<Product> handle(GetProductByNameQuery query) {
        return productRepository.findByNameAndUserId(query.name(), query.userId());
    }
}
