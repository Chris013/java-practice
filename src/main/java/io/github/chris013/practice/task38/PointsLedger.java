package io.github.chris013.practice.task38;

import java.util.List;
import java.util.Map;

/**
 * The loyalty points of our café members, keyed by member name.
 *
 * <p>Not thread-safe: a {@code PointsLedger} must not be used from several threads at once
 * without external synchronization.
 *
 * <p>Task 38: read {@code tasks/38-loyalty-points-map-api.md} first.
 */
public final class PointsLedger {

    /**
     * Adds points to a member, who becomes a member if they were not one yet.
     *
     * @throws IllegalArgumentException if {@code points} is not positive or the name is blank
     * @throws NullPointerException if {@code member} is null
     * @throws ArithmeticException if the new balance would not fit in an {@code int}
     */
    public void earn(String member, int points) {
        throw new UnsupportedOperationException("TODO: Task 38");
    }

    /** Returns the balance of a member, or 0 for a name the ledger does not know. */
    public int balance(String member) {
        throw new UnsupportedOperationException("TODO: Task 38");
    }

    /**
     * Removes points from a member. A member whose balance reaches exactly 0 is removed from the ledger.
     *
     * @throws InsufficientPointsException if the balance is lower than {@code points}; nothing changes
     * @throws IllegalArgumentException if {@code points} is not positive
     */
    public void redeem(String member, int points) {
        throw new UnsupportedOperationException("TODO: Task 38");
    }

    /** Returns whether the ledger holds a balance for this member. */
    public boolean isMember(String member) {
        throw new UnsupportedOperationException("TODO: Task 38");
    }

    /**
     * Removes every member whose balance is strictly below {@code threshold}.
     *
     * @throws IllegalArgumentException if {@code threshold} is not positive
     */
    public void expireBelow(int threshold) {
        throw new UnsupportedOperationException("TODO: Task 38");
    }

    /**
     * Adds {@code percent} of every balance, rounded down to whole points.
     *
     * @throws IllegalArgumentException if {@code percent} is not between 1 and 100
     * @throws ArithmeticException if any new balance would not fit in an {@code int}; nothing changes
     */
    public void applyBonusPercent(int percent) {
        throw new UnsupportedOperationException("TODO: Task 38");
    }

    /**
     * Groups the members by tier. Only tiers with at least one member appear, in enum order.
     * Names are sorted alphabetically. The returned map and lists are unmodifiable copies.
     */
    public Map<Tier, List<String>> membersByTier() {
        throw new UnsupportedOperationException("TODO: Task 38");
    }

    /** Returns an unmodifiable copy of all balances that does not change when the ledger changes. */
    public Map<String, Integer> snapshot() {
        throw new UnsupportedOperationException("TODO: Task 38");
    }

    /** Returns the number of members, that is, the entries with a balance. */
    public int memberCount() {
        throw new UnsupportedOperationException("TODO: Task 38");
    }
}
