package io.github.chris013.practice.task24;

/** The payment provider refused the card. Nothing was charged. */
public class PaymentDeclinedException extends RuntimeException {

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
