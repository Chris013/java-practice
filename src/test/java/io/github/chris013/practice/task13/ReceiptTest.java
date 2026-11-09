package io.github.chris013.practice.task13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReceiptTest {

    private static LineItem item(String name, Category category, String price, int quantity) {
        return new LineItem(name, category, new BigDecimal(price), quantity);
    }

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    /** The receipt from the task text: 1.99 + 2 x 1.09 (reduced) and 6 x 0.89 + 1.79 (standard). */
    private static Receipt sampleReceipt() {
        return Receipt.of(List.of(
                item("Bananas", Category.FRUIT_AND_VEGETABLES, "1.99", 1),
                item("Milk", Category.DAIRY, "1.09", 2),
                item("Beer", Category.BEER_AND_WINE, "0.89", 6),
                item("Dish soap", Category.HOUSEHOLD, "1.79", 1)));
    }

    @Nested
    class Totals {

        @Test
        void adds_up_the_line_totals_with_scale_two() {
            assertThat(sampleReceipt().totalGross()).isEqualTo(amount("11.30"));
        }

        @Test
        void an_empty_receipt_has_zero_totals_and_nothing_to_show() {
            Receipt empty = Receipt.of(List.of());

            assertThat(empty.totalGross()).isEqualTo(amount("0.00"));
            assertThat(empty.vatBreakdown()).isEmpty();
            assertThat(empty.minimumAgeToVerify()).isEqualTo(OptionalInt.empty());
            assertThat(empty.categories()).isEmpty();
        }
    }

    @Nested
    class VatBreakdown {

        @Test
        void shows_one_row_per_rate_with_gross_net_and_vat() {
            Map<VatRate, VatLine> breakdown = sampleReceipt().vatBreakdown();

            // Reduced: 4.17 gross, 4.17 x 7 / 107 = 0.2728 -> 0.27 VAT
            assertThat(breakdown.get(VatRate.REDUCED))
                    .isEqualTo(new VatLine(VatRate.REDUCED, amount("4.17"), amount("3.90"), amount("0.27")));
            // Standard: 7.13 gross, 7.13 x 19 / 119 = 1.1384 -> 1.14 VAT
            assertThat(breakdown.get(VatRate.STANDARD))
                    .isEqualTo(new VatLine(VatRate.STANDARD, amount("7.13"), amount("5.99"), amount("1.14")));
        }

        @Test
        void the_rows_add_up_to_the_receipt_total() {
            BigDecimal sum = sampleReceipt().vatBreakdown().values().stream()
                    .map(VatLine::gross)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(sum).isEqualByComparingTo(sampleReceipt().totalGross());
        }

        @Test
        void computes_the_vat_out_of_a_gross_price() {
            // 10.00 x 19 / 119 = 1.5966 -> 1.60, so the net is 8.40
            Receipt receipt = Receipt.of(List.of(item("Cola", Category.SOFT_DRINKS, "10.00", 1)));

            assertThat(receipt.vatBreakdown().get(VatRate.STANDARD))
                    .isEqualTo(new VatLine(VatRate.STANDARD, amount("10.00"), amount("8.40"), amount("1.60")));
        }

        @Test
        void rounds_the_vat_once_per_rate_not_once_per_line() {
            // Three lines of 0.10 at 19 percent.
            // Per line: 0.10 x 19 / 119 = 0.0160 -> 0.02 each, 0.06 in total. That is wrong.
            // Per rate: 0.30 x 19 / 119 = 0.0479 -> 0.05. That is the rule.
            Receipt receipt = Receipt.of(List.of(
                    item("Sparkling water", Category.SOFT_DRINKS, "0.10", 1),
                    item("Lemonade", Category.SOFT_DRINKS, "0.10", 1),
                    item("Tonic", Category.SOFT_DRINKS, "0.10", 1)));

            assertThat(receipt.vatBreakdown().get(VatRate.STANDARD))
                    .isEqualTo(new VatLine(VatRate.STANDARD, amount("0.30"), amount("0.25"), amount("0.05")));
        }

        @Test
        void has_only_the_rates_that_occur_on_the_receipt() {
            Receipt milkOnly = Receipt.of(List.of(item("Milk", Category.DAIRY, "1.09", 2)));

            // 2.18 x 7 / 107 = 0.1426 -> 0.14, net 2.04
            assertThat(milkOnly.vatBreakdown()).containsOnlyKeys(VatRate.REDUCED);
            assertThat(milkOnly.vatBreakdown().get(VatRate.REDUCED))
                    .isEqualTo(new VatLine(VatRate.REDUCED, amount("2.18"), amount("2.04"), amount("0.14")));
        }

        @Test
        void lists_the_rates_in_enum_order_whatever_the_scan_order() {
            // Reduced item scanned first, but STANDARD (A) comes first in the breakdown.
            Receipt receipt = Receipt.of(List.of(
                    item("Bread", Category.BAKERY, "2.00", 1),
                    item("Beer", Category.BEER_AND_WINE, "1.00", 1)));

            assertThat(receipt.vatBreakdown().keySet()).containsExactly(VatRate.STANDARD, VatRate.REDUCED);
        }

        @Test
        void the_breakdown_cannot_be_changed() {
            Map<VatRate, VatLine> breakdown = sampleReceipt().vatBreakdown();

            assertThatThrownBy(breakdown::clear).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class AgeCheck {

        @Test
        void needs_no_check_when_nothing_is_restricted() {
            Receipt receipt = Receipt.of(List.of(
                    item("Bread", Category.BAKERY, "2.00", 1),
                    item("Cola", Category.SOFT_DRINKS, "1.20", 2)));

            assertThat(receipt.minimumAgeToVerify()).isEqualTo(OptionalInt.empty());
        }

        @Test
        void needs_sixteen_for_beer_and_wine() {
            Receipt receipt = Receipt.of(List.of(
                    item("Milk", Category.DAIRY, "1.09", 1),
                    item("Beer", Category.BEER_AND_WINE, "0.89", 6)));

            assertThat(receipt.minimumAgeToVerify()).isEqualTo(OptionalInt.of(16));
        }

        @Test
        void needs_eighteen_when_beer_and_tobacco_are_mixed() {
            Receipt receipt = Receipt.of(List.of(
                    item("Beer", Category.BEER_AND_WINE, "0.89", 6),
                    item("Tobacco", Category.TOBACCO, "7.50", 1)));

            assertThat(receipt.minimumAgeToVerify()).isEqualTo(OptionalInt.of(18));
        }
    }

    @Nested
    class Categories {

        @Test
        void lists_each_category_once_in_enum_order() {
            Receipt receipt = Receipt.of(List.of(
                    item("Beer", Category.BEER_AND_WINE, "0.89", 1),
                    item("Yoghurt", Category.DAIRY, "0.49", 1),
                    item("Cheese", Category.DAIRY, "2.99", 1)));

            assertThat(receipt.categories()).containsExactly(Category.DAIRY, Category.BEER_AND_WINE);
        }

        @Test
        void the_categories_cannot_be_changed() {
            assertThat(sampleReceipt().categories()).hasSize(4);
            assertThatThrownBy(() -> sampleReceipt().categories().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class DefensiveCopy {

        @Test
        void a_later_change_to_the_callers_list_does_not_change_the_receipt() {
            List<LineItem> scanned = new ArrayList<>();
            scanned.add(item("Milk", Category.DAIRY, "1.09", 1));
            Receipt receipt = Receipt.of(scanned);

            scanned.add(item("Beer", Category.BEER_AND_WINE, "0.89", 1));

            assertThat(receipt.items()).hasSize(1);
            assertThat(receipt.totalGross()).isEqualTo(amount("1.09"));
        }

        @Test
        void the_items_cannot_be_changed_through_the_receipt() {
            Receipt receipt = sampleReceipt();

            assertThat(receipt.items()).hasSize(4);
            assertThatThrownBy(() -> receipt.items().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void rejects_a_missing_list_and_a_missing_item() {
            assertThatThrownBy(() -> Receipt.of(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Receipt.of(Arrays.asList(item("Milk", Category.DAIRY, "1.09", 1), null)))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
