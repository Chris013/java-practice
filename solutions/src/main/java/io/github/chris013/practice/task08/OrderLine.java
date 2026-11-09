package io.github.chris013.practice.task08;

import java.util.Objects;

/** One product on an order. Part of the {@link Order} aggregate. */
public record OrderLine(ProductId productId, Money unitPrice, int quantity) {

    static final int MAX_QUANTITY = 99;

    public OrderLine {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(unitPrice, "unitPrice");
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException(
                    "Quantity of %s must be 1 to %d, was %d".formatted(productId.value(), MAX_QUANTITY, quantity));
        }
    }

    public Money total() {
        return unitPrice.times(quantity);
    }

    OrderLine withQuantity(int newQuantity) {
        return new OrderLine(productId, unitPrice, newQuantity);
    }
}
