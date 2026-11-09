package io.github.chris013.practice.task07;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** A hand-written fake: knows some rates, can be told to fail, and counts how often it was asked. */
final class FakeExchangeRateProvider implements ExchangeRateProvider {

    private final String name;
    private final Map<CurrencyPair, BigDecimal> rates = new HashMap<>();
    private int failuresToSimulate;
    private int calls;

    FakeExchangeRateProvider(String name) {
        this.name = name;
    }

    FakeExchangeRateProvider knows(CurrencyPair pair, String rate) {
        rates.put(pair, new BigDecimal(rate));
        return this;
    }

    FakeExchangeRateProvider failsNextCalls(int times) {
        failuresToSimulate = times;
        return this;
    }

    FakeExchangeRateProvider alwaysFails() {
        return failsNextCalls(Integer.MAX_VALUE);
    }

    int calls() {
        return calls;
    }

    @Override
    public ExchangeRate rateFor(CurrencyPair pair) {
        calls++;
        if (failuresToSimulate > 0) {
            failuresToSimulate--;
            throw new RateUnavailableException(pair, name + " is down");
        }
        BigDecimal rate = rates.get(pair);
        if (rate == null) {
            throw new IllegalArgumentException(name + " does not support " + pair);
        }
        return new ExchangeRate(pair, rate);
    }
}
