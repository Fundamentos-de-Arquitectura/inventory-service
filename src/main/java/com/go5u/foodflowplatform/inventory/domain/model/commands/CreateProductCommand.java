package com.go5u.foodflowplatform.inventory.domain.model.commands;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProductCommand(
        String name,
        Long productId,
        Integer quantity,
        LocalDate expirationDate,
        BigDecimal price,
        Long userId
) {}