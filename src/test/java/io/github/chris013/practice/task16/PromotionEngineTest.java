package io.github.chris013.practice.task16;

import static io.github.chris013.practice.task16.Lines.PASTA;
import static io.github.chris013.practice.task16.Lines.groceryRun;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PromotionEngineTest {

    private final Promotion pastaThreeForTwo = new BuyXPayY("3 for 2 on pasta", PASTA, 3, 2);
    private final Promotion bakeryTenPercent = new PercentOffCategory("10% off bakery", Category.BAKERY, 10);
    private final Promotion drinksDeal = new BestOf("Drinks deal", List.of(
            new PercentOffCategory("20% off drinks", Category.DRINKS, 20),
            new BuyXPayY("Juice 2 for 1", Lines.JUICE, 2, 1)));
    private final Promotion fiveOffFromSixteen =
            new SpendAndSave("5 off from 16", new BigDecimal("16.00"), new BigDecimal("5.00"));
    private final Promotion fiveOffFromTwenty =
            new SpendAndSave("5 off from 20", new BigDecimal("20.00"), new BigDecimal("5.00"));

    /** A promotion that always takes the same amount off, with no claims. Written as a class, not a lambda. */
    private static Promotion fixedDiscount(String name, String amount) {
        return new Promotion() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public Optional<Discount> evaluate(Basket basket) {
                return Optional.of(new Discount(name, new BigDecimal(amount), Set.of()));
            }
        };
    }

    @Nested
    class WorkedExample {

        // Subtotal 20.64. Line-level promotions first, then the basket-level one sees 16.83 and still applies.
        private final PromotionEngine engine = new PromotionEngine(
                List.of(pastaThreeForTwo, bakeryTenPercent, drinksDeal, fiveOffFromSixteen));

        @Test
        void gives_the_exact_subtotal_discounts_and_total() {
            Receipt receipt = engine.price(groceryRun());

            assertThat(receipt.subtotal()).isEqualTo("20.64");
            assertThat(receipt.discounts()).extracting(Discount::promotionName)
                    .containsExactly("3 for 2 on pasta", "10% off bakery", "Juice 2 for 1", "5 off from 16");
            assertThat(receipt.discounts()).extracting(Discount::amount)
                    .containsExactly(new BigDecimal("1.49"), new BigDecimal("0.33"), new BigDecimal("1.99"),
                            new BigDecimal("5.00"));
            // 20.64 - (1.49 + 0.33 + 1.99 + 5.00) = 20.64 - 8.81
            assertThat(receipt.total()).isEqualTo("11.83");
        }
    }

    @Nested
    class Claims {

        @Test
        void a_claimed_sku_is_not_discounted_twice() {
            PromotionEngine engine = new PromotionEngine(List.of(pastaThreeForTwo,
                    new PercentOffCategory("50% off pasta", Category.PASTA, 50)));

            Receipt receipt = engine.price(groceryRun());

            assertThat(receipt.discounts()).extracting(Discount::promotionName).containsExactly("3 for 2 on pasta");
            assertThat(receipt.total()).isEqualTo("19.15");
        }

        @Test
        void an_earlier_line_promotion_hides_the_sku_from_a_later_one() {
            PromotionEngine engine = new PromotionEngine(List.of(
                    new PercentOffCategory("10% off pasta", Category.PASTA, 10), pastaThreeForTwo));

            Receipt receipt = engine.price(groceryRun());

            // 10% of 4.47 is 0.447, which rounds to 0.45; the 3-for-2 finds its SKU already claimed.
            assertThat(receipt.discounts()).extracting(Discount::promotionName).containsExactly("10% off pasta");
            assertThat(receipt.total()).isEqualTo("20.19");
        }
    }

    @Nested
    class Order {

        @Test
        void a_basket_level_promotion_placed_first_sees_the_full_subtotal() {
            PromotionEngine engine = new PromotionEngine(List.of(fiveOffFromTwenty,
                    pastaThreeForTwo, bakeryTenPercent, drinksDeal));

            Receipt receipt = engine.price(groceryRun());

            // 20.64 is at least 20.00, so the 5.00 applies first. The line promotions still apply afterwards.
            assertThat(receipt.discounts()).extracting(Discount::promotionName).startsWith("5 off from 20");
            assertThat(receipt.total()).isEqualTo("11.83");
        }

        @Test
        void a_basket_level_promotion_placed_last_sees_only_the_discounted_total() {
            PromotionEngine engine = new PromotionEngine(List.of(
                    pastaThreeForTwo, bakeryTenPercent, drinksDeal, fiveOffFromTwenty));

            Receipt receipt = engine.price(groceryRun());

            // After 3.81 of line discounts the basket is 16.83, below 20.00, so the 5.00 does not apply.
            assertThat(receipt.discounts()).hasSize(3);
            assertThat(receipt.total()).isEqualTo("16.83");
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void an_empty_basket_has_zero_subtotal_and_no_discounts() {
            PromotionEngine engine = new PromotionEngine(List.of(pastaThreeForTwo, bakeryTenPercent,
                    drinksDeal, fiveOffFromSixteen));

            Receipt receipt = engine.price(List.of());

            assertThat(receipt.subtotal()).isEqualTo("0.00");
            assertThat(receipt.discounts()).isEmpty();
            assertThat(receipt.total()).isEqualTo("0.00");
        }

        @Test
        void no_promotions_means_the_total_is_the_subtotal() {
            Receipt receipt = new PromotionEngine(List.of()).price(groceryRun());

            assertThat(receipt.discounts()).isEmpty();
            assertThat(receipt.total()).isEqualTo("20.64");
        }

        @Test
        void the_total_never_goes_below_zero() {
            PromotionEngine engine = new PromotionEngine(List.of(fixedDiscount("Too generous", "100.00")));

            Receipt receipt = engine.price(groceryRun());

            assertThat(receipt.subtotal()).isEqualTo("20.64");
            assertThat(receipt.discounts()).hasSize(1);
            assertThat(receipt.total()).isEqualTo("0.00");
        }

        @Test
        void rejects_a_missing_list_of_promotions() {
            assertThatThrownBy(() -> new PromotionEngine(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejects_a_missing_list_of_lines() {
            assertThatThrownBy(() -> new PromotionEngine(List.of()).price(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
