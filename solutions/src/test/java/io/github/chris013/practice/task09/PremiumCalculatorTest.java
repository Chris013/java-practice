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
            Policyholder holder = aPolicyholder().bornOn(POLICY_START.minusYears(18).plusDays(1)).build();

            assertThatThrownBy(() -> liabilityQuote(holder))
                    .isInstanceOf(UninsurableException.class);
        }

        @ParameterizedTest
        @ValueSource(ints = {16, 17})
        void drivers_aged_16_or_17_are_uninsurable(int age) {
            assertThatThrownBy(() -> liabilityQuote(aPolicyholder().aged(age).licensedForYears(0).build()))
                    .isInstanceOf(UninsurableException.class);
        }

        @ParameterizedTest(name = "aged {0} pays {1}")
        @CsvSource({
            "18, 450.00", // insurable from the 18th birthday: young-driver factor
            "24, 450.00", // last year with the young-driver factor
            "25, 300.00", // the young-driver factor is gone on the 25th birthday
            "69, 300.00", // last year before the senior factor
            "70, 360.00"  // senior factor from the 70th birthday
        })
        void the_age_factor_follows_the_age_bands(int age, String expected) {
            BigDecimal premium = liabilityQuote(aPolicyholder().aged(age).build());

            assertThat(premium).isEqualTo(new BigDecimal(expected));
        }

        @Test
        void the_young_driver_factor_still_applies_the_day_before_the_25th_birthday() {
            // Born the day after 25 years before the policy start: 24 today, 25 tomorrow.
            Policyholder holder = aPolicyholder().bornOn(POLICY_START.minusYears(25).plusDays(1)).build();

            assertThat(liabilityQuote(holder)).isEqualTo(new BigDecimal("450.00")); // 300.00 x 1.50
        }

        @Test
        void the_senior_factor_does_not_apply_the_day_before_the_70th_birthday() {
            // Born the day after 70 years before the policy start: 69 today, 70 tomorrow.
            Policyholder holder = aPolicyholder().bornOn(POLICY_START.minusYears(70).plusDays(1)).build();

            assertThat(liabilityQuote(holder)).isEqualTo(new BigDecimal("300.00"));
        }

        @Test
        void young_driver_factor_applies_until_the_25th_birthday_not_until_new_year() {
            // Born in September: 24 on 1 March 2026, 25 only from September 2026.
            BigDecimal premium = liabilityQuote(aPolicyholder().bornOn(LocalDate.of(2001, 9, 15)).build());

            assertThat(premium).isEqualTo(new BigDecimal("450.00")); // 300.00 x 1.50
        }

        @Test
        void the_age_is_measured_on_the_policy_start_date() {
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
                    .aged(24)
                    .licensedForYears(2)
                    .withClaims(1)
                    .build();

            BigDecimal premium = quote(holder, aVehicle().withHorsepower(201).build(), Coverage.LIABILITY);

            assertThat(premium).isEqualTo(new BigDecimal("982.80")); // 300.00 x 3.276
        }

        @Test
        void the_premium_is_rounded_to_cents_half_up() {
            // 420.00 x 1.20 (age 75) x 1.30 (two years licensed) x 1.20 (one claim) x 1.20 (160 hp)
            // = 943.488, which rounds to 943.49. Rounding down would give 943.48.
            Policyholder holder = aPolicyholder().aged(75).licensedForYears(2).withClaims(1).build();

            BigDecimal premium = quote(holder, aVehicle().withHorsepower(160).build(), Coverage.PARTIAL);

            assertThat(premium).isEqualTo(new BigDecimal("943.49"));
        }
    }
}
