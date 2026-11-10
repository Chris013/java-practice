package io.github.chris013.practice.task16;

import static io.github.chris013.practice.task16.Lines.BUTTER;
import static io.github.chris013.practice.task16.Lines.PASTA;
import static io.github.chris013.practice.task16.Lines.ROLLS;
import static io.github.chris013.practice.task16.Lines.SOURDOUGH;
import static io.github.chris013.practice.task16.Lines.groceryRun;
import static io.github.chris013.practice.task16.Lines.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PromotionsTest {

    @Nested
    class PercentOffCategoryRules {

        @Test
        void takes_the_percentage_of_every_unclaimed_line_in_the_category() {
            Optional<Discount> discount = new PercentOffCategory("10% off bakery", Category.BAKERY, 10)
                    .evaluate(Basket.of(groceryRun()));

            assertThat(discount).hasValueSatisfying(d -> {
                assertThat(d.promotionName()).isEqualTo("10% off bakery");
                assertThat(d.amount()).isEqualTo("0.33");
                assertThat(d.claimedSkus()).containsExactlyInAnyOrder(SOURDOUGH, ROLLS);
            });
        }

        // Rounded once, half up: 0.05 x 10 / 100 = 0.005 becomes 0.01, 2.45 x 50 / 100 = 1.225 becomes 1.23.
        @ParameterizedTest
        @CsvSource({"1.00, 33, 0.33", "0.05, 10, 0.01", "2.45, 50, 1.23"})
        void rounds_the_discount_half_up_to_cents(String unitPrice, int percent, String expected) {
            Basket basket = Basket.of(List.of(line(SOURDOUGH, Category.BAKERY, unitPrice, 1)));

            Optional<Discount> discount = new PercentOffCategory("Off", Category.BAKERY, percent).evaluate(basket);

            assertThat(discount).hasValueSatisfying(d -> assertThat(d.amount()).isEqualTo(expected));
        }

        @Test
        void ignores_lines_from_other_categories() {
            Optional<Discount> discount = new PercentOffCategory("20% off dairy", Category.DAIRY, 20)
                    .evaluate(Basket.of(List.of(line(PASTA, Category.PASTA, "1.49", 3))));

            assertThat(discount).isEmpty();
        }

        @Test
        void gives_nothing_when_the_discount_rounds_to_zero() {
            // 0.04 / 100 * 10 = 0.004, which is 0.00 to the cent.
            Basket basket = Basket.of(List.of(line(SOURDOUGH, Category.BAKERY, "0.04", 1)));

            assertThat(new PercentOffCategory("Off", Category.BAKERY, 10).evaluate(basket)).isEmpty();
        }

        @Test
        void skips_lines_whose_sku_is_already_claimed() {
            Basket basket = Basket.of(groceryRun())
                    .after(new Discount("Earlier", new BigDecimal("2.40"), Set.of(SOURDOUGH)));

            Optional<Discount> discount = new PercentOffCategory("10% off bakery", Category.BAKERY, 10)
                    .evaluate(basket);

            // Only the rolls are left: 0.89 / 100 * 10 = 0.089, which is 0.09.
            assertThat(discount).hasValueSatisfying(d -> {
                assertThat(d.amount()).isEqualTo("0.09");
                assertThat(d.claimedSkus()).containsExactly(ROLLS);
            });
        }

        @Test
        void rounds_once_on_the_category_total_not_per_line() {
            // 0.10 x 10% = 0.01 once; per line it would be 0.005 -> 0.01 twice = 0.02.
            Basket basket = Basket.of(List.of(
                    line(SOURDOUGH, Category.BAKERY, "0.05", 1),
                    line(ROLLS, Category.BAKERY, "0.05", 1)));

            assertThat(new PercentOffCategory("Off", Category.BAKERY, 10).evaluate(basket))
                    .hasValueSatisfying(d -> assertThat(d.amount()).isEqualTo("0.01"));
        }

        @Test
        void rounds_once_on_the_line_total_not_per_unit() {
            // 0.15 x 10% = 0.015 -> 0.02; per unit it would be 0.005 -> 0.01 three times = 0.03.
            Basket basket = Basket.of(List.of(line(SOURDOUGH, Category.BAKERY, "0.05", 3)));

            assertThat(new PercentOffCategory("Off", Category.BAKERY, 10).evaluate(basket))
                    .hasValueSatisfying(d -> assertThat(d.amount()).isEqualTo("0.02"));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 100})
        void accepts_the_percentage_limits(int percent) {
            assertThatCode(() -> new PercentOffCategory("Off", Category.BAKERY, percent))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 101})
        void rejects_a_percentage_outside_one_to_one_hundred(int percent) {
            assertThatThrownBy(() -> new PercentOffCategory("Off", Category.BAKERY, percent))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class BuyXPayYRules {

        private final BuyXPayY threeForTwo = new BuyXPayY("3 for 2 on pasta", PASTA, 3, 2);

        @Test
        void gives_one_free_unit_for_a_full_group_and_claims_the_sku() {
            assertThat(threeForTwo.evaluate(Basket.of(groceryRun()))).hasValueSatisfying(d -> {
                assertThat(d.promotionName()).isEqualTo("3 for 2 on pasta");
                assertThat(d.amount()).isEqualTo("1.49");
                assertThat(d.claimedSkus()).containsExactly(PASTA);
            });
        }

        @ParameterizedTest
        @CsvSource({"3, 1.49", "5, 1.49", "6, 2.98", "7, 2.98"})
        void gives_one_free_unit_per_full_group(int quantity, String expected) {
            Basket basket = Basket.of(List.of(line(PASTA, Category.PASTA, "1.49", quantity)));

            assertThat(threeForTwo.evaluate(basket)).hasValueSatisfying(d -> assertThat(d.amount()).isEqualTo(expected));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2})
        void gives_nothing_until_a_full_group_is_in_the_basket(int quantity) {
            Basket basket = Basket.of(List.of(line(PASTA, Category.PASTA, "1.49", quantity)));

            assertThat(threeForTwo.evaluate(basket)).isEmpty();
        }

        @Test
        void ignores_other_skus() {
            assertThat(threeForTwo.evaluate(Basket.of(List.of(line(BUTTER, Category.DAIRY, "3.20", 6)))))
                    .isEmpty();
        }

        @Test
        void gives_nothing_when_the_sku_is_already_claimed() {
            Basket basket = Basket.of(groceryRun())
                    .after(new Discount("Earlier", new BigDecimal("0.45"), Set.of(PASTA)));

            assertThat(threeForTwo.evaluate(basket)).isEmpty();
        }

        @Test
        void gives_nothing_when_the_unit_price_is_zero() {
            Basket basket = Basket.of(List.of(line(PASTA, Category.PASTA, "0.00", 3)));
            assertThat(threeForTwo.evaluate(basket)).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({"2, 2", "3, 0", "2, 3"})
        void rejects_a_deal_where_buy_is_not_more_than_pay_or_pay_is_below_one(int buy, int pay) {
            assertThatThrownBy(() -> new BuyXPayY("Bad deal", PASTA, buy, pay))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class SpendAndSaveRules {

        @Test
        void takes_the_amount_off_when_the_total_reaches_the_threshold() {
            // The threshold equals the total exactly: that counts as reaching it.
            assertThat(new SpendAndSave("5 off from 20.64", new BigDecimal("20.64"), new BigDecimal("5.00"))
                    .evaluate(Basket.of(groceryRun())))
                    .hasValueSatisfying(d -> {
                        assertThat(d.amount()).isEqualTo("5.00");
                        assertThat(d.claimedSkus()).isEmpty();
                    });
        }

        @Test
        void does_nothing_just_below_the_threshold() {
            assertThat(new SpendAndSave("5 off from 20.65", new BigDecimal("20.65"), new BigDecimal("5.00"))
                    .evaluate(Basket.of(groceryRun()))).isEmpty();
        }

        @Test
        void never_takes_more_than_the_total() {
            assertThat(new SpendAndSave("50 off", BigDecimal.ZERO, new BigDecimal("50.00"))
                    .evaluate(Basket.of(groceryRun())))
                    .hasValueSatisfying(d -> assertThat(d.amount()).isEqualTo("20.64"));
        }

        @Test
        void sees_the_total_after_earlier_discounts() {
            Basket afterDiscounts = Basket.of(groceryRun())
                    .after(new Discount("Earlier", new BigDecimal("3.81"), Set.of()));
            SpendAndSave fiveOffFrom20 = new SpendAndSave("5 off from 20", new BigDecimal("20.00"), new BigDecimal("5.00"));

            // 20.64 - 3.81 = 16.83, which is below 20.00.
            assertThat(fiveOffFrom20.evaluate(afterDiscounts)).isEmpty();
            assertThat(new SpendAndSave("5 off from 16.83", new BigDecimal("16.83"), new BigDecimal("5.00"))
                    .evaluate(afterDiscounts)).hasValueSatisfying(d -> assertThat(d.amount()).isEqualTo("5.00"));
        }

        @Test
        void gives_nothing_when_the_discount_would_be_zero() {
            // The threshold is reached by an empty basket (0.00 >= 0.00), but min(5.00, 0.00) is 0.00: not a discount.
            assertThat(new SpendAndSave("Free", BigDecimal.ZERO, new BigDecimal("5.00")).evaluate(Basket.of(List.of())))
                    .isEmpty();
        }

        @ParameterizedTest
        @CsvSource({"20.00, 0.00", "-1.00, 5.00", "20.00, 5.001", "20.001, 5.00"})
        void rejects_a_negative_threshold_a_non_positive_amount_or_more_than_two_decimals(String threshold, String amountOff) {
            assertThatThrownBy(() -> new SpendAndSave("Bad", new BigDecimal(threshold), new BigDecimal(amountOff)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
