package io.github.chris013.practice.task40;

/** Legacy. One rule that a normalized coupon code must satisfy. */
interface ValidationRule {

    boolean holds(String normalizedCode);
}
