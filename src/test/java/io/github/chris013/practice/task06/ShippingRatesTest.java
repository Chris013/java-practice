package io.github.chris013.practice.task06;

import static io.github.chris013.practice.task06.Zone.DOMESTIC;
import static io.github.chris013.practice.task06.Zone.EU;
import static io.github.chris013.practice.task06.Zone.WORLD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShippingRatesTest {

    private static final Dimensions SHOE_BOX = new Dimensions(35, 20, 12);

    private final ShippingRates rates = new ShippingRates();

    @Nested
    class Letters {

        @ParameterizedTest(name = "{0} g cost {1}")
        @CsvSource({"1, 0.95", "20, 0.95", "21, 1.60", "500, 1.60", "501, 2.75", "1000, 2.75"})
        void are_priced_by_weight_tier(int grams, String expectedPrice) {
            assertThat(rates.priceFor(new Letter(grams), DOMESTIC)).isEqualByComparingTo(expectedPrice);
        }

        @Test
        void cost_more_abroad_rounded_half_up_to_cents() {
            assertThat(rates.priceFor(new Letter(20), EU)).isEqualByComparingTo("1.43"); // 0.95 x 1.5 = 1.425
            assertThat(rates.priceFor(new Letter(20), WORLD)).isEqualByComparingTo("2.38"); // 0.95 x 2.5 = 2.375
        }

        @Test
        void must_have_a_plausible_weight() {
            assertThatThrownBy(() -> new Letter(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Letter(1001))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1001");
        }
    }

    @Nested
    class Parcels {

        @Test
        void are_priced_by_actual_weight_when_they_are_compact() {
            assertThat(rates.priceFor(new Parcel(1500, SHOE_BOX), DOMESTIC)).isEqualByComparingTo("4.99");
        }

        @ParameterizedTest(name = "{0} g cost {1}")
        @CsvSource({"2000, 4.99", "2001, 6.99", "5000, 6.99", "5001, 9.99", "10000, 9.99", "10001, 16.99", "31500, 16.99"})
        void are_priced_by_weight_tier(int grams, String expectedPrice) {
            assertThat(rates.priceFor(new Parcel(grams, SHOE_BOX), DOMESTIC)).isEqualByComparingTo(expectedPrice);
        }

        @Test
        void are_priced_by_volumetric_weight_when_they_are_light_but_bulky() {
            // 60 x 40 x 40 cm = 96,000 cm³ -> 19.2 kg volumetric weight
            Parcel pillows = new Parcel(1000, new Dimensions(60, 40, 40));

            assertThat(pillows.billableWeightGrams()).isEqualTo(19_200);
            assertThat(rates.priceFor(pillows, DOMESTIC)).isEqualByComparingTo("16.99");
        }

        @Test
        void round_volumetric_weight_up_to_the_next_gram() {
            assertThat(new Parcel(1, new Dimensions(1, 1, 7)).billableWeightGrams()).isEqualTo(2); // 7 cm³ / 5 = 1.4 g
        }

        @Test
        void cost_more_abroad() {
            assertThat(rates.priceFor(new Parcel(1500, SHOE_BOX), EU)).isEqualByComparingTo("7.49"); // 4.99 x 1.5
            assertThat(rates.priceFor(new Parcel(1500, SHOE_BOX), WORLD)).isEqualByComparingTo("12.48"); // 4.99 x 2.5
        }

        @Test
        void cannot_be_shipped_above_31_5_kg_billable_weight() {
            assertThatThrownBy(() -> rates.priceFor(new Parcel(31_501, SHOE_BOX), DOMESTIC))
                    .isInstanceOf(NotShippableException.class);
            assertThatThrownBy(() -> rates.priceFor(new Parcel(5000, new Dimensions(80, 60, 40)), DOMESTIC))
                    .isInstanceOf(NotShippableException.class);
        }

        @Test
        void must_have_a_positive_weight_and_dimensions() {
            assertThatThrownBy(() -> new Parcel(0, SHOE_BOX)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Parcel(100, null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Dimensions(10, 0, 10)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Pallets {

        @Test
        void cost_a_flat_rate_up_to_300_kg() {
            assertThat(rates.priceFor(new Pallet(250, true), DOMESTIC)).isEqualByComparingTo("89.00");
            assertThat(rates.priceFor(new Pallet(300, true), DOMESTIC)).isEqualByComparingTo("89.00");
        }

        @ParameterizedTest(name = "{0} kg cost {1}")
        @CsvSource({"301, 101.00", "400, 101.00", "401, 113.00", "1000, 173.00"})
        void cost_extra_per_started_100_kg_above_300_kg(int kilograms, String expectedPrice) {
            assertThat(rates.priceFor(new Pallet(kilograms, true), DOMESTIC)).isEqualByComparingTo(expectedPrice);
        }

        @Test
        void cost_25_percent_more_when_not_stackable() {
            assertThat(rates.priceFor(new Pallet(250, false), DOMESTIC)).isEqualByComparingTo("111.25");
        }

        @Test
        void can_be_shipped_within_the_eu() {
            assertThat(rates.priceFor(new Pallet(250, true), EU)).isEqualByComparingTo("133.50");
        }

        @Test
        void cannot_be_shipped_outside_the_eu() {
            assertThatThrownBy(() -> rates.priceFor(new Pallet(250, true), WORLD))
                    .isInstanceOf(NotShippableException.class);
        }

        @Test
        void must_weigh_between_1_and_1000_kg() {
            assertThatThrownBy(() -> new Pallet(0, true)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Pallet(1001, true)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class TransitDays {

        @ParameterizedTest(name = "{0}")
        @CsvSource({"DOMESTIC, 1", "EU, 3", "WORLD, 7"})
        void for_letters(Zone zone, int days) {
            assertThat(rates.transitDays(new Letter(20), zone)).isEqualTo(days);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"DOMESTIC, 2", "EU, 4", "WORLD, 8"})
        void for_parcels(Zone zone, int days) {
            assertThat(rates.transitDays(new Parcel(1000, SHOE_BOX), zone)).isEqualTo(days);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"DOMESTIC, 3", "EU, 5"})
        void for_pallets(Zone zone, int days) {
            assertThat(rates.transitDays(new Pallet(250, true), zone)).isEqualTo(days);
        }

        @Test
        void are_not_promised_for_shipments_we_cannot_ship() {
            assertThatThrownBy(() -> rates.transitDays(new Pallet(250, true), WORLD))
                    .isInstanceOf(NotShippableException.class);
        }
    }

    @Test
    void prices_are_always_in_cents() {
        BigDecimal price = rates.priceFor(new Letter(20), EU);

        assertThat(price.scale()).isEqualTo(2);
    }
}
