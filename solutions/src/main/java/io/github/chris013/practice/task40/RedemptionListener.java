package io.github.chris013.practice.task40;

import java.math.BigDecimal;

/**
 * Given. Called once for every redemption, in registration order.
 *
 * <p>The second argument is the final amount the customer pays (after the discount and shipping).
 * The parameter name {@code orderTotal} is a leftover from an older version of this interface.
 */
@FunctionalInterface
public interface RedemptionListener {
    void redeemed(Coupon coupon, BigDecimal orderTotal);
}
