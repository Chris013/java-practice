package io.github.chris013.practice.task28;

/**
 * Summary of a non-empty set of readings. {@code standardDeviation} is the population standard deviation
 * (divide by {@code count}, not {@code count - 1}).
 */
public record Statistics(long count, double min, double max, double mean, double standardDeviation) {

    public Statistics {
        if (count < 1) {
            throw new IllegalArgumentException("count must be at least 1, was " + count);
        }
    }
}
