package io.github.chris013.practice.task25.application;

/**
 * Given. The two outcomes of a charge. Both are normal business results, so both are values and not exceptions.
 */
public sealed interface PaymentResult permits PaymentResult.Paid, PaymentResult.Declined {

    record Paid(String transactionId) implements PaymentResult {}

    record Declined(String reason) implements PaymentResult {}
}
