package io.github.chris013.practice.task15;

import static io.github.chris013.practice.task15.DeclineReason.CARD_BLOCKED;
import static io.github.chris013.practice.task15.DeclineReason.EMPTY;
import static io.github.chris013.practice.task15.DeclineReason.EXPIRED;
import static io.github.chris013.practice.task15.DeclineReason.UNKNOWN_CARD;
import static io.github.chris013.practice.task15.DeclineReason.WRONG_PIN;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Redeems gift cards at the till.
 *
 * <p>The checks run in a fixed order, and the order is part of the behaviour: a wrong PIN is counted even on an
 * expired card, and an expired card is reported as expired even when its balance is zero.
 */
public final class GiftCardService {

    private final GiftCardRepository repository;
    private final Clock clock;

    public GiftCardService(GiftCardRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.clock = Objects.requireNonNull(clock, "clock");
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
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(pin, "pin");
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("A redemption must be positive, got " + amount.toPlainString());
        }
        BigDecimal requested = GiftCard.requireCents(amount, "amount");

        Optional<GiftCard> found = repository.findByCode(code);
        if (found.isEmpty()) {
            return new Declined(UNKNOWN_CARD);
        }
        GiftCard card = found.get();
        if (card.isBlocked()) {
            return new Declined(CARD_BLOCKED);
        }

        boolean pinAccepted = card.checkPin(pin);
        // Save after every PIN attempt: the counter changed (or was reset), and a lost counter lets a thief keep guessing.
        repository.save(card);
        if (!pinAccepted) {
            // The attempt that blocks the card is reported as CARD_BLOCKED, so the cashier knows to refer the customer.
            return new Declined(card.isBlocked() ? CARD_BLOCKED : WRONG_PIN);
        }

        // Valid on the last day itself: expired only once today is after validUntil.
        if (LocalDate.now(clock).isAfter(card.validUntil())) {
            return new Declined(EXPIRED);
        }
        if (card.balance().signum() == 0) {
            return new Declined(EMPTY);
        }

        BigDecimal charged = card.balance().min(requested);
        card.debit(charged);
        repository.save(card);

        if (charged.compareTo(requested) == 0) {
            return new Redeemed(charged, card.balance());
        }
        return new PartiallyRedeemed(charged, requested.subtract(charged));
    }
}
