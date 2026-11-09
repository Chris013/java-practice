package io.github.chris013.practice.task09;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The person who wants to insure a car.
 *
 * @param birthDate            used for the age rules
 * @param licensedSince        the day the driving licence was issued
 * @param claimsInLastFiveYears number of claims we paid out in the last five years, never negative
 * @param postcode             five digits; not used for pricing yet
 */
public record Policyholder(LocalDate birthDate, LocalDate licensedSince, int claimsInLastFiveYears, String postcode) {

    public Policyholder {
        Objects.requireNonNull(birthDate, "birthDate");
        Objects.requireNonNull(licensedSince, "licensedSince");
        Objects.requireNonNull(postcode, "postcode");
        if (claimsInLastFiveYears < 0) {
            throw new IllegalArgumentException("claimsInLastFiveYears must not be negative: " + claimsInLastFiveYears);
        }
        if (!postcode.matches("\\d{5}")) {
            throw new IllegalArgumentException("postcode must have exactly five digits: " + postcode);
        }
    }
}
