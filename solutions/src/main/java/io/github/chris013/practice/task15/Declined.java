package io.github.chris013.practice.task15;

import java.util.Objects;

/** The card could not be used at all. Nothing was charged. */
public record Declined(DeclineReason reason) implements RedemptionResult {

    public Declined {
        Objects.requireNonNull(reason, "reason");
    }
}
