package io.github.chris013.practice.task24.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Numbers placed orders: {@code ORD-yyyyMMdd-NNNN}, counted from 1 per instance. Only called once a payment has
 * succeeded, so a refused order never uses up a number.
 */
final class OrderIds {

    private final Clock clock;
    private int counter;

    OrderIds(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    String next() {
        counter++;
        return "ORD-" + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format(Locale.ROOT, "%04d", counter);
    }
}
