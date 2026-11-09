package io.github.chris013.practice.task25.domain;

import java.math.BigDecimal;

/** Given. The plans we sell, cheapest first. */
public enum Plan {

    FREE(new BigDecimal("0.00")),
    TEAM(new BigDecimal("29.00")),
    BUSINESS(new BigDecimal("79.00"));

    private final BigDecimal monthlyPrice;

    Plan(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public BigDecimal monthlyPrice() {
        return monthlyPrice;
    }

    /** Compares prices, not declaration order, so reordering the constants cannot change billing. */
    public boolean isUpgradeFrom(Plan other) {
        return monthlyPrice.compareTo(other.monthlyPrice) > 0;
    }
}
