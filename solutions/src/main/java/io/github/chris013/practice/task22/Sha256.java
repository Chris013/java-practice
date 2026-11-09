package io.github.chris013.practice.task22;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Package-private helper shared by the token store key and the exercise password hasher. */
final class Sha256 {

    private Sha256() {
    }

    /** Lower-case hex SHA-256 of the UTF-8 bytes of {@code text}: 64 characters. */
    static String hex(String text) {
        Objects.requireNonNull(text, "text");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("every Java runtime provides SHA-256", e);
        }
    }
}
