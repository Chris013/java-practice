package io.github.chris013.practice.task09;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Calculates the annual car insurance premium.
 *
 * <p>Start with the base premium of the coverage, multiply in every risk factor, and round
 * to cents once at the very end.
 */
public final class PremiumCalculator {

    public BigDecimal annualPremium(Policyholder holder, Vehicle vehicle, Coverage coverage, LocalDate policyStart) {
        Objects.requireNonNull(holder, "holder");
        Objects.requireNonNull(vehicle, "vehicle");
        Objects.requireNonNull(coverage, "coverage");
        Objects.requireNonNull(policyStart, "policyStart");

        BigDecimal premium = coverage.basePremium();

        // age on the day the policy starts
        int age = policyStart.getYear() - holder.birthDate().getYear();
        if (age < 18) {
            throw new UninsurableException("Drivers must be 18 or older, this one is " + age);
        }
        // young drivers
        if (age <= 24) {
            premium = premium.multiply(new BigDecimal("1.50"));
        } else if (age >= 70) {
            // senior drivers
            premium = premium.multiply(new BigDecimal("1.20"));
        }

        // inexperienced drivers: fewer than three full years with a licence
        int experience = Period.between(holder.licensedSince(), policyStart).getYears();
        if (experience < 3) {
            premium = premium.multiply(new BigDecimal("1.30"));
        }

        // claims in the last five years
        int claims = holder.claimsInLastFiveYears();
        if (claims >= 3) {
            throw new UninsurableException("Three or more claims in five years");
        } else if (claims == 2) {
            premium = premium.multiply(new BigDecimal("1.50"));
        } else if (claims == 1) {
            premium = premium.multiply(new BigDecimal("1.20"));
        }

        // power
        if (vehicle.horsepower() > 200) {
            premium = premium.multiply(new BigDecimal("1.40"));
        } else if (vehicle.horsepower() > 150) {
            premium = premium.multiply(new BigDecimal("1.20"));
        }

        if (coverage == Coverage.FULL && vehicle.valueEuros() > 100_000) {
            throw new UninsurableException("Full cover is not offered for cars worth more than 100000 EUR");
        }

        return premium.setScale(2, RoundingMode.HALF_UP);
    }
}
