package io.github.chris013.practice.task15;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;

/**
 * Redeems gift cards at the till.
 *
 * <p>Task 15: read {@code tasks/15-gift-card-results.md} first.
 */
public final class GiftCardService {

    private final GiftCardRepository repository;
    private final Clock clock;

    public GiftCardService(GiftCardRepository repository, Clock clock) {
        throw new UnsupportedOperationException("TODO: Task 15");
    }

    /**
     * Tries to pay {@code amount} with the card.
     *
     * <p>Business outcomes (unknown card, wrong PIN, blocked, expired, empty) are returned as a {@link RedemptionResult}.
     *
     * @throws NullPointerException     if an argument is null
     * @throws IllegalArgumentException if the amount is zero, negative or has more than 2 decimals
     */
    public RedemptionResult redeem(GiftCardCode code, String pin, BigDecimal amount) {
        throw new UnsupportedOperationException("TODO: Task 15");
    }
}
