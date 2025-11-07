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

    @PostMapping
    @Operation(summary = "Create a new product in the DB",
            description = "Persist a new product record to the database using JPA")
    public ResponseEntity<Map<String, Object>> createProduct(@RequestBody CreateProductResource resource) {
        try {
            log.info("Creating product in DB: {}", resource);

            CreateProductCommand command = CreateProductCommandFromResourceAssembler.toCommandFromResource(resource);
            var product = new com.go5u.foodflowplatform.inventory.domain.model.aggregates.Product(command);
            productRepository.save(product);

            log.info("Successfully created product in DB with id {}", product.getProductId());
            Map<String, Object> body = new HashMap<>();
            body.put("productId", product.getProductId());
            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("Error creating product in DB", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @Operation(summary = "Get all products from the database")
    public ResponseEntity<List<Product>> getAllProducts() {
        try {
            log.info("Fetching all products from DB");
            List<Product> products = productQueryService.handle(new GetAllProductsQuery());
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            log.error("Error fetching products from DB", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get a product by its ID from the DB")
    public ResponseEntity<Product> getProductById(@PathVariable Long productId) {
        try {
            var result = productQueryService.handle(new GetProductByIdQuery(productId));
            return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error fetching product {} from DB", productId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update an existing product by ID in the DB")
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId,
                                                 @RequestBody CreateProductResource resource) {
        try {
            var existing = productRepository.findById(productId).orElse(null);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            var updatedCommand = new CreateProductCommand(
                    existing.getName(),
                    existing.getProductId(),
                    resource.quantity(),
                    resource.expirationDate(),
                    resource.price()
            );
            var updatedProduct = new Product(updatedCommand);
            productRepository.save(updatedProduct);
            log.info("Successfully updated product {} in DB", productId);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            log.error("Error updating product {} in DB", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Delete a product by ID from the DB")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long productId) {
        try {
            productRepository.deleteById(productId);
            log.info("Deleted product {} from DB", productId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting product {} from DB", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
