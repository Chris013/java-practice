package io.github.chris013.practice.task14;

/**
 * Maps a (flag, user) pair to a bucket from 0 to 99. The same pair always gets the same bucket, so a user who is on
 * at 25 percent stays on at 50 percent, and nobody flips between variants on a reload.
 *
 * <p>We hash the flag name together with the user id. Without the flag name, the same users would be first in line
 * for every rollout, and a feature at 10 percent would always be the same 10 percent of users as every other one.
 * {@link String#hashCode()} is specified by the JLS, so the bucket is the same on every JVM. A random number
 * generator is not an option: users would flip between variants on every page load.
 */
final class Bucketing {

    static final int BUCKETS = 100;

    private Bucketing() {
    }

    static int of(String flagName, String userId) {
        // floorMod, not %: hashCode() is often negative, and % would then return a negative bucket.
        return Math.floorMod((flagName + ":" + userId).hashCode(), BUCKETS);
    }
}
