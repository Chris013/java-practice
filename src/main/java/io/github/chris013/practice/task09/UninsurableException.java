package io.github.chris013.practice.task09;

import java.io.Serial;

/**
 * Thrown when we refuse to quote a premium at all. This is a refusal, not a price.
 */
public final class UninsurableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UninsurableException(String message) {
        super(message);
    }
}
