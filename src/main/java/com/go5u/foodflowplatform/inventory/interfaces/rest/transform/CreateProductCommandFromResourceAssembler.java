package com.go5u.foodflowplatform.inventory.interfaces.rest.transform;

import com.go5u.foodflowplatform.inventory.domain.model.commands.CreateProductCommand;
import com.go5u.foodflowplatform.inventory.interfaces.rest.resources.CreateProductResource;

public class CreateProductCommandFromResourceAssembler {
    public static CreateProductCommand toCommandFromResource(CreateProductResource resource, Long userId){
        return new CreateProductCommand(
                resource.name(),
                resource.productItemId(),
                resource.quantity(),
                resource.expirationDate(),
                resource.price(),
                userId
        );
    }
}
