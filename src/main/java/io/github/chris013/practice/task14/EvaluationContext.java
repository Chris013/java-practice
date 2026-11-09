package io.github.chris013.practice.task14;

import java.util.Optional;

/**
 * Who is asking: a known user, possibly with a country, or an anonymous visitor with a country.
 *
 * <p>This is a final class and not a record on purpose: a record would expose a canonical constructor that takes
 * nullable values or {@code Optional}s, and callers would have two ways to say the same thing.
 */
public final class EvaluationContext {

    /** A known user. The user id must not be blank; the country may be {@code null} when it is unknown. */
    public static EvaluationContext user(String userId, String country) {
        throw new UnsupportedOperationException("TODO: Task 14");
    }

    /** An anonymous visitor. The country may be {@code null} when it is unknown. */
    public static EvaluationContext anonymous(String country) {
        throw new UnsupportedOperationException("TODO: Task 14");
    }

    /** The user id, or empty for an anonymous visitor. */
    public Optional<String> userId() {
        throw new UnsupportedOperationException("TODO: Task 14");
    }

    /** The ISO 3166-1 alpha-2 country code, or empty when unknown. */
    public Optional<String> country() {
        throw new UnsupportedOperationException("TODO: Task 14");
    }
}
