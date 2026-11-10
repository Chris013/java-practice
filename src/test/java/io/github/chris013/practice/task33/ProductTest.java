package io.github.chris013.practice.task33;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the given record. They pass on the starter too: they pin down the validation that the rest of the
 * task relies on.
 */
class ProductTest {

    @Nested
    class Validation {

        @Test
        void accepts_a_free_product_with_no_stock() {
            assertThatCode(() -> SampleProducts.product("X-1", "Free sample", Category.FOOD, "0", 0, 1))
                    .doesNotThrowAnyException();
        }

        @Test
        void rejects_a_null_sku_name_category_or_price() {
            BigDecimal price = BigDecimal.ONE;

            assertThatThrownBy(() -> new Product(null, "n", Category.FOOD, price, 1, 1))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Product("s", null, Category.FOOD, price, 1, 1))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Product("s", "n", null, price, 1, 1))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Product("s", "n", Category.FOOD, null, 1, 1))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest
        @CsvSource({
            "' ', n",
            "'', n",
            "s, ' '"
        })
        void rejects_a_blank_sku_or_name(String sku, String name) {
            assertThatThrownBy(() -> new Product(sku, name, Category.FOOD, BigDecimal.ONE, 1, 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_negative_price() {
            assertThatThrownBy(() -> SampleProducts.product("s", "n", Category.FOOD, "-0.01", 1, 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_negative_stock() {
            assertThatThrownBy(() -> new Product("s", "n", Category.FOOD, BigDecimal.ONE, -1, 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource({"0", "-5"})
        void rejects_a_weight_that_is_not_positive(int weightGrams) {
            assertThatThrownBy(() -> new Product("s", "n", Category.FOOD, BigDecimal.ONE, 1, weightGrams))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
