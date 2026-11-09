package io.github.chris013.practice.task22;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;

/**
 * Hashes passwords with plain SHA-256.
 *
 * <p>For the exercise only; use bcrypt/argon2 in production. SHA-256 is fast on purpose, which
 * is exactly what a password hash must not be: an attacker with the database can try billions of
 * guesses per second. A real hasher is slow and salted.
 */
public final class Sha256PasswordHasher implements PasswordHasher {

    @Override
    public String hash(String raw) {
        return Sha256.hex(Objects.requireNonNull(raw, "raw"));
    }

    @Override
    public boolean matches(String raw, String hash) {
        byte[] computed = hash(raw).getBytes(StandardCharsets.UTF_8);
        byte[] expected = Objects.requireNonNull(hash, "hash").getBytes(StandardCharsets.UTF_8);
        // Constant-time comparison, so the time taken does not reveal how many leading characters matched.
        return MessageDigest.isEqual(computed, expected);
    }
}
