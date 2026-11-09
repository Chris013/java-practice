package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.Objects;

/** Given. One product on the basket: what it is, what it costs and how many. */
public record BasketLine(Sku sku, String name, Category category, BigDecimal unitPrice, int quantity) {

    public BasketLine {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        unitPrice = Cents.of(unitPrice, "unitPrice");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice cannot be negative, was " + unitPrice);
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1, was " + quantity);
        }
    }

    /** The price of this line before any discount: unit price times quantity. */
    public BigDecimal total() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
