package io.github.chris013.practice.task16;

import static io.github.chris013.practice.task16.Lines.BUTTER;
import static io.github.chris013.practice.task16.Lines.JUICE;
import static io.github.chris013.practice.task16.Lines.PASTA;
import static io.github.chris013.practice.task16.Lines.groceryRun;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BestOfTest {

    private final Basket basket = Basket.of(groceryRun());

    private final Promotion bakeryTenPercent = new PercentOffCategory("10% off bakery", Category.BAKERY, 10);
    private final Promotion drinksTwentyPercent = new PercentOffCategory("20% off drinks", Category.DRINKS, 20);
    private final Promotion juiceTwoForOne = new BuyXPayY("Juice 2 for 1", JUICE, 2, 1);

    @Nested
    class Choosing {

        @Test
        void picks_the_option_with_the_highest_discount() {
            // Juice 2 for 1 gives 1.99; 20% off drinks gives 0.80.
            BestOf drinksDeal = new BestOf("Drinks deal", List.of(drinksTwentyPercent, juiceTwoForOne));

            assertThat(drinksDeal.evaluate(basket)).hasValueSatisfying(d -> assertThat(d.amount()).isEqualTo("1.99"));
        }

        @Test
        void keeps_the_name_of_the_winning_option_not_the_wrapper() {
            BestOf drinksDeal = new BestOf("Drinks deal", List.of(drinksTwentyPercent, juiceTwoForOne));

            assertThat(drinksDeal.evaluate(basket)).hasValueSatisfying(d -> {
                assertThat(d.promotionName()).isEqualTo("Juice 2 for 1");
                assertThat(d.claimedSkus()).containsExactly(JUICE);
            });
        }

        @Test
        void on_a_tie_the_option_listed_first_wins() {
            Promotion first = new PercentOffCategory("First", Category.BAKERY, 10);
            Promotion second = new PercentOffCategory("Second", Category.BAKERY, 10);

            assertThat(new BestOf("Tie", List.of(first, second)).evaluate(basket))
                    .hasValueSatisfying(d -> assertThat(d.promotionName()).isEqualTo("First"));
            assertThat(new BestOf("Tie", List.of(second, first)).evaluate(basket))
                    .hasValueSatisfying(d -> assertThat(d.promotionName()).isEqualTo("Second"));
        }

        @Test
        void gives_nothing_when_no_option_applies() {
            BestOf pastaOnly = new BestOf("Pasta only",
                    List.of(new BuyXPayY("3 for 2 on pasta", PASTA, 3, 2)));

            assertThat(pastaOnly.evaluate(Basket.of(List.of()))).isEmpty();
        }

        @Test
        void skips_options_that_do_not_apply_and_picks_among_the_rest() {
            // Two butter make no full group of 3, so that option is empty; only the bakery offer is left.
            BestOf deal = new BestOf("Deal", List.of(new BuyXPayY("3 for 2 on butter", BUTTER, 3, 2), bakeryTenPercent));

            assertThat(deal.evaluate(basket)).hasValueSatisfying(d -> {
                assertThat(d.promotionName()).isEqualTo("10% off bakery");
                assertThat(d.amount()).isEqualTo("0.33");
            });
        }
    }

    @Nested
    class Composition {

        @Test
        void a_best_of_can_contain_another_best_of() {
            BestOf innerDrinks = new BestOf("Drinks deal", List.of(drinksTwentyPercent, juiceTwoForOne));
            // Pasta 3 for 2 gives 1.49 and the inner drinks deal gives 1.99, so the inner winner competes as one option.
            BestOf outer = new BestOf("Best of all", List.of(
                    new BuyXPayY("3 for 2 on pasta", PASTA, 3, 2), innerDrinks));

            assertThat(outer.evaluate(basket)).hasValueSatisfying(d -> {
                assertThat(d.promotionName()).isEqualTo("Juice 2 for 1");
                assertThat(d.amount()).isEqualTo("1.99");
            });
        }

        @Test
        void an_inner_winner_can_lose_to_a_later_option_of_the_outer_best_of() {
            BestOf innerDrinks = new BestOf("Drinks deal", List.of(drinksTwentyPercent, juiceTwoForOne));
            BestOf outer = new BestOf("Best of all", List.of(innerDrinks, bakeryTenPercent,
                    new PercentOffCategory("50% off pasta", Category.PASTA, 50)));

            // 50% off pasta is 4.47 / 2 = 2.235, which rounds to 2.24 and beats the juice deal.
            assertThat(outer.evaluate(basket)).hasValueSatisfying(d -> {
                assertThat(d.promotionName()).isEqualTo("50% off pasta");
                assertThat(d.amount()).isEqualTo("2.24");
            });
        }

        @Test
        void rejects_an_empty_list_of_options() {
            assertThatThrownBy(() -> new BestOf("Nothing", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
