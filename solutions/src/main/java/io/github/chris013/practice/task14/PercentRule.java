package io.github.chris013.practice.task14;

/**
 * {@code percent 25}: on for the users whose bucket is below 25. Anonymous visitors have no stable identity, so they
 * never match: a rollout by percentage must not flip a visitor in and out of the feature between page loads.
 */
record PercentRule(int percent) implements Rule {

    @Override
    public boolean matches(EvaluationContext context, String flagName) {
        return context.userId()
                .map(userId -> Bucketing.of(flagName, userId) < percent)
                .orElse(false);
    }
}
