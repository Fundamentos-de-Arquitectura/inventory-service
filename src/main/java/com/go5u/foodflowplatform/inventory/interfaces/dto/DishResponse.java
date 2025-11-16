package com.go5u.foodflowplatform.inventory.interfaces.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para respuesta de un plato desde Menu service
 */
public record DishResponse(
        Long id,
        String name,
        List<IngredientResponse> ingredients,
        BigDecimal price,
        String description,
        Long userId
) {
    /**
     * DTO para respuesta de un ingrediente
     */
    public record IngredientResponse(
            String name,
            Double quantity,
            String unit
    ) {}
}

