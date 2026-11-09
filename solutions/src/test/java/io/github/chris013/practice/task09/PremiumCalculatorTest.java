package io.github.chris013.practice.task09;

import static io.github.chris013.practice.task09.PolicyholderBuilder.aPolicyholder;
import static io.github.chris013.practice.task09.TestDates.POLICY_START;
import static io.github.chris013.practice.task09.VehicleBuilder.aVehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Specifications for {@link PremiumCalculator}. Every premium is the base premium of
 * {@link Coverage#LIABILITY} (300.00) times the factors of the rule under test.
 */
class PremiumCalculatorTest {

    private final PremiumCalculator calculator = new PremiumCalculator();

    private BigDecimal quote(Policyholder holder, Vehicle vehicle, Coverage coverage) {
        return calculator.annualPremium(holder, vehicle, coverage, POLICY_START);
    }

    private BigDecimal liabilityQuote(Policyholder holder) {
        return quote(holder, aVehicle().build(), Coverage.LIABILITY);
    }

    @Nested
    class BasePremium {

        @ParameterizedTest
        @CsvSource({"LIABILITY, 300.00", "PARTIAL, 420.00", "FULL, 600.00"})
        void depends_only_on_the_coverage_for_a_neutral_driver(Coverage coverage, String expected) {
            BigDecimal premium = quote(aPolicyholder().build(), aVehicle().build(), coverage);

            assertThat(premium).isEqualTo(new BigDecimal(expected));
        }

        @Test
        void the_premium_has_exactly_two_decimal_places() {
            BigDecimal premium = liabilityQuote(aPolicyholder().build());

            assertThat(premium.scale()).isEqualTo(2);
        }
    }

    @Nested
    class Age {

        @Test
        void drivers_under_18_are_uninsurable() {
            // 17 today, and 17 until the birthday tomorrow
            assertThatThrownBy(() -> liabilityQuote(aPolicyholder().bornOn(LocalDate.parse("2008-03-02")).build()))
                    .isInstanceOf(UninsurableException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2008-09-15", "2010-01-01"})
        void drivers_well_under_18_are_uninsurable(String birthDate) {
            assertThatThrownBy(() -> liabilityQuote(aPolicyholder().bornOn(LocalDate.parse(birthDate)).build()))
                    .isInstanceOf(UninsurableException.class);
        }

        /** Births are given relative to the policy start, 2026-03-01. */
        @ParameterizedTest(name = "born {0} pays {1}")
        @CsvSource({
            "2008-03-01, 450.00", // 18 today: insurable, young-driver factor
            "2001-03-02, 450.00", // 24 until tomorrow
            "2001-03-01, 300.00", // 25 today: the young-driver factor is gone
            "1956-03-02, 300.00", // 69 until tomorrow
            "1956-03-01, 360.00"  // 70 today: senior factor
        })
        void age_factor_follows_the_birthday_on_the_policy_start(String birthDate, String expected) {
            BigDecimal premium = liabilityQuote(aPolicyholder().bornOn(LocalDate.parse(birthDate)).build());

            assertThat(premium).isEqualTo(new BigDecimal(expected));
        }

        @Test
        void young_driver_factor_applies_until_the_25th_birthday_not_until_new_year() {
            // Born in September: 24 on 1 March 2026, 25 only from September 2026.
            BigDecimal premium = liabilityQuote(aPolicyholder().bornOn(LocalDate.of(2001, 9, 15)).build());

            assertThat(premium).isEqualTo(new BigDecimal("450.00")); // 300.00 x 1.50
        }

        @Test
        void the_age_is_counted_on_the_policy_start_date_not_on_today() {
            LocalDate birthDate = LocalDate.of(2001, 3, 1);

            BigDecimal beforeBirthday = calculator.annualPremium(
                    aPolicyholder().bornOn(birthDate).build(), aVehicle().build(), Coverage.LIABILITY,
                    LocalDate.of(2026, 2, 28));
            BigDecimal onBirthday = calculator.annualPremium(
                    aPolicyholder().bornOn(birthDate).build(), aVehicle().build(), Coverage.LIABILITY,
                    LocalDate.of(2026, 3, 1));

            assertThat(beforeBirthday).isEqualTo(new BigDecimal("450.00")); // still 24
            assertThat(onBirthday).isEqualTo(new BigDecimal("300.00"));     // 25
        }
    }

    @Nested
    class DrivingExperience {

        @ParameterizedTest(name = "licensed for {0} years pays {1}")
        @CsvSource({"2, 390.00", "3, 300.00"})
        void fewer_than_three_full_years_costs_thirty_percent_more(int years, String expected) {
            BigDecimal premium = liabilityQuote(aPolicyholder().licensedForYears(years).build());

            assertThat(premium).isEqualTo(new BigDecimal(expected));
        }

        @ParameterizedTest(name = "licensed since {0} pays {1}")
        @CsvSource({
            "2023-03-02, 390.00", // 2 years and 11 months: still only 2 full years
            "2023-03-01, 300.00"  // exactly 3 years
        })
        void only_complete_years_count(String licensedSince, String expected) {
            BigDecimal premium = liabilityQuote(aPolicyholder().licensedOn(LocalDate.parse(licensedSince)).build());

            assertThat(premium).isEqualTo(new BigDecimal(expected));
        }
    }

    @Nested
    class Claims {

        @ParameterizedTest(name = "{0} claims pays {1}")
        @CsvSource({"0, 300.00", "1, 360.00", "2, 450.00"})
        void each_claim_adds_a_surcharge_up_to_two_claims(int claims, String expected) {
            BigDecimal premium = liabilityQuote(aPolicyholder().withClaims(claims).build());

            assertThat(premium).isEqualTo(new BigDecimal(expected));
        }

        @ParameterizedTest
        @ValueSource(ints = {3, 4, 10})
        void three_or_more_claims_are_uninsurable(int claims) {
            assertThatThrownBy(() -> liabilityQuote(aPolicyholder().withClaims(claims).build()))
                    .isInstanceOf(UninsurableException.class);
        }
    }

    @Nested
    class Horsepower {

        @ParameterizedTest(name = "{0} hp pays {1}")
        @CsvSource({
            "150, 300.00", // up to 150: no factor
            "151, 360.00", // 151 to 200: x 1.20
            "200, 360.00",
            "201, 420.00"  // above 200: x 1.40
        })
        void the_factor_steps_at_150_and_200_horsepower(int horsepower, String expected) {
            BigDecimal premium = quote(aPolicyholder().build(), aVehicle().withHorsepower(horsepower).build(),
                    Coverage.LIABILITY);

            assertThat(premium).isEqualTo(new BigDecimal(expected));
        }
    }

    @Nested
    class VehicleValue {

        @Test
        void full_cover_is_allowed_for_a_car_worth_exactly_100000_euros() {
            BigDecimal premium = quote(aPolicyholder().build(), aVehicle().worth(100_000).build(), Coverage.FULL);

            assertThat(premium).isEqualTo(new BigDecimal("600.00"));
        }

        @Test
        void full_cover_is_uninsurable_for_a_car_worth_more_than_100000_euros() {
            assertThatThrownBy(() -> quote(aPolicyholder().build(), aVehicle().worth(100_001).build(), Coverage.FULL))
                    .isInstanceOf(UninsurableException.class);
        }

        @ParameterizedTest
        @CsvSource({"LIABILITY, 300.00", "PARTIAL, 420.00"})
        void the_value_limit_only_applies_to_full_cover(Coverage coverage, String expected) {
            BigDecimal premium = quote(aPolicyholder().build(), aVehicle().worth(100_001).build(), coverage);

            assertThat(premium).isEqualTo(new BigDecimal(expected));
        }
    }

    @Nested
    class FactorsCombined {

        @Test
        void factors_multiply_together() {
            // 1.50 (age 24) x 1.30 (two years licensed) x 1.20 (one claim) x 1.40 (201 hp)
            Policyholder holder = aPolicyholder()
                    .bornOn(LocalDate.of(2001, 3, 2))
                    .licensedForYears(2)
                    .withClaims(1)
                    .build();

            BigDecimal premium = quote(holder, aVehicle().withHorsepower(201).build(), Coverage.LIABILITY);

            assertThat(premium).isEqualTo(new BigDecimal("982.80")); // 300.00 x 3.276
        }
    }
}
