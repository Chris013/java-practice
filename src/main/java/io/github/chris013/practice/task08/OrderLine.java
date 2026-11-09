package io.github.chris013.practice.task08;

/** One product on an order. Part of the {@link Order} aggregate. */
public record OrderLine(ProductId productId, Money unitPrice, int quantity) {

    public Money total() {
        return unitPrice.times(quantity);
    }
}
