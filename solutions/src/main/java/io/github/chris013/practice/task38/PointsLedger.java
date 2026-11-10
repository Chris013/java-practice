package io.github.chris013.practice.task38;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The loyalty points of our café members, keyed by member name.
 *
 * <p>Not thread-safe: a {@code PointsLedger} must not be used from several threads at once
 * without external synchronization.
 *
 * <p>Invariant: every balance in the map is positive. A member with no points has no entry,
 * so {@link #memberCount()} and the monthly export never list dead members.
 *
 * <p>Task 38: read {@code tasks/38-loyalty-points-map-api.md} first.
 */
public final class PointsLedger {

    private final Map<String, Integer> balances = new HashMap<>();

    /**
     * Adds points to a member, who becomes a member if they were not one yet.
     *
     * @throws IllegalArgumentException if {@code points} is not positive or the name is blank
     * @throws NullPointerException if {@code member} is null
     * @throws ArithmeticException if the new balance would not fit in an {@code int}
     */
    public void earn(String member, int points) {
        String name = normalise(member);
        requirePositive(points, "points");
        // merge calls the remapping function only when the key is present. If addExact throws,
        // HashMap leaves the old mapping in place, so the balance is unchanged.
        balances.merge(name, points, Math::addExact);
    }

    /** Returns the balance of a member, or 0 for a name the ledger does not know. */
    public int balance(String member) {
        return balances.getOrDefault(normalise(member), 0);
    }

    /**
     * Removes points from a member. A member whose balance reaches exactly 0 is removed from the ledger.
     *
     * @throws InsufficientPointsException if the balance is lower than {@code points}; nothing changes
     * @throws IllegalArgumentException if {@code points} is not positive
     */
    public void redeem(String member, int points) {
        String name = normalise(member);
        requirePositive(points, "points");
        int available = balances.getOrDefault(name, 0);
        if (available < points) {
            throw new InsufficientPointsException(name, available, points);
        }
        // Returning null from the remapping function removes the mapping.
        balances.computeIfPresent(name, (key, held) -> held == points ? null : held - points);
    }

    /** Returns whether the ledger holds a balance for this member. */
    public boolean isMember(String member) {
        return balances.containsKey(normalise(member));
    }

    /**
     * Removes every member whose balance is strictly below {@code threshold}.
     *
     * @throws IllegalArgumentException if {@code threshold} is not positive
     */
    public void expireBelow(int threshold) {
        requirePositive(threshold, "threshold");
        balances.values().removeIf(points -> points < threshold);
    }

    /**
     * Adds {@code percent} of every balance, rounded down to whole points.
     *
     * @throws IllegalArgumentException if {@code percent} is not between 1 and 100
     * @throws ArithmeticException if any new balance would not fit in an {@code int}; nothing changes
     */
    public void applyBonusPercent(int percent) {
        if (percent < 1 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 1 and 100: " + percent);
        }
        // The bonus only grows with the balance, so checking the largest balance covers them all.
        // replaceAll would otherwise stop halfway and leave the ledger half-updated.
        long largest = balances.values().stream().mapToLong(Integer::longValue).max().orElse(0L);
        if (withBonus(largest, percent) > Integer.MAX_VALUE) {
            throw new ArithmeticException("bonus would overflow the balance of a member");
        }
        balances.replaceAll((member, points) -> (int) withBonus(points, percent));
    }

    /**
     * Groups the members by tier. Only tiers with at least one member appear, in enum order.
     * Names are sorted alphabetically. The returned map and lists are unmodifiable copies.
     */
    public Map<Tier, List<String>> membersByTier() {
        Map<Tier, List<String>> byTier = new EnumMap<>(Tier.class);
        // TreeMap sorts the names, so each list is sorted as it fills up.
        for (Map.Entry<String, Integer> entry : new TreeMap<>(balances).entrySet()) {
            Tier tier = Tier.forBalance(entry.getValue());
            byTier.computeIfAbsent(tier, t -> new ArrayList<>()).add(entry.getKey());
        }
        byTier.replaceAll((tier, names) -> List.copyOf(names));
        return Collections.unmodifiableMap(byTier);
    }

    /** Returns an unmodifiable copy of all balances that does not change when the ledger changes. */
    public Map<String, Integer> snapshot() {
        return Map.copyOf(balances);
    }

    /** Returns the number of members, that is, the entries with a balance. */
    public int memberCount() {
        return balances.size();
    }

    private static String normalise(String member) {
        String name = Objects.requireNonNull(member, "member").strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("member name must not be blank");
        }
        return name;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("%s must be positive: %d".formatted(name, value));
        }
    }

    private static long withBonus(long points, int percent) {
        return points + points * percent / 100;
    }
}
