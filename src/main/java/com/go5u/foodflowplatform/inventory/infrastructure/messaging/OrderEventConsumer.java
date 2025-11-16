package com.go5u.foodflowplatform.inventory.infrastructure.messaging;

import com.go5u.foodflowplatform.inventory.domain.model.events.OrderEvent;
import com.go5u.foodflowplatform.inventory.domain.model.events.OrderItemEvent;
import com.go5u.foodflowplatform.inventory.domain.services.ProductCommandService;
import com.go5u.foodflowplatform.inventory.domain.services.ProductQueryService;
import com.go5u.foodflowplatform.inventory.domain.model.queries.GetProductByNameQuery;
import com.go5u.foodflowplatform.inventory.infrastructure.client.MenuClient;
import com.go5u.foodflowplatform.inventory.interfaces.dto.DishResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ProductCommandService productCommandService;
    private final ProductQueryService productQueryService;
    private final InventoryEventProducer inventoryEventProducer;
    private final MenuClient menuClient;

    @KafkaListener(
            topics = "orders-events",
            groupId = "inventory-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(OrderEvent event) {
        log.info("Received order event: {} with status: {}", event.getOrderId(), event.getStatus());

        try {
            switch(event.getStatus()) {
                case "CREATED":
                    log.info("Processing order creation: {}", event.getOrderId());
                    processOrderCreation(event);
                    break;

                case "CANCELLED":
                    log.info("Processing order cancellation: {}", event.getOrderId());
                    processOrderCancellation(event);
                    break;

                default:
                    log.warn("Unknown order status: {}", event.getStatus());
            }
        } catch (Exception ex) {
            log.error("Error processing order event: {}", event.getOrderId(), ex);
        }
    }

    /**
     * Process order creation by:
     * 1. Querying menu service for each dish's ingredients
     * 2. Calculating total required ingredients (considering dish quantity)
     * 3. Decreasing stock for each ingredient by ingredient name
     */
    private void processOrderCreation(OrderEvent event) {
        // Map to aggregate required ingredients across all dishes in the order
        // Key: ingredient name, Value: total quantity needed
        Map<String, Double> requiredIngredients = new HashMap<>();

        // Iterate through each dish in the order
        for (OrderItemEvent item : event.getItems()) {
            Long dishId = item.getDishId();
            Integer dishQuantity = item.getQuantity(); // Quantity of this dish ordered

            log.info("Processing dish ID: {}, quantity: {}", dishId, dishQuantity);

            // Query menu service to get dish ingredients
            var dishOpt = menuClient.getDishById(dishId);
            if (dishOpt.isEmpty()) {
                log.warn("Dish {} not found in menu service, skipping inventory update", dishId);
                continue;
            }

            DishResponse dish = dishOpt.get();

            // Calculate required ingredients for this dish
            for (DishResponse.IngredientResponse ingredient : dish.ingredients()) {
                String ingredientName = ingredient.name();
                // Total needed = ingredient quantity per dish * number of dishes ordered
                double totalNeeded = ingredient.quantity() * dishQuantity;

                // Aggregate with other dishes that might use the same ingredient
                requiredIngredients.merge(ingredientName, totalNeeded, Double::sum);

                log.info("Dish '{}' requires {} {} of ingredient '{}'",
                        dish.name(), totalNeeded, ingredient.unit(), ingredientName);
            }
        }

        // Update inventory for each required ingredient
        for (Map.Entry<String, Double> entry : requiredIngredients.entrySet()) {
            String ingredientName = entry.getKey();
            Double quantityToDecrease = entry.getValue();

            try {
                // Find product by ingredient name
                var productOpt = productQueryService.handle(new GetProductByNameQuery(ingredientName));
                
                if (productOpt.isEmpty()) {
                    log.warn("Ingredient '{}' not found in inventory, skipping stock update", ingredientName);
                    continue;
                }

                var product = productOpt.get();
                
                // Decrease stock (converting Double to Integer)
                int quantityToDecreaseInt = quantityToDecrease.intValue();
                productCommandService.decreaseInventoryQuantity(
                        product.getProductId(),
                        quantityToDecreaseInt
                );

                log.info("Decreased inventory for ingredient '{}' by {} units",
                        ingredientName, quantityToDecreaseInt);

            } catch (IllegalArgumentException e) {
                log.error("Cannot decrease stock for ingredient '{}': {}", ingredientName, e.getMessage());
                // Continue processing other ingredients even if one fails
            } catch (Exception e) {
                log.error("Error updating stock for ingredient '{}': {}", ingredientName, e.getMessage(), e);
            }
        }
    }

    /**
     * Process order cancellation by:
     * 1. Querying menu service for each dish's ingredients
     * 2. Calculating total ingredients to restore
     * 3. Increasing stock for each ingredient by ingredient name
     */
    private void processOrderCancellation(OrderEvent event) {
        // Map to aggregate ingredients to restore across all dishes in the order
        Map<String, Double> ingredientsToRestore = new HashMap<>();

        // Iterate through each dish in the cancelled order
        for (OrderItemEvent item : event.getItems()) {
            Long dishId = item.getDishId();
            Integer dishQuantity = item.getQuantity();

            log.info("Restoring ingredients for cancelled dish ID: {}, quantity: {}", dishId, dishQuantity);

            // Query menu service to get dish ingredients
            var dishOpt = menuClient.getDishById(dishId);
            if (dishOpt.isEmpty()) {
                log.warn("Dish {} not found in menu service, skipping inventory restore", dishId);
                continue;
            }

            DishResponse dish = dishOpt.get();

            // Calculate ingredients to restore for this dish
            for (DishResponse.IngredientResponse ingredient : dish.ingredients()) {
                String ingredientName = ingredient.name();
                double totalToRestore = ingredient.quantity() * dishQuantity;

                // Aggregate with other dishes
                ingredientsToRestore.merge(ingredientName, totalToRestore, Double::sum);

                log.info("Restoring {} {} of ingredient '{}' for cancelled dish '{}'",
                        totalToRestore, ingredient.unit(), ingredientName, dish.name());
            }
        }

        // Restore inventory for each ingredient
        for (Map.Entry<String, Double> entry : ingredientsToRestore.entrySet()) {
            String ingredientName = entry.getKey();
            Double quantityToRestore = entry.getValue();

            try {
                // Find product by ingredient name
                var productOpt = productQueryService.handle(new GetProductByNameQuery(ingredientName));
                
                if (productOpt.isEmpty()) {
                    log.warn("Ingredient '{}' not found in inventory, skipping stock restore", ingredientName);
                    continue;
                }

                var product = productOpt.get();
                
                // Increase stock (converting Double to Integer)
                int quantityToRestoreInt = quantityToRestore.intValue();
                productCommandService.increaseInventoryQuantity(
                        product.getProductId(),
                        quantityToRestoreInt
                );

                log.info("Restored inventory for ingredient '{}' by {} units",
                        ingredientName, quantityToRestoreInt);

            } catch (Exception e) {
                log.error("Error restoring stock for ingredient '{}': {}", ingredientName, e.getMessage(), e);
            }
        }
    }
}