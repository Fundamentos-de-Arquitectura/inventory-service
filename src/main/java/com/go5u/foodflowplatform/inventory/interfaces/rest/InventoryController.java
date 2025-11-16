package com.go5u.foodflowplatform.inventory.interfaces.rest;

import com.go5u.foodflowplatform.inventory.domain.model.aggregates.Product;
import com.go5u.foodflowplatform.inventory.domain.model.queries.GetProductByNameQuery;
import com.go5u.foodflowplatform.inventory.domain.services.ProductCommandService;
import com.go5u.foodflowplatform.inventory.domain.services.ProductQueryService;
import com.go5u.foodflowplatform.inventory.interfaces.dto.DecreaseStockRequest;
import com.go5u.foodflowplatform.inventory.interfaces.dto.StockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller para gestión de inventario por nombre de ingrediente
 * Usado para comunicación entre microservicios
 */
@Slf4j
@Tag(name = "Inventory", description = "Inventory management operations")
@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final ProductQueryService productQueryService;
    private final ProductCommandService productCommandService;

    public InventoryController(ProductQueryService productQueryService,
                              ProductCommandService productCommandService) {
        this.productQueryService = productQueryService;
        this.productCommandService = productCommandService;
    }

    /**
     * Obtiene el stock disponible de un ingrediente por nombre
     * @param ingredientName Nombre del ingrediente
     * @return Stock disponible del ingrediente
     */
    @GetMapping("/ingredients/{ingredientName}/stock")
    @Operation(summary = "Get stock by ingredient name", description = "Retrieve available stock for an ingredient")
    public ResponseEntity<StockResponse> getStockByIngredientName(@PathVariable String ingredientName) {
        try {
            log.info("Fetching stock for ingredient: {}", ingredientName);

            var productOpt = productQueryService.handle(new GetProductByNameQuery(ingredientName));
            
            if (productOpt.isEmpty()) {
                log.warn("Ingredient {} not found in inventory", ingredientName);
                return ResponseEntity.notFound().build();
            }

            Product product = productOpt.get();
            StockResponse response = new StockResponse(
                    product.getProductId(),
                    product.getName(),
                    product.getQuantity().quantity()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching stock for ingredient {}: {}", ingredientName, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Resta cantidad de un ingrediente del inventario
     * @param ingredientName Nombre del ingrediente
     * @param request Cantidad a restar
     * @return Respuesta de éxito o error
     */
    @PostMapping("/ingredients/{ingredientName}/decrease")
    @Operation(summary = "Decrease ingredient stock", description = "Decrease the stock of an ingredient")
    public ResponseEntity<Map<String, String>> decreaseIngredientStock(
            @PathVariable String ingredientName,
            @RequestBody DecreaseStockRequest request) {
        try {
            log.info("Decreasing stock for ingredient {} by {}", ingredientName, request.quantity());

            var productOpt = productQueryService.handle(new GetProductByNameQuery(ingredientName));
            
            if (productOpt.isEmpty()) {
                log.warn("Ingredient {} not found in inventory", ingredientName);
                return ResponseEntity.notFound().build();
            }

            Product product = productOpt.get();
            // Convertir Double a Integer para el método decreaseInventoryQuantity
            int quantityToDecrease = request.quantity().intValue();
            
            productCommandService.decreaseInventoryQuantity(product.getProductId(), quantityToDecrease);

            log.info("Successfully decreased stock for ingredient {} by {}", ingredientName, quantityToDecrease);
            return ResponseEntity.ok(Map.of("message", "Stock decreased successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("Cannot decrease stock for ingredient {}: {}", ingredientName, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error decreasing stock for ingredient {}: {}", ingredientName, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
        }
    }
}

