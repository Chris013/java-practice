package io.github.chris013.practice.task40;

import java.math.BigDecimal;

/** Given. A coupon as the shop knows it: a normalized code, a discount type and that discount's value. */
public record Coupon(String code, DiscountType type, BigDecimal value) {
}
