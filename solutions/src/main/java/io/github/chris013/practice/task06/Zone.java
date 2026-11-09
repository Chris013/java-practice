package io.github.chris013.practice.task06;

import java.math.BigDecimal;

/** Where a shipment goes, seen from our depot in Germany. */
public enum Zone {
    DOMESTIC("1.0"),
    EU("1.5"),
    WORLD("2.5");

    private final BigDecimal priceFactor;

    Zone(String priceFactor) {
        this.priceFactor = new BigDecimal(priceFactor);
    }

    BigDecimal priceFactor() {
        return priceFactor;
    }
}
