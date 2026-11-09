package io.github.chris013.practice.task16;

import java.util.Optional;

/**
 * "Buy X, pay for Y" on one product, for example "3 for 2 on pasta".
 *
 * <p>Task 16: read {@code tasks/16-promotion-engine.md} first.
 */
public final class BuyXPayY implements Promotion {

    public BuyXPayY(String name, Sku sku, int buy, int pay) {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    @Override
    public Optional<Discount> evaluate(Basket basket) {
        throw new UnsupportedOperationException("TODO: Task 16");
    }
}
