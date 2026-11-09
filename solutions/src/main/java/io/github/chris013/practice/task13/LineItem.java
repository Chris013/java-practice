package io.github.chris013.practice.task13;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One scanned product, with the number of items bought.
 *
 * <p>Prices are gross: they include VAT, as the shelf price does. The price is stored with scale 2, so
 * {@code 1.1} becomes {@code 1.10} and always prints as money.
 */
public record LineItem(String name, Category category, BigDecimal unitPriceGross, int quantity) {

    public LineItem {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(unitPriceGross, "unitPriceGross");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (unitPriceGross.signum() < 0) {
            throw new IllegalArgumentException("price must not be negative, was " + unitPriceGross);
        }
        // Reject instead of rounding: a till must never silently change a price it was given.
        if (unitPriceGross.scale() > 2) {
            throw new IllegalArgumentException("price has more than two decimal places: " + unitPriceGross);
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1, was " + quantity);
        }
        unitPriceGross = unitPriceGross.setScale(2);
    }

    /** The gross price of all items on this line: unit price times quantity. */
    public BigDecimal totalGross() {
        return unitPriceGross.multiply(BigDecimal.valueOf(quantity));
    }
}
