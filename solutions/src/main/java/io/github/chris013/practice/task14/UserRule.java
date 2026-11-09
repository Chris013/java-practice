package io.github.chris013.practice.task14;

import java.util.Objects;
import java.util.Set;

/** {@code users alice, bob}: on for these user ids. Anonymous visitors never match. */
record UserRule(Set<String> userIds) implements Rule {

    UserRule {
        Objects.requireNonNull(userIds, "userIds");
        userIds = Set.copyOf(userIds);
    }

    @Override
    public boolean matches(EvaluationContext context, String flagName) {
        return context.userId().filter(userIds::contains).isPresent();
    }
}
