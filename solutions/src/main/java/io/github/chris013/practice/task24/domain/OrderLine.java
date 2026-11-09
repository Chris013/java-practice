package io.github.chris013.practice.task24.domain;

import java.util.Objects;

/**
 * One product in an order. The quantity rule lives here, so a line that exists is always a valid line.
 */
public record OrderLine(String sku, int quantity, Money unitPrice) {

    public static final int MAX_QUANTITY = 10;

    public OrderLine {
        Objects.requireNonNull(sku, "sku");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (!isValidQuantity(quantity)) {
            throw new IllegalArgumentException("quantity must be 1 to " + MAX_QUANTITY + ": " + quantity);
        }
    }

    public static boolean isValidQuantity(int quantity) {
        return quantity >= 1 && quantity <= MAX_QUANTITY;
    }

    public Money lineTotal() {
        return unitPrice.times(quantity);
    }
}
