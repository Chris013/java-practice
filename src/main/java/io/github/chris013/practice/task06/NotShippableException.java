package io.github.chris013.practice.task06;

import java.io.Serial;

/** A valid shipment that we cannot ship to the requested zone, or that exceeds what its service can carry. */
public final class NotShippableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NotShippableException(String reason) {
        super(reason);
    }
}
