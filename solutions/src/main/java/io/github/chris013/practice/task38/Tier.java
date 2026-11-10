package io.github.chris013.practice.task38;

/** The loyalty tier of a member, derived from their current point balance. */
public enum Tier {

    /** Below 1 000 points. */
    BRONZE,
    /** From 1 000 up to and including 4 999 points. */
    SILVER,
    /** From 5 000 points. */
    GOLD;

    /**
     * Returns the tier for a balance.
     *
     * @throws IllegalArgumentException if {@code points} is negative
     */
    public static Tier forBalance(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("points must not be negative: " + points);
        }
        if (points < 1_000) {
            return BRONZE;
        }
        if (points < 5_000) {
            return SILVER;
        }
        return GOLD;
    }
}
