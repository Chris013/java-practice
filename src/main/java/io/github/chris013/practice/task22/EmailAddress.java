package io.github.chris013.practice.task22;

import java.util.Objects;

/** A user's login email. Equal addresses are the same mailbox. */
public record EmailAddress(String value) {

    public EmailAddress {
        Objects.requireNonNull(value, "value");
        if (value.isBlank() || !value.contains("@")) {
            throw new IllegalArgumentException("not an email address: " + value);
        }
    }
}
