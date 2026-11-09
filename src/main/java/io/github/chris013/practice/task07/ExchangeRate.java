package io.github.chris013.practice.task07;

import java.math.BigDecimal;
import java.util.Objects;

public record ExchangeRate(CurrencyPair pair, BigDecimal rate) {

    public ExchangeRate {
        Objects.requireNonNull(pair, "pair");
        Objects.requireNonNull(rate, "rate");
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("Exchange rates are positive, got " + rate.toPlainString());
        }
    }
}
