package io.github.chris013.practice.task15;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The printed code on a gift card, e.g. {@code ABCD-1234-EFGH}.
 *
 * <p>The constructor trims and upper-cases the input first, so {@code " abcd-1234-efgh "} is the same code as
 * {@code ABCD-1234-EFGH}. Invalid formats are a programming error, not a business outcome: the till validates
 * the format before it calls the service. The message does not echo the code, because a code is a secret too.
 */
public record GiftCardCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}");

    public GiftCardCode {
        Objects.requireNonNull(value, "value");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("A gift card code looks like XXXX-XXXX-XXXX with letters A-Z and digits 0-9");
        }
    }
}
