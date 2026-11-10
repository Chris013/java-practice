package io.github.chris013.practice.task33;

import static io.github.chris013.practice.task33.ProductFilters.heavierThan;
import static io.github.chris013.practice.task33.ProductFilters.inCategory;
import static io.github.chris013.practice.task33.ProductFilters.inStock;
import static io.github.chris013.practice.task33.ProductOrder.byPrice;
import static io.github.chris013.practice.task33.ProductOrder.byStockDescending;
import static io.github.chris013.practice.task33.SampleProducts.CLAW_HAMMER;
import static io.github.chris013.practice.task33.SampleProducts.CORDLESS_DRILL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CatalogueTest {

    private final Catalogue catalogue = new Catalogue(SampleProducts.all());

    @Nested
    class Find {

        @Test
        void returns_the_matches_sorted_by_the_given_order() {
            assertThat(catalogue.find(inStock(), byPrice()))
                    .extracting(Product::sku)
                    .containsExactly("F-010", "B-002", "F-020", "T-100", "Y-001", "B-001");
        }

        @Test
        void returns_an_empty_list_when_nothing_matches() {
            assertThat(catalogue.find(inCategory(Category.TOYS).and(heavierThan(5000)), byPrice())).isEmpty();
        }

        @Test
        void returns_an_immutable_list() {
            List<Product> result = catalogue.find(inStock(), byPrice());

            assertThatThrownBy(() -> result.add(CLAW_HAMMER)).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(result::clear).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void rejects_null_arguments() {
            assertThatThrownBy(() -> catalogue.find(null, byPrice())).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> catalogue.find(inStock(), null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void accepts_a_predicate_and_comparator_written_for_a_supertype() {
            Predicate<Object> isProduct = Product.class::isInstance;
            Comparator<Object> byText = Comparator.comparing(Object::toString);

            List<Product> result = catalogue.find(isProduct, byText);

            assertThat(result).hasSize(8);
            assertThat(result.get(0).sku()).isEqualTo("B-001");
        }
    }

    @Nested
    class FindFirst {

        @Test
        void returns_the_first_match_in_the_given_order() {
            assertThat(catalogue.findFirst(inCategory(Category.TOOLS), byPrice()))
                    .contains(CLAW_HAMMER);
        }

        @Test
        void returns_empty_when_nothing_matches() {
            assertThat(catalogue.findFirst(heavierThan(5000), byPrice())).isEqualTo(Optional.empty());
        }

        @Test
        void on_a_tie_returns_the_product_that_comes_first_in_the_catalogue() {
            // Both are out of stock, so the comparator sees a tie.
            Predicate<Product> outOfStock = inStock().negate();

            assertThat(catalogue.findFirst(outOfStock, byStockDescending())).contains(CORDLESS_DRILL);
        }

        @Test
        void rejects_null_arguments() {
            assertThatThrownBy(() -> catalogue.findFirst(null, byPrice())).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> catalogue.findFirst(inStock(), null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Project {

        @Test
        void maps_the_matches_in_catalogue_order_not_sorted_order() {
            assertThat(catalogue.project(inCategory(Category.BOOKS), Product::name))
                    .containsExactly("Hammer handbook", "Gardening for beginners");
        }

        @Test
        void can_map_to_any_type() {
            assertThat(catalogue.project(inCategory(Category.TOYS), Product::weightGrams))
                    .containsExactly(300, 1200);
        }

        @Test
        void returns_an_empty_immutable_list_when_nothing_matches() {
            List<String> names = catalogue.project(heavierThan(5000), Product::name);

            assertThat(names).isEmpty();
            assertThatThrownBy(() -> names.add("x")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void rejects_null_arguments() {
            assertThatThrownBy(() -> catalogue.project(null, Product::name))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> catalogue.project(inStock(), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class CountBy {

        @Test
        void counts_the_matches_per_category() {
            assertThat(catalogue.countBy(inStock()))
                    .isEqualTo(Map.of(Category.FOOD, 2L, Category.TOOLS, 1L, Category.TOYS, 1L, Category.BOOKS, 2L));
        }

        @Test
        void leaves_out_categories_without_a_match() {
            assertThat(catalogue.countBy(heavierThan(1000)))
                    .isEqualTo(Map.of(Category.TOOLS, 1L, Category.TOYS, 1L))
                    .doesNotContainKeys(Category.FOOD, Category.BOOKS);
        }

        @Test
        void returns_an_empty_map_when_nothing_matches() {
            assertThat(catalogue.countBy(heavierThan(5000))).isEmpty();
        }

        @Test
        void returns_an_unmodifiable_map() {
            Map<Category, Long> counts = catalogue.countBy(inStock());

            assertThatThrownBy(() -> counts.put(Category.TOYS, 9L)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void rejects_a_null_filter() {
            assertThatThrownBy(() -> catalogue.countBy(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Construction {

        @Test
        void copies_the_input_so_later_changes_do_not_leak_in() {
            List<Product> source = new ArrayList<>(SampleProducts.all());
            Catalogue copy = new Catalogue(source);

            source.clear();

            assertThat(copy.find(inStock().or(inStock().negate()), byPrice())).hasSize(8);
        }

        @Test
        void rejects_null_input_and_null_products() {
            assertThatThrownBy(() -> new Catalogue(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Catalogue(Arrays.asList(CLAW_HAMMER, null)))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
