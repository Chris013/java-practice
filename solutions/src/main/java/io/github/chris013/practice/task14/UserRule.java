package io.github.chris013.practice.task14;

import java.util.List;
import java.util.Objects;

/** {@code users alice, bob}: on for these user ids. Anonymous visitors never match. */
record UserRule(List<String> userIds) implements Rule {

    UserRule {
        Objects.requireNonNull(userIds, "userIds");
        userIds = List.copyOf(userIds);
    }

    @Override
    public boolean matches(EvaluationContext context) {
        return context.userId().filter(userIds::contains).isPresent();
    }
}
