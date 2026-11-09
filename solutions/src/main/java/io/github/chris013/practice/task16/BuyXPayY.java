package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * "Buy X, pay for Y" on one product, for example "3 for 2 on pasta".
 *
 * <p>Only full groups count: 5 pasta with "3 for 2" give one free pasta, not two. The free units are priced at the
 * line's unit price, so the customer never gets a discount larger than the item.
 */
public final class BuyXPayY implements Promotion {

    private final String name;
    private final Sku sku;
    private final int buy;
    private final int pay;

    public BuyXPayY(String name, Sku sku, int buy, int pay) {
        this.name = Objects.requireNonNull(name, "name");
        this.sku = Objects.requireNonNull(sku, "sku");
        if (pay < 1) {
            throw new IllegalArgumentException("pay must be at least 1, was " + pay);
        }
        if (buy <= pay) {
            throw new IllegalArgumentException("buy must be more than pay, was buy %d pay %d".formatted(buy, pay));
        }
        this.buy = buy;
        this.pay = pay;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<Discount> evaluate(Basket basket) {
        Optional<BasketLine> line = basket.unclaimedLines().stream()
                .filter(candidate -> candidate.sku().equals(sku))
                .findFirst();
        if (line.isEmpty()) {
            return Optional.empty();
        }
        int freeUnits = (line.get().quantity() / buy) * (buy - pay);
        if (freeUnits == 0) {
            return Optional.empty();
        }
        BigDecimal amount = line.get().unitPrice().multiply(BigDecimal.valueOf(freeUnits));
        if (amount.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(new Discount(name, amount, Set.of(sku)));
    }
}
