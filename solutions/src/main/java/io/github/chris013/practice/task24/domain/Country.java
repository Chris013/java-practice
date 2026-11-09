package io.github.chris013.practice.task24.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * The countries we ship to, each with its shipping cost. A closed set, so an enum: the compiler finds every
 * place that needs to change when we add a country.
 */
public enum Country {

    DE(new Money(new BigDecimal("4.90"))),
    AT(new Money(new BigDecimal("9.90"))),
    FR(new Money(new BigDecimal("9.90"))),
    NL(new Money(new BigDecimal("9.90")));

    private final Money shippingCost;

    Country(Money shippingCost) {
        this.shippingCost = shippingCost;
    }

    public Money shippingCost() {
        return shippingCost;
    }

    /** Empty for {@code null} and for codes we do not ship to. Codes are exact: "de" is not "DE". */
    public static Optional<Country> fromCode(String code) {
        for (Country country : values()) {
            if (country.name().equals(code)) {
                return Optional.of(country);
            }
        }
        return Optional.empty();
    }
}
