package io.github.chris013.practice.task24.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * The money side of an order, computed before anything is reserved or charged. This is the "phase 1" of the
 * split: no collaborators, no clock, no exceptions for business outcomes. It is easy to test because it is pure.
 */
public record OrderPricing(Money subtotal, Money discount, Money shipping, Money total) {

    /** Shipping is free from this amount, measured after the discount. */
    public static final Money FREE_SHIPPING_FROM = new Money(new BigDecimal("50.00"));

    public static Money subtotalOf(List<OrderLine> lines) {
        return lines.stream().map(OrderLine::lineTotal).reduce(Money.ZERO, Money::plus);
    }

    /** Caller must have checked {@link Coupon#appliesTo} already. */
    public static OrderPricing calculate(Money subtotal, Country country, Coupon coupon) {
        Objects.requireNonNull(subtotal, "subtotal");
        Objects.requireNonNull(country, "country");
        Objects.requireNonNull(coupon, "coupon");

        Money discount = coupon.discountOn(subtotal);
        Money discounted = subtotal.minus(discount);
        Money shipping = discounted.isAtLeast(FREE_SHIPPING_FROM) ? Money.ZERO : country.shippingCost();
        return new OrderPricing(subtotal, discount, shipping, discounted.plus(shipping));
    }
}
