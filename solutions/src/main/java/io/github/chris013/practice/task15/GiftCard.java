package io.github.chris013.practice.task15;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A prepaid gift card. Mutable on purpose: the balance and the PIN counter change with every redemption.
 * Identity is the {@link GiftCardCode}.
 *
 * <p>The PIN is stored as given. Hashing it is a stretch goal of task 15.
 */
public final class GiftCard {

    static final int MAX_CONSECUTIVE_WRONG_PINS = 3;
    private static final Pattern PIN_FORMAT = Pattern.compile("\\d{4}");

    private final GiftCardCode code;
    private final String pin;
    private final LocalDate validUntil;
    private BigDecimal balance;
    private int consecutiveWrongPins;
    private boolean blocked;

    public GiftCard(GiftCardCode code, String pin, BigDecimal balance, LocalDate validUntil) {
        this.code = Objects.requireNonNull(code, "code");
        Objects.requireNonNull(pin, "pin");
        if (!PIN_FORMAT.matcher(pin).matches()) {
            // Never put the PIN itself into a message: messages end up in logs.
            throw new IllegalArgumentException("A PIN has exactly 4 digits");
        }
        this.pin = pin;
        Objects.requireNonNull(balance, "balance");
        if (balance.signum() < 0) {
            throw new IllegalArgumentException("A gift card balance cannot be negative, got " + balance.toPlainString());
        }
        this.balance = requireCents(balance, "balance");
        this.validUntil = Objects.requireNonNull(validUntil, "validUntil");
    }

    public GiftCardCode code() {
        return code;
    }

    public BigDecimal balance() {
        return balance;
    }

    public LocalDate validUntil() {
        return validUntil;
    }

    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Checks a PIN attempt. A right PIN resets the counter of consecutive wrong attempts. A wrong PIN increments it,
     * and the third consecutive wrong PIN blocks the card. A blocked card always returns {@code false}, and its
     * counter no longer changes.
     */
    public boolean checkPin(String attempt) {
        Objects.requireNonNull(attempt, "attempt");
        if (blocked) {
            return false;
        }
        if (pin.equals(attempt)) {
            consecutiveWrongPins = 0;
            return true;
        }
        consecutiveWrongPins++;
        if (consecutiveWrongPins >= MAX_CONSECUTIVE_WRONG_PINS) {
            blocked = true;
        }
        return false;
    }

    /**
     * Takes {@code amount} off the balance.
     *
     * @throws IllegalArgumentException if the amount is zero, negative, has more than 2 decimals or exceeds the balance
     */
    public void debit(BigDecimal amount) {
        BigDecimal cents = requireCents(amount, "amount");
        if (cents.signum() <= 0) {
            throw new IllegalArgumentException("A debit must be positive, got " + cents.toPlainString());
        }
        if (cents.compareTo(balance) > 0) {
            throw new IllegalArgumentException(
                    "Cannot debit %s from a balance of %s".formatted(cents.toPlainString(), balance.toPlainString()));
        }
        balance = balance.subtract(cents);
    }

    /**
     * Returns {@code value} with scale 2. Rejects values that have more than 2 significant decimals, so that
     * {@code 12.5} and {@code 12.500} are accepted and {@code 12.505} is not. Comparing with {@code equals} would
     * make {@code 12.5} and {@code 12.50} different, so we normalise the scale once, here.
     */
    static BigDecimal requireCents(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(name + " has at most 2 decimals, got " + value.toPlainString());
        }
        return value.setScale(2);
    }
}
