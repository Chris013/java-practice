package io.github.chris013.practice.task15;

/**
 * The outcome of paying with a gift card. A declined card is a normal business outcome, not an exception.
 *
 * <p>Task 15: read {@code tasks/15-gift-card-results.md} first.
 */
public sealed interface RedemptionResult permits Redeemed, PartiallyRedeemed, Declined {}
