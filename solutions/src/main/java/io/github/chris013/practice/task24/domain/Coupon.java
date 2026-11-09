package io.github.chris013.practice.task24.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * A coupon, replacing the string comparisons of the legacy code. The three variants behave differently, so they
 * are three records and the compiler checks that every {@code switch} over them handles all of them.
 */
public sealed interface Coupon permits Coupon.Percentage, Coupon.FixedAmount, Coupon.NoCoupon {

    /**
     * Reads a customer's code. {@code null} or blank means "no coupon". Empty means the code is unknown.
     * Codes are case-sensitive, as they were in the legacy checkout.
     */
    static Optional<Coupon> parse(String code) {
        if (code == null || code.isBlank()) {
            return Optional.of(new NoCoupon());
        }
        return switch (code) {
            case "SAVE10" -> Optional.of(new Percentage(10));
            case "FIVEOFF" -> Optional.of(new FixedAmount(
                    new Money(new BigDecimal("5.00")), new Money(new BigDecimal("25.00"))));
            default -> Optional.empty();
        };
    }

    /** A coupon can be refused for a basket, e.g. a fixed discount below its minimum subtotal. */
    boolean appliesTo(Money subtotal);

    /** Only meaningful when {@link #appliesTo} is true. */
    Money discountOn(Money subtotal);

    record Percentage(int percent) implements Coupon {

        @Override
        public boolean appliesTo(Money subtotal) {
            return true;
        }

        @Override
        public Money discountOn(Money subtotal) {
            return subtotal.percent(percent);
        }
    }

    /** A fixed amount off. The minimum subtotal guarantees the discount never exceeds the subtotal. */
    record FixedAmount(Money amount, Money minimumSubtotal) implements Coupon {

        @Override
        public boolean appliesTo(Money subtotal) {
            return subtotal.isAtLeast(minimumSubtotal);
        }

        @Override
        public Money discountOn(Money subtotal) {
            return amount;
        }
    }

    record NoCoupon() implements Coupon {

        @Override
        public boolean appliesTo(Money subtotal) {
            return true;
        }

        @Override
        public Money discountOn(Money subtotal) {
            return Money.ZERO;
        }
    }
}
