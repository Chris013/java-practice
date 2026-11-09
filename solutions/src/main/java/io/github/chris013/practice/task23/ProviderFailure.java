package io.github.chris013.practice.task23;

import java.util.Objects;

/** Why one provider did not contribute quotes. Task 23. */
public record ProviderFailure(String provider, Reason reason, String detail) {

    public enum Reason {
        /** No answer within the budget. The provider was cancelled. */
        TIMEOUT,
        /** The provider answered with an exception. */
        ERROR
    }

    public ProviderFailure {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(detail, "detail");
    }
}
