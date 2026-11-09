package io.github.chris013.practice.task09;

import static io.github.chris013.practice.task09.TestDates.POLICY_START;

import java.time.LocalDate;

/**
 * Builds a {@link Policyholder} for tests.
 *
 * <p>The defaults describe a boring driver: 40 years old, licensed for 20 years, no claims. None
 * of the pricing rules applies to this driver, so a test that does not mention a property gets
 * the base premium. A test should only mention what it is about.
 *
 * <p>Ages are relative to {@link TestDates#POLICY_START}, so {@code aged(30)} is a 30-year-old on
 * the day the policy starts.
 */
final class PolicyholderBuilder {

    private LocalDate birthDate = POLICY_START.minusYears(40);
    private LocalDate licensedSince = POLICY_START.minusYears(20);
    private int claimsInLastFiveYears = 0;
    private String postcode = "10115";

    private PolicyholderBuilder() {
    }

    static PolicyholderBuilder aPolicyholder() {
        return new PolicyholderBuilder();
    }

    /** The driver is this many years old on the policy start. */
    PolicyholderBuilder aged(int years) {
        return bornOn(POLICY_START.minusYears(years));
    }

    PolicyholderBuilder bornOn(LocalDate birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    /** The driver has held a licence for this many full years on the policy start. */
    PolicyholderBuilder licensedForYears(int years) {
        return licensedOn(POLICY_START.minusYears(years));
    }

    PolicyholderBuilder licensedOn(LocalDate licensedSince) {
        this.licensedSince = licensedSince;
        return this;
    }

    PolicyholderBuilder withClaims(int claimsInLastFiveYears) {
        this.claimsInLastFiveYears = claimsInLastFiveYears;
        return this;
    }

    Policyholder build() {
        return new Policyholder(birthDate, licensedSince, claimsInLastFiveYears, postcode);
    }
}
