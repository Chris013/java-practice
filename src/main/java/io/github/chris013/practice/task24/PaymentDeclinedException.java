package io.github.chris013.practice.task24;

import java.io.Serial;

/** The payment provider refused the card. Nothing was charged. */
public class PaymentDeclinedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PaymentDeclinedException(String message) {
        super(message);
    }
}
