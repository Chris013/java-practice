package io.github.chris013.practice.task33;

import static io.github.chris013.practice.task33.ProductFilters.allOf;
import static io.github.chris013.practice.task33.ProductFilters.anyOf;
import static io.github.chris013.practice.task33.ProductFilters.heavierThan;
import static io.github.chris013.practice.task33.ProductFilters.inCategory;
import static io.github.chris013.practice.task33.ProductFilters.inStock;
import static io.github.chris013.practice.task33.ProductFilters.nameContains;
import static io.github.chris013.practice.task33.ProductFilters.priceBetween;
import static io.github.chris013.practice.task33.SampleProducts.CLAW_HAMMER;
import static io.github.chris013.practice.task33.SampleProducts.CORDLESS_DRILL;
import static io.github.chris013.practice.task33.SampleProducts.GARDENING_BOOK;
import static io.github.chris013.practice.task33.SampleProducts.HAMMER_HANDBOOK;
import static io.github.chris013.practice.task33.SampleProducts.OLIVE_OIL;
import static io.github.chris013.practice.task33.SampleProducts.PLUSH_BEAR;
import static io.github.chris013.practice.task33.SampleProducts.ROLLED_OATS;
import static io.github.chris013.practice.task33.SampleProducts.WOODEN_TRAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProductFiltersTest {

    private static List<Product> matching(Predicate<Product> predicate) {
        return SampleProducts.all().stream().filter(predicate).toList();
    }

    @Nested
    class InCategory {

        @Test
        void matches_only_products_of_that_category() {
            assertThat(matching(inCategory(Category.TOOLS))).containsExactly(CLAW_HAMMER, CORDLESS_DRILL);
        }

        @Test
        void rejects_a_null_category() {
            assertThatThrownBy(() -> inCategory(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class PriceBetween {

        @Test
        void includes_both_bounds_when_prices_are_exactly_on_them() {
            assertThat(matching(priceBetween(new BigDecimal("5.00"), new BigDecimal("15.00"))))
                    .containsExactly(CLAW_HAMMER, OLIVE_OIL, PLUSH_BEAR, WOODEN_TRAIN, GARDENING_BOOK);
        }

        // The min and max are compared by value, so the scale of the BigDecimal does not matter.
        @ParameterizedTest
        @CsvSource({
            "15, 15, 2",
            "15.000, 15.0, 2",
            "15.01, 19.99, 1",
            "5.00, 5.00, 1",
            "5.01, 7.89, 0"
        })
        void counts_matches_for_each_boundary_case(String min, String max, int expectedCount) {
            assertThat(matching(priceBetween(new BigDecimal(min), new BigDecimal(max)))).hasSize(expectedCount);
        }

        @Test
        void rejects_a_minimum_above_the_maximum() {
            assertThatThrownBy(() -> priceBetween(new BigDecimal("16"), new BigDecimal("15")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_null_bounds() {
            assertThatThrownBy(() -> priceBetween(null, BigDecimal.TEN)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> priceBetween(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class InStock {

        @Test
        void excludes_products_with_zero_stock() {
            assertThat(matching(inStock())).containsExactly(
                    CLAW_HAMMER, ROLLED_OATS, OLIVE_OIL, PLUSH_BEAR, HAMMER_HANDBOOK, GARDENING_BOOK);
        }
    }

    @Nested
    class NameContains {

        @ParameterizedTest
        @CsvSource({
            "hammer, 2",
            "HAMMER, 2",
            "HaMmEr, 2",
            "oil, 1",
            "xyz, 0",
            "'', 8"
        })
        void matches_case_insensitively_anywhere_in_the_name(String text, int expectedCount) {
            assertThat(matching(nameContains(text))).hasSize(expectedCount);
        }

        @Test
        void returns_the_matching_products_in_catalogue_order() {
            assertThat(matching(nameContains("hammer"))).containsExactly(CLAW_HAMMER, HAMMER_HANDBOOK);
        }

        @Test
        void rejects_null_text() {
            assertThatThrownBy(() -> nameContains(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class HeavierThan {

        @Test
        void is_strict_so_a_product_of_exactly_that_weight_is_excluded() {
            assertThat(matching(heavierThan(1000))).containsExactly(CORDLESS_DRILL, WOODEN_TRAIN);
        }

        @ParameterizedTest
        @CsvSource({
            "999, 3",
            "1000, 2",
            "1199, 2",
            "1200, 1",
            "1800, 0"
        })
        void counts_matches_around_each_weight(int grams, int expectedCount) {
            assertThat(matching(heavierThan(grams))).hasSize(expectedCount);
        }
    }

    @Nested
    class AllOf {

        @Test
        void an_empty_list_matches_everything() {
            assertThat(matching(allOf(List.of()))).hasSize(8);
        }

        @Test
        void matches_only_when_every_predicate_matches() {
            assertThat(matching(allOf(List.of(inCategory(Category.TOOLS), inStock())))).containsExactly(CLAW_HAMMER);
        }

        @Test
        void copies_the_list_so_later_changes_do_not_affect_the_filter() {
            List<Predicate<Product>> conditions = new ArrayList<>(List.of(inStock()));
            Predicate<Product> filter = allOf(conditions);

            conditions.add(inCategory(Category.TOYS));

            assertThat(matching(filter)).hasSize(6);
        }

        @Test
        void rejects_a_null_list_or_a_null_element() {
            assertThatThrownBy(() -> allOf(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> allOf(Arrays.asList(inStock(), null)))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class AnyOf {

        @Test
        void an_empty_list_matches_nothing() {
            assertThat(matching(anyOf(List.of()))).isEmpty();
        }

        @Test
        void matches_when_at_least_one_predicate_matches() {
            Predicate<Product> books = inCategory(Category.BOOKS);
            Predicate<Product> cheap = priceBetween(BigDecimal.ZERO, BigDecimal.valueOf(3));

            assertThat(matching(anyOf(List.of(books, cheap))))
                    .containsExactly(ROLLED_OATS, HAMMER_HANDBOOK, GARDENING_BOOK);
        }

        @Test
        void rejects_a_null_list_or_a_null_element() {
            assertThatThrownBy(() -> anyOf(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> anyOf(Arrays.asList(inStock(), null)))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Composition {

        @Test
        void and_combines_two_filters() {
            assertThat(matching(inCategory(Category.TOYS).and(inStock()))).containsExactly(PLUSH_BEAR);
        }

        @Test
        void or_combines_two_filters() {
            assertThat(matching(inCategory(Category.BOOKS).or(heavierThan(1500))))
                    .containsExactly(CORDLESS_DRILL, HAMMER_HANDBOOK, GARDENING_BOOK);
        }

        @Test
        void negate_inverts_a_filter() {
            assertThat(matching(inStock().negate())).containsExactly(CORDLESS_DRILL, WOODEN_TRAIN);
        }

        @Test
        void predicate_not_is_the_same_as_negate() {
            assertThat(matching(Predicate.not(inStock()))).containsExactly(CORDLESS_DRILL, WOODEN_TRAIN);
        }
    }
}
