package io.github.chris013.practice.task13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LineItemTest {

    private static LineItem item(String price, int quantity) {
        return new LineItem("Milk", Category.DAIRY, new BigDecimal(price), quantity);
    }

    @Nested
    class Price {

        @Test
        void normalizes_the_price_to_two_decimal_places() {
            assertThat(item("1.1", 1).unitPriceGross()).isEqualTo(new BigDecimal("1.10"));
            assertThat(item("3", 1).unitPriceGross()).isEqualTo(new BigDecimal("3.00"));
        }

        @Test
        void accepts_a_price_of_zero() {
            assertThat(item("0.00", 1).unitPriceGross()).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        void rejects_a_negative_price() {
            assertThatThrownBy(() -> item("-0.01", 1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_more_than_two_decimal_places_instead_of_rounding() {
            assertThatThrownBy(() -> item("1.005", 1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_missing_price() {
            assertThatThrownBy(() -> new LineItem("Milk", Category.DAIRY, null, 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("unitPriceGross");
        }
    }

    @Nested
    class Quantity {

        @Test
        void accepts_a_quantity_of_one() {
            assertThat(item("1.09", 1).quantity()).isEqualTo(1);
        }

        @Test
        void rejects_a_quantity_of_zero() {
            assertThatThrownBy(() -> item("1.09", 0)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_negative_quantity() {
            assertThatThrownBy(() -> item("1.09", -2)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Name {

        @Test
        void rejects_a_blank_name() {
            assertThatThrownBy(() -> new LineItem("  ", Category.DAIRY, new BigDecimal("1.09"), 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_missing_name() {
            assertThatThrownBy(() -> new LineItem(null, Category.DAIRY, new BigDecimal("1.09"), 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("name");
        }

        @Test
        void rejects_a_missing_category() {
            assertThatThrownBy(() -> new LineItem("Milk", null, new BigDecimal("1.09"), 1))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("category");
        }
    }

    @Nested
    class Total {

        @Test
        void is_the_unit_price_times_the_quantity_with_scale_two() {
            // 1.09 x 2 = 2.18
            assertThat(item("1.09", 2).totalGross()).isEqualTo(new BigDecimal("2.18"));
        }
    }
}
