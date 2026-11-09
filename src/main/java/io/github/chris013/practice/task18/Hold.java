package io.github.chris013.practice.task18;

import java.time.Instant;
import java.util.Objects;

/**
 * Given. A customer's temporary claim on a seat while they pay. The hold stops counting at {@code expiresAt}.
 */
public record Hold(SeatId seat, CustomerId customer, Instant expiresAt) {

    public Hold {
        Objects.requireNonNull(seat, "seat");
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }
}
