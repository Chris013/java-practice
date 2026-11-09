package io.github.chris013.practice.task22;

import java.security.SecureRandom;
import java.util.Base64;

/** Production token generator: 32 bytes from {@link SecureRandom}, URL-safe Base64 without padding. */
public final class SecureTokenGenerator implements TokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
