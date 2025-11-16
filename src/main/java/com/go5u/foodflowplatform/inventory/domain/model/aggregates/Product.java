package com.go5u.foodflowplatform.inventory.domain.model.aggregates;

import com.go5u.foodflowplatform.inventory.domain.model.commands.CreateProductCommand;
import com.go5u.foodflowplatform.inventory.domain.model.valueobjects.ExpirationDate;
import com.go5u.foodflowplatform.inventory.domain.model.valueobjects.Price;
import com.go5u.foodflowplatform.inventory.domain.model.valueobjects.ProductId;
import com.go5u.foodflowplatform.inventory.domain.model.valueobjects.Quantity;
import com.go5u.foodflowplatform.shared.domain.model.aggregates.AuditableAbstractAggregateRoot;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import org.apache.logging.log4j.util.Strings;

@Entity
@Getter
public class Product{

    private String name;

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long productId;

    @Embedded
    private Quantity quantity;

    @Embedded
    private ExpirationDate expirationDate;

    @Embedded
    private Price price;

    @jakarta.persistence.Column(nullable = false)
    private Long userId;

    public Product() {
        this.name = Strings.EMPTY;
    }

    public Product(CreateProductCommand command) {
        this.name = command.name();
        this.quantity = new Quantity(command.quantity());
        this.expirationDate = new ExpirationDate(command.expirationDate());
        this.price = new Price(command.price());
        this.userId = command.userId();
    }

    public void decreaseQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Decrease amount must be non-negative");
        }
        int current = (this.quantity != null) ? this.quantity.quantity() : 0;
        int updated = current - amount;
        if (updated < 0) {
            throw new IllegalArgumentException("Insufficient inventory");
        }
        this.quantity = new Quantity(updated);
    }

    public void increaseQuantity(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Increase amount must be non-negative");
        }
        int current = (this.quantity != null) ? this.quantity.quantity() : 0;
        this.quantity = new Quantity(current + amount);
    }
}
