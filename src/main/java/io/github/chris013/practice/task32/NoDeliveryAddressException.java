package io.github.chris013.practice.task32;

import java.io.Serial;

/** The customer has no pickup point and no usable home address, so the parcel cannot be routed. */
public class NoDeliveryAddressException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NoDeliveryAddressException(CustomerId id) {
        super("no delivery address for customer " + id);
    }
}
