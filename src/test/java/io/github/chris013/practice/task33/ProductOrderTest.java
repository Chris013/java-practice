package io.github.chris013.practice.task33;

import static io.github.chris013.practice.task33.SampleProducts.product;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProductOrderTest {

    private static List<String> skusSortedBy(Comparator<Product> order) {
        return SampleProducts.all().stream().sorted(order).map(Product::sku).toList();
    }

    @Nested
    class ByPrice {

        @Test
        void sorts_the_cheapest_first() {
            assertThat(skusSortedBy(ProductOrder.byPrice()))
                    .containsExactly("F-010", "B-002", "F-020", "T-100", "Y-001", "Y-002", "B-001", "T-200");
        }

        @Test
        void compares_prices_by_value_not_by_scale() {
            Comparator<Product> order = ProductOrder.byPrice();

            assertThat(order.compare(product("A", "a", Category.FOOD, "5.0", 1, 1),
                    product("B", "b", Category.FOOD, "5.00", 1, 1))).isZero();
        }

        @Test
        void keeps_the_catalogue_order_for_equal_prices() {
            assertThat(skusSortedBy(ProductOrder.byPrice())).containsSubsequence("Y-001", "Y-002");
        }
    }

    @Nested
    class ByStockDescending {

        @Test
        void sorts_the_most_items_first() {
            assertThat(skusSortedBy(ProductOrder.byStockDescending()))
                    .containsExactly("F-010", "F-020", "Y-001", "B-002", "T-100", "B-001", "T-200", "Y-002");
        }

        @Test
        void treats_equal_stock_as_a_tie_and_keeps_the_catalogue_order() {
            Comparator<Product> order = ProductOrder.byStockDescending();

            assertThat(order.compare(SampleProducts.CORDLESS_DRILL, SampleProducts.WOODEN_TRAIN)).isZero();
            assertThat(skusSortedBy(order)).containsSubsequence("T-200", "Y-002");
        }
    }

    @Nested
    class ByCategoryThenName {

        @Test
        void groups_by_category_in_enum_order_then_sorts_by_name() {
            assertThat(skusSortedBy(ProductOrder.byCategoryThenName()))
                    .containsExactly("F-020", "F-010", "T-100", "T-200", "Y-001", "Y-002", "B-002", "B-001");
        }

        @Test
        void compares_names_ignoring_case() {
            Comparator<Product> order = ProductOrder.byCategoryThenName();
            Product banana = product("1", "Banana", Category.FOOD, "1", 1, 1);
            Product apple = product("2", "apple", Category.FOOD, "1", 1, 1);

            // Case-sensitive order would put "Banana" first, because 'B' comes before 'a'.
            assertThat(order.compare(apple, banana)).isNegative();
        }

        @Test
        void an_earlier_category_wins_even_when_its_name_is_later() {
            Comparator<Product> order = ProductOrder.byCategoryThenName();
            Product zebraFood = product("1", "Zebra", Category.FOOD, "1", 1, 1);
            Product aardvarkBook = product("2", "Aardvark", Category.BOOKS, "1", 1, 1);

            assertThat(order.compare(zebraFood, aardvarkBook)).isNegative();
        }
    }

    @Nested
    class CheapestThenLightest {

        @Test
        void sorts_by_price_and_then_by_weight() {
            assertThat(skusSortedBy(ProductOrder.cheapestThenLightest()))
                    .containsExactly("F-010", "B-002", "F-020", "T-100", "Y-001", "Y-002", "B-001", "T-200");
        }

        @Test
        void a_lighter_product_wins_a_price_tie() {
            Comparator<Product> order = ProductOrder.cheapestThenLightest();
            Product heavy = product("Y-9", "Heavy", Category.TOYS, "15.00", 1, 1200);
            Product light = product("Y-1", "Light", Category.TOYS, "15.00", 1, 300);

            assertThat(order.compare(light, heavy)).isNegative();
        }

        @Test
        void the_sku_decides_when_price_and_weight_are_equal() {
            Comparator<Product> order = ProductOrder.cheapestThenLightest();
            Product b2 = product("B-2", "Same", Category.BOOKS, "3.00", 1, 100);
            Product b1 = product("B-1", "Same", Category.BOOKS, "3.00", 1, 100);

            assertThat(order.compare(b1, b2)).isNegative();
            assertThat(order.compare(b2, b1)).isPositive();
        }
    }
}
