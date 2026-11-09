package io.github.chris013.practice.task16;

import static io.github.chris013.practice.task16.Lines.BUTTER;
import static io.github.chris013.practice.task16.Lines.JUICE;
import static io.github.chris013.practice.task16.Lines.PASTA;
import static io.github.chris013.practice.task16.Lines.ROLLS;
import static io.github.chris013.practice.task16.Lines.SOURDOUGH;
import static io.github.chris013.practice.task16.Lines.groceryRun;
import static io.github.chris013.practice.task16.Lines.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BasketTest {

    @Nested
    class Totals {

        @Test
        void subtotal_adds_up_every_line() {
            assertThat(Basket.of(groceryRun()).subtotal()).isEqualTo("20.64");
        }

        @Test
        void total_equals_the_subtotal_when_nothing_is_discounted() {
            assertThat(Basket.of(groceryRun()).total()).isEqualTo("20.64");
        }

        @Test
        void an_empty_basket_has_a_subtotal_with_two_decimals() {
            assertThat(Basket.of(List.of()).subtotal()).isEqualTo("0.00");
            assertThat(Basket.of(List.of()).total()).isEqualTo("0.00");
        }
    }

    @Nested
    class Claims {

        @Test
        void unclaimed_lines_are_all_lines_at_the_start() {
            assertThat(Basket.of(groceryRun()).unclaimedLines()).hasSize(6);
        }

        @Test
        void after_adds_the_discount_to_the_total_and_claims_its_skus() {
            Discount discount = new Discount("Earlier", new BigDecimal("1.49"), Set.of(PASTA));

            Basket basket = Basket.of(groceryRun()).after(discount);

            assertThat(basket.total()).isEqualTo("19.15");
            assertThat(basket.unclaimedLines()).extracting(BasketLine::sku)
                    .doesNotContain(PASTA)
                    .hasSize(5);
        }

        @Test
        void after_does_not_change_the_original_basket() {
            Basket original = Basket.of(groceryRun());

            original.after(new Discount("Earlier", new BigDecimal("1.49"), Set.of(PASTA)));

            assertThat(original.total()).isEqualTo("20.64");
            assertThat(original.unclaimedLines()).hasSize(6);
        }

        @Test
        void later_changes_to_the_input_list_do_not_reach_the_basket() {
            List<BasketLine> input = new ArrayList<>(groceryRun());
            Basket basket = Basket.of(input);

            input.clear();

            assertThat(basket.lines()).hasSize(6);
            assertThat(basket.subtotal()).isEqualTo("20.64");
        }

        @Test
        void rejects_a_missing_list_of_lines() {
            assertThatThrownBy(() -> Basket.of(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void after_rejects_a_missing_discount() {
            assertThatThrownBy(() -> Basket.of(groceryRun()).after(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class BasketLines {

        @Test
        void normalizes_the_unit_price_to_two_decimals() {
            BasketLine bread = new BasketLine(SOURDOUGH, "Sourdough", Category.BAKERY, new BigDecimal("2.4"), 1);

            assertThat(bread.unitPrice()).isEqualTo("2.40");
        }

        @Test
        void multiplies_unit_price_by_quantity() {
            assertThat(line(PASTA, Category.PASTA, "1.49", 3).total()).isEqualTo("4.47");
        }

        @ParameterizedTest
        @CsvSource({"1.499, 1", "-0.01, 1", "2.40, 0"})
        void rejects_a_price_with_three_decimals_a_negative_price_or_a_quantity_below_one(String price, int quantity) {
            assertThatThrownBy(() -> line(ROLLS, Category.BAKERY, price, quantity))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Discounts {

        @Test
        void normalizes_the_amount_to_two_decimals() {
            assertThat(new Discount("Five off", new BigDecimal("5"), Set.of()).amount()).isEqualTo("5.00");
        }

        @ParameterizedTest
        @CsvSource({"0.00", "-1.00"})
        void rejects_an_amount_that_is_not_positive(String amount) {
            assertThatThrownBy(() -> new Discount("Nothing", new BigDecimal(amount), Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_an_amount_with_more_than_two_decimals() {
            assertThatThrownBy(() -> new Discount("Too precise", new BigDecimal("0.333"), Set.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void lists_the_claimed_skus_it_was_created_with() {
            Discount discount = new Discount("Juice", new BigDecimal("1.99"), Set.of(JUICE));

            assertThat(discount.claimedSkus()).containsExactly(JUICE);
            assertThat(discount.claimedSkus()).doesNotContain(BUTTER);
        }
    }
}
