package io.github.chris013.practice.task09;

/**
 * Thrown when we refuse to quote a premium at all. This is a refusal, not a price.
 */
public final class UninsurableException extends RuntimeException {

    public UninsurableException(String message) {
        super(message);
    }
}
