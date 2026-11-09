package io.github.chris013.practice.task15;

/**
 * The printed code on a gift card, e.g. {@code ABCD-1234-EFGH}.
 *
 * <p>The constructor trims and upper-cases the input first, so {@code " abcd-1234-efgh "} is the same code as
 * {@code ABCD-1234-EFGH}. Invalid formats are a programming error, not a business outcome: throw
 * {@link IllegalArgumentException}.
 */
public record GiftCardCode(String value) {

    public GiftCardCode {
        throw new UnsupportedOperationException("TODO: Task 15");
    }
}
