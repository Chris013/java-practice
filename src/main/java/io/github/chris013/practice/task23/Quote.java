package io.github.chris013.practice.task23;

import java.math.BigDecimal;
import java.util.Objects;

/** One offer from one provider: a flight at a price. Task 23. */
public record Quote(String provider, BigDecimal price, String flightNumber) {

    public Quote {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(flightNumber, "flightNumber");
        if (price.signum() <= 0) {
            throw new IllegalArgumentException("A quote must cost more than zero, got " + price.toPlainString());
        }
        if (price.scale() != 2) {
            throw new IllegalArgumentException("Prices have exactly two decimals, got " + price.toPlainString());
        }
    }
}
