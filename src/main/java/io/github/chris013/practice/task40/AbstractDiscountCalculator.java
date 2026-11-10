package io.github.chris013.practice.task40;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Legacy. A template method: the fixed steps of "apply a discount, then add shipping". Subclasses fill in the hooks. */
abstract class AbstractDiscountCalculator {

    final BigDecimal value;

    AbstractDiscountCalculator(BigDecimal value) {
        this.value = value;
    }

    /** The template method. Returns the amount to pay, with scale 2. */
    final BigDecimal apply(BigDecimal orderTotal, BigDecimal shippingCost) {
        BigDecimal discounted = discountedTotal(orderTotal);
        BigDecimal payable = chargesShipping() ? discounted.add(shippingCost) : discounted;
        return payable.setScale(2, RoundingMode.HALF_UP);
    }

    /** Hook: the order total after the discount, before shipping. */
    abstract BigDecimal discountedTotal(BigDecimal orderTotal);

    /** Hook: whether the shipping cost is added. */
    abstract boolean chargesShipping();

    /** Hook: whether the coupon's value is acceptable for this kind of discount. */
    abstract boolean allowsValue();
}
