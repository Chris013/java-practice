package io.github.chris013.practice.task01;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * An amount of money in a specific currency.
 *
 * <p>Immutable value object. The amount always has exactly as many decimal places as the currency's minor unit
 * (2 for EUR, 0 for JPY). That single invariant is what makes {@code equals}, {@code hashCode} and
 * {@code toString} behave by value: {@code 10}, {@code 10.0} and {@code 10.00} EUR are all the same money.
 */
public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = scaledToMinorUnit(amount, currency);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    /** Multiplies by an arbitrary factor (a tax rate, a discount, ...). The caller decides how to round. */
    public Money multipliedBy(BigDecimal factor, RoundingMode rounding) {
        Objects.requireNonNull(factor, "factor");
        Objects.requireNonNull(rounding, "rounding");
        BigDecimal exact = amount.multiply(factor);
        return new Money(exact.setScale(amount.scale(), rounding), currency);
    }

    /**
     * Splits this amount into parts proportional to the given ratios without losing or creating a single minor unit.
     * Units that cannot be divided evenly go to the first parties with a non-zero ratio.
     *
     * <p>Example: EUR 100.00 allocated 1:1:1 is EUR 33.34, EUR 33.33, EUR 33.33.
     */
    public List<Money> allocate(int... ratios) {
        requireUsableRatios(ratios);
        BigInteger total = amount.unscaledValue();
        BigInteger ratioSum = BigInteger.valueOf(IntStream.of(ratios).asLongStream().sum());

        List<BigInteger> shares = new ArrayList<>(ratios.length);
        for (int ratio : ratios) {
            shares.add(total.multiply(BigInteger.valueOf(ratio)).divide(ratioSum));
        }

        BigInteger leftover = total.subtract(shares.stream().reduce(BigInteger.ZERO, BigInteger::add));
        BigInteger oneUnit = BigInteger.valueOf(leftover.signum());
        int unitsToHandOut = leftover.abs().intValueExact();
        for (int i = 0; unitsToHandOut > 0; i++) {
            if (ratios[i] > 0) {
                shares.set(i, shares.get(i).add(oneUnit));
                unitsToHandOut--;
            }
        }

        return shares.stream().map(this::inMinorUnits).toList();
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) > 0;
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }

    private Money inMinorUnits(BigInteger units) {
        return new Money(new BigDecimal(units, amount.scale()), currency);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }

    private static void requireUsableRatios(int[] ratios) {
        if (ratios.length == 0) {
            throw new IllegalArgumentException("At least one ratio is required");
        }
        if (IntStream.of(ratios).anyMatch(ratio -> ratio < 0)) {
            throw new IllegalArgumentException("Ratios must not be negative");
        }
        if (IntStream.of(ratios).allMatch(ratio -> ratio == 0)) {
            throw new IllegalArgumentException("At least one ratio must be positive");
        }
    }

    private static BigDecimal scaledToMinorUnit(BigDecimal amount, Currency currency) {
        int minorUnitDigits = currency.getDefaultFractionDigits();
        if (minorUnitDigits < 0) {
            throw new IllegalArgumentException("Not a currency you can pay with: " + currency);
        }
        try {
            return amount.setScale(minorUnitDigits, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException tooPrecise) {
            throw new IllegalArgumentException(
                    "%s has more decimal places than %s allows (%d)"
                            .formatted(amount.toPlainString(), currency.getCurrencyCode(), minorUnitDigits),
                    tooPrecise);
        }
    }
}
