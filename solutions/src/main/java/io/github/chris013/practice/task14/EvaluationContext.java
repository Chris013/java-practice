package io.github.chris013.practice.task14;

import java.util.Objects;
import java.util.Optional;

/**
 * Who is asking: a known user, possibly with a country, or an anonymous visitor with a country.
 *
 * <p>This is a final class and not a record on purpose: a record would expose a canonical constructor that takes
 * nullable values or {@code Optional}s, and callers would have two ways to say the same thing. Two named factories
 * say exactly what the situation is. The private constructor is the only place where nulls are stored.
 */
public final class EvaluationContext {

    private final String userId;
    private final String country;

    private EvaluationContext(String userId, String country) {
        this.userId = userId;
        this.country = country;
    }

    /** A known user. The user id must not be blank; the country may be {@code null} when it is unknown. */
    public static EvaluationContext user(String userId, String country) {
        Objects.requireNonNull(userId, "userId");
        if (userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return new EvaluationContext(userId, country);
    }

    /** An anonymous visitor. The country may be {@code null} when it is unknown. */
    public static EvaluationContext anonymous(String country) {
        return new EvaluationContext(null, country);
    }

    /** The user id, or empty for an anonymous visitor. */
    public Optional<String> userId() {
        return Optional.ofNullable(userId);
    }

    /** The ISO 3166-1 alpha-2 country code, or empty when unknown. */
    public Optional<String> country() {
        return Optional.ofNullable(country);
    }
}
