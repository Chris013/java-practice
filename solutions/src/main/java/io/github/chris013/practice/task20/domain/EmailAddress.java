package io.github.chris013.practice.task20.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A deliberately simple email check: one {@code @}, a local part, and a domain with at least one dot.
 * It is not RFC 5322; it only has to reject the obvious junk. Stored lower-cased.
 */
public record EmailAddress(String value) {

    private static final Pattern SIMPLE_EMAIL = Pattern.compile("[^@\\s]+@[^@\\s.]+(?:\\.[^@\\s.]+)+");

    public EmailAddress {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!SIMPLE_EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("not an email address: '" + value + "'");
        }
    }
}
