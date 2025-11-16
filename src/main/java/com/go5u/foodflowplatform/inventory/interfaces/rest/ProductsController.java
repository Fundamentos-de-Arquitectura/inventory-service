package com.go5u.foodflowplatform.inventory.interfaces.rest;

import com.go5u.foodflowplatform.inventory.domain.model.aggregates.Product;
import com.go5u.foodflowplatform.inventory.domain.model.commands.CreateProductCommand;
import com.go5u.foodflowplatform.inventory.domain.model.queries.GetAllProductsQuery;
import com.go5u.foodflowplatform.inventory.domain.model.queries.GetProductByIdQuery;
import com.go5u.foodflowplatform.inventory.interfaces.rest.resources.CreateProductResource;
import com.go5u.foodflowplatform.inventory.interfaces.rest.transform.CreateProductCommandFromResourceAssembler;
import com.go5u.foodflowplatform.inventory.domain.services.ProductQueryService;
import com.go5u.foodflowplatform.inventory.infrastructure.persistence.jpa.repositories.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "Products", description = "Operations on products in the inventory")
@RestController
@RequestMapping("/api/v1/products")
public class ProductsController {

    private final ProductRepository productRepository;
    private final ProductQueryService productQueryService;

    @Autowired
    public ProductsController(ProductRepository productRepository,
                              ProductQueryService productQueryService) {
        this.productRepository = productRepository;
        this.productQueryService = productQueryService;
    }

    @PostMapping("/users/{userId}")
    @Operation(summary = "Create a new product in the DB",
            description = "Persist a new product record to the database using JPA")
    public ResponseEntity<Map<String, Object>> createProduct(
            @PathVariable Long userId,
            @RequestBody CreateProductResource resource) {
        try {
            log.info("Creating product in DB for user {}: {}", userId, resource);

            CreateProductCommand command = CreateProductCommandFromResourceAssembler.toCommandFromResource(resource, userId);
            var product = new com.go5u.foodflowplatform.inventory.domain.model.aggregates.Product(command);
            productRepository.save(product);

            log.info("Successfully created product in DB with id {} for user {}", product.getProductId(), userId);
            Map<String, Object> body = new HashMap<>();
            body.put("productId", product.getProductId());
            return ResponseEntity.ok(body);

        } catch (IllegalArgumentException e) {
            log.error("Invalid input when creating product: {}", e.getMessage(), e);
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        } catch (Exception e) {
            log.error("Error creating product in DB", e);
            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("error", "Failed to create product: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorBody);
        }
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get all products from the database for a user")
    public ResponseEntity<List<Product>> getAllProducts(@PathVariable Long userId) {
        try {
            log.info("Fetching all products from DB for user {}", userId);
            List<Product> products = productQueryService.handle(new GetAllProductsQuery(userId));
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error fetching products from DB for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("/users/{userId}/{productId}")
    @Operation(summary = "Get a product by its ID from the DB for a user")
    public ResponseEntity<Product> getProductById(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        try {
            var result = productQueryService.handle(new GetProductByIdQuery(productId, userId));
            return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching product {} from DB for user {}", productId, userId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/users/{userId}/{productId}")
    @Operation(summary = "Update an existing product by ID in the DB for a user")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestBody CreateProductResource resource) {
        try {
            var existing = productRepository.findByProductIdAndUserId(productId, userId).orElse(null);
            if (existing == null) {
                log.warn("Product {} not found for user {}", productId, userId);
                return ResponseEntity.notFound().build();
            }

            var updatedCommand = new CreateProductCommand(
                    existing.getName(),
                    existing.getProductId(),
                    resource.quantity(),
                    resource.expirationDate(),
                    resource.price(),
                    userId
            );
            var updatedProduct = new Product(updatedCommand);
            productRepository.save(updatedProduct);
            log.info("Successfully updated product {} in DB for user {}", productId, userId);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            log.error("Error updating product {} in DB for user {}", productId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/users/{userId}/{productId}")
    @Operation(summary = "Delete a product by ID from the DB for a user")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        try {
            var existing = productRepository.findByProductIdAndUserId(productId, userId).orElse(null);
            if (existing == null) {
                log.warn("Product {} not found for user {}", productId, userId);
                return ResponseEntity.notFound().build();
            }
            productRepository.deleteById(productId);
            log.info("Deleted product {} from DB for user {}", productId, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting product {} from DB for user {}", productId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
