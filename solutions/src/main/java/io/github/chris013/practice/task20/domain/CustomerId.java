package io.github.chris013.practice.task20.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/** The customer number we use in the loyalty context: 1 to 10 digits. */
public record CustomerId(String value) {

    private static final Pattern FORMAT = Pattern.compile("\\d{1,10}");

    public CustomerId {
        Objects.requireNonNull(value, "value");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("A customer id is 1 to 10 digits, was '" + value + "'");
        }
    }
}
