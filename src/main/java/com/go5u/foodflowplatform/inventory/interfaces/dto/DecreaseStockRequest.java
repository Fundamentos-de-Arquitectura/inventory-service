package com.go5u.foodflowplatform.inventory.interfaces.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * DTO para restar stock de un ingrediente
 */
public record DecreaseStockRequest(
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser positiva")
        Double quantity
) {}

