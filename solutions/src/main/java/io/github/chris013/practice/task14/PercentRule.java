package io.github.chris013.practice.task14;

import java.util.Objects;

/**
 * {@code percent 25}: on for the users whose bucket is below 25.
 *
 * <p>A bucket is a number from 0 to 99 that is fixed for one flag and one user. We hash the flag name together with
 * the user id. Without the flag name, the same users would be first in line for every rollout, and a feature at 10
 * percent would always be the same 10 percent of users as every other one. {@link String#hashCode()} is specified by
 * the JLS, so the bucket is the same on every JVM. A random number generator is not an option: users would flip
 * between variants on every page load.
 *
 * <p>Anonymous visitors have no stable identity, so they never match: a rollout by percentage must not flip a visitor
 * in and out of the feature between page loads.
 */
record PercentRule(String flagName, int percent) implements Rule {

    private static final int BUCKETS = 100;

    PercentRule {
        Objects.requireNonNull(flagName, "flagName");
    }

    @Override
    public boolean matches(EvaluationContext context) {
        return context.userId()
                .map(userId -> bucket(flagName, userId) < percent)
                .orElse(false);
    }

    private static int bucket(String flagName, String userId) {
        // floorMod, not %: hashCode() is often negative, and % would then return a negative bucket.
        return Math.floorMod((flagName + ":" + userId).hashCode(), BUCKETS);
    }
}
