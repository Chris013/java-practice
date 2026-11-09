package io.github.chris013.practice.task09;

import java.math.BigDecimal;

/**
 * The three coverage types we sell. The base premium is the annual price for a standard car,
 * before any risk factor is applied.
 */
public enum Coverage {

    /** Haftpflicht: pays the damage we cause to other people. */
    LIABILITY("300.00"),

    /** Teilkasko: liability plus theft, fire, glass and storm damage. */
    PARTIAL("420.00"),

    /** Vollkasko: partial cover plus damage to your own car. */
    FULL("600.00");

    private final BigDecimal basePremium;

    Coverage(String basePremium) {
        this.basePremium = new BigDecimal(basePremium);
    }

    public BigDecimal basePremium() {
        return basePremium;
    }
}
