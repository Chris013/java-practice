package io.github.chris013.practice.task15;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A prepaid gift card. Mutable on purpose: the balance and the PIN counter change with every redemption.
 * Identity is the {@link GiftCardCode}.
 *
 * <p>Task 15: read {@code tasks/15-gift-card-results.md} first.
 */
public final class GiftCard {

    public GiftCard(GiftCardCode code, String pin, BigDecimal balance, LocalDate validUntil) {
        throw new UnsupportedOperationException("TODO: Task 15");
    }

    public GiftCardCode code() {
        throw new UnsupportedOperationException("TODO: Task 15");
    }

    public BigDecimal balance() {
        throw new UnsupportedOperationException("TODO: Task 15");
    }

    public LocalDate validUntil() {
        throw new UnsupportedOperationException("TODO: Task 15");
    }

    public boolean isBlocked() {
        throw new UnsupportedOperationException("TODO: Task 15");
    }

    /**
     * Checks a PIN attempt. A right PIN resets the counter of consecutive wrong attempts. A wrong PIN increments it,
     * and the third consecutive wrong PIN blocks the card. A blocked card always returns {@code false}.
     */
    public boolean checkPin(String attempt) {
        throw new UnsupportedOperationException("TODO: Task 15");
    }

    /**
     * Takes {@code amount} off the balance.
     *
     * @throws IllegalArgumentException if the amount is zero, negative, has more than 2 decimals or exceeds the balance
     */
    public void debit(BigDecimal amount) {
        throw new UnsupportedOperationException("TODO: Task 15");
    }

    /**
     * Returns {@code value} with scale 2. Rejects values that have more than 2 significant decimals, so that
     * {@code 12.5} and {@code 12.500} are accepted and {@code 12.505} is not.
     */
    static BigDecimal requireCents(BigDecimal value, String name) {
        throw new UnsupportedOperationException("TODO: Task 15");
    }
}
