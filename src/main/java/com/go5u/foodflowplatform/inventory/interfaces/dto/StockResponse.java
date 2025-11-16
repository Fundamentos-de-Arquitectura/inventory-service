package com.go5u.foodflowplatform.inventory.interfaces.dto;

/**
 * DTO para respuesta de stock de un ingrediente
 */
public record StockResponse(
        Long productId,
        String ingredientName,
        Integer availableQuantity
) {}

