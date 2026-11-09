package io.github.chris013.practice.task14;

import java.util.Set;

/**
 * Answers one question for the whole shop: is this flag on for this user?
 *
 * <p>Callers never see rules, parsers or hashing. They parse a configuration once, keep the instance, and ask
 * {@link #isEnabled}. The instance is immutable, so it can be shared between request threads.
 *
 * <p>Task 14: read {@code tasks/14-feature-flags-deep-module.md} first.
 */
public final class FeatureFlags {

    /**
     * Parses a configuration text in the line-based format described in the task.
     *
     * @throws FlagConfigurationException if the text is invalid; the message starts with {@code line <n>: }
     */
    public static FeatureFlags parse(String configuration) {
        throw new UnsupportedOperationException("TODO: Task 14");
    }

    /**
     * Returns whether the flag is on for this context. An unknown flag name returns {@code false}.
     */
    public boolean isEnabled(String flagName, EvaluationContext context) {
        throw new UnsupportedOperationException("TODO: Task 14");
    }

    /**
     * The names of all flags in the configuration, in the order they are written. The set cannot be modified.
     */
    public Set<String> flagNames() {
        throw new UnsupportedOperationException("TODO: Task 14");
    }
}
