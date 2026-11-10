package io.github.chris013.practice.task40;

/** Given. The three kinds of discount the shop offers. */
public enum DiscountType {
    /** The value is a percentage of the order total, 1 to 100. */
    PERCENT,
    /** The value is an amount off the order total, more than zero. */
    FIXED,
    /** The value is always zero; the shipping cost is waived. */
    FREE_SHIPPING
}
