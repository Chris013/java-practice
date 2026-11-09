package io.github.chris013.practice.task07;

import java.util.Currency;
import java.util.Objects;

/** "EUR/USD": how many units of {@code quote} you get for one unit of {@code base}. */
public record CurrencyPair(Currency base, Currency quote) {

    public CurrencyPair {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(quote, "quote");
        if (base.equals(quote)) {
            throw new IllegalArgumentException("A currency pair needs two different currencies, got " + base);
        }
    }

    public static CurrencyPair of(String base, String quote) {
        return new CurrencyPair(Currency.getInstance(base), Currency.getInstance(quote));
    }

    @Override
    public String toString() {
        return base.getCurrencyCode() + "/" + quote.getCurrencyCode();
    }
}
