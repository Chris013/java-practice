package io.github.chris013.practice.task14;

import java.util.Map;
import java.util.Objects;
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

    private final Map<String, Flag> flags;

    private FeatureFlags(Map<String, Flag> flags) {
        this.flags = flags;
    }

    /**
     * Parses a configuration text in the line-based format described in the task.
     *
     * @throws FlagConfigurationException if the text is invalid; the message starts with {@code line <n>: }
     */
    public static FeatureFlags parse(String configuration) {
        Objects.requireNonNull(configuration, "configuration");
        return new FeatureFlags(FlagConfigParser.parse(configuration));
    }

    /**
     * Returns whether the flag is on for this context. An unknown flag name returns {@code false}.
     *
     * <p>That is a deliberate choice: a flag that nobody has written down is, for every practical purpose, a flag
     * that is off. Callers never need a try/catch around a flag check. The trade-off is that a typo in a flag name
     * fails silently, so tests should cover the flag names they depend on.
     */
    public boolean isEnabled(String flagName, EvaluationContext context) {
        Objects.requireNonNull(flagName, "flagName");
        Objects.requireNonNull(context, "context");
        Flag flag = flags.get(flagName);
        return flag != null && flag.isOnFor(context);
    }

    /** The names of all flags in the configuration. */
    public Set<String> flagNames() {
        return Set.copyOf(flags.keySet());
    }
}
