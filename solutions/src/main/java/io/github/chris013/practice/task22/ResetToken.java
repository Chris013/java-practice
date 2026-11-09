package io.github.chris013.practice.task22;

import java.time.Instant;
import java.util.Objects;

/**
 * A password reset token as it is stored: the SHA-256 hash of the token, never the token itself.
 * If the store leaks, the attacker gets hashes they cannot use to reset anyone's password.
 */
public record ResetToken(String tokenHash, UserId userId, Instant expiresAt) {

    public ResetToken {
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /** Expired from the moment {@code now} reaches {@code expiresAt}, not one tick later. */
    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
