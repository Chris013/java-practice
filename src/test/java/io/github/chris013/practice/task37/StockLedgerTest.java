package io.github.chris013.practice.task37;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StockLedgerTest {

    private final StockLedger ledger = new StockLedger();

    @Nested
    class Receiving {

        @Test
        void adds_up_the_quantities_received_for_each_sku() {
            ledger.receive("MUG-WHITE", 5);
            ledger.receive("MUG-WHITE", 3);
            ledger.receive("PLATE-BLUE", 2);

            assertThat(ledger.quantityOf("MUG-WHITE")).isEqualTo(8);
            assertThat(ledger.quantityOf("PLATE-BLUE")).isEqualTo(2);
        }

        @ParameterizedTest
        @CsvSource({"0", "-1", "-1000"})
        void rejects_quantities_that_are_not_positive(int quantity) {
            ledger.receive("MUG-WHITE", 4);

            assertThatThrownBy(() -> ledger.receive("MUG-WHITE", quantity))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(ledger.totalUnits()).isEqualTo(4L);
        }
    }

    @Nested
    class UnknownSku {

        // Bug 1: quantityOf unboxes the null that Map.get returns, so this throws NullPointerException.
        @Test
        void an_sku_we_never_received_has_zero_units() {
            assertThat(ledger.quantityOf("NEVER-SEEN")).isZero();
        }

        @Test
        void a_received_sku_is_no_longer_unknown() {
            ledger.receive("NEVER-SEEN", 1);

            assertThat(ledger.quantityOf("NEVER-SEEN")).isOne();
        }
    }

    @Nested
    class BinIdentity {

        @Test
        void the_boxed_values_used_below_are_different_objects_outside_the_cache() {
            // The precondition of the bugs: Integer.valueOf caches only -128..127.
            assertThat(Integer.valueOf(128)).isNotSameAs(Integer.valueOf(128));
            assertThat(Integer.valueOf(200)).isNotSameAs(Integer.valueOf(200));
            assertThat(Integer.valueOf(1000)).isNotSameAs(Integer.valueOf(1000));
            assertThat(Integer.valueOf(-129)).isNotSameAs(Integer.valueOf(-129));
            assertThat(Integer.valueOf(127)).isSameAs(Integer.valueOf(127));
            assertThat(Integer.valueOf(-128)).isSameAs(Integer.valueOf(-128));
        }

        // Bug 2: sameBin compares references with ==, so 200 and 200 are "different" bins.
        @ParameterizedTest
        @CsvSource({
            "1,1,true",
            "127,127,true",
            "128,128,true",
            "200,200,true",
            "-129,-129,true",
            "200,201,false",
        })
        void compares_bin_numbers_by_value(int binA, int binB, boolean expected) {
            Integer a = Integer.valueOf(binA);
            Integer b = Integer.valueOf(binB);

            assertThat(StockLedger.sameBin(a, b)).isEqualTo(expected);
        }

        @Test
        void two_missing_bins_are_the_same_and_a_missing_bin_is_not_any_number() {
            assertThat(StockLedger.sameBin(null, null)).isTrue();
            assertThat(StockLedger.sameBin(null, 3)).isFalse();
            assertThat(StockLedger.sameBin(3, null)).isFalse();
        }
    }

    @Nested
    class OpenBins {

        @Test
        void lists_the_open_bins_in_the_order_they_were_opened() {
            openBins(5, 3, 9, 4);

            assertThat(ledger.openBins()).containsExactly(5, 3, 9, 4);
        }

        @Test
        void the_list_of_open_bins_cannot_be_changed_from_outside() {
            openBins(5, 3);

            assertThatThrownBy(() -> ledger.openBins().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        // Bug 3: closeBin calls remove(int), which removes by index. Closing 3 removes the bin at index 3.
        @Test
        void closing_a_bin_removes_the_bin_with_that_number() {
            openBins(5, 3, 9, 4);

            ledger.closeBin(3);

            assertThat(ledger.openBins()).containsExactly(5, 9, 4);
        }

        @Test
        void closing_another_bin_removes_only_that_number() {
            openBins(5, 3, 9, 4);

            ledger.closeBin(9);

            assertThat(ledger.openBins()).containsExactly(5, 3, 4);
        }

        @Test
        void closing_a_bin_that_is_not_open_changes_nothing() {
            openBins(5, 3, 9, 4);

            ledger.closeBin(42);

            assertThat(ledger.openBins()).containsExactly(5, 3, 9, 4);
        }

        private void openBins(int... bins) {
            for (int bin : bins) {
                ledger.openBin(bin);
            }
        }
    }

    @Nested
    class Comparison {

        // Bug 4: the hand-written lambda compares the references of two equal boxed values above 127.
        @Test
        void equal_quantities_above_127_compare_as_equal() {
            assertThat(StockLedger.BY_QUANTITY.compare(Integer.valueOf(1000), Integer.valueOf(1000))).isZero();
        }

        @ParameterizedTest
        @CsvSource({
            "1,2,-1",
            "2,1,1",
            "127,128,-1",
            "128,127,1",
            "-129,-128,-1",
            "200,200,0",
            "1000,1000,0",
        })
        void orders_boxed_quantities_by_value(int a, int b, int expectedSign) {
            int result = StockLedger.BY_QUANTITY.compare(Integer.valueOf(a), Integer.valueOf(b));

            assertThat(Integer.signum(result)).isEqualTo(expectedSign);
        }

        // Characterization: passes on the starter too, because TimSort never merges a list this short.
        @Test
        void sorts_a_short_list_correctly_even_with_the_broken_comparator() {
            ledger.receive("A", 1000);
            ledger.receive("B", 5);
            ledger.receive("C", 1000);
            ledger.receive("D", 200);
            ledger.receive("E", 127);
            ledger.receive("F", 128);

            assertThat(ledger.sortedQuantities()).containsExactly(5, 127, 128, 200, 1000, 1000);
        }

        // Bug 4 at production size: TimSort uses insertion sort below 32 elements and merges from 32 on. Only the
        // merge checks the comparator's contract, so the broken lambda throws "Comparison method violates its
        // general contract!" for some lists of 32 or more values, including this one of exactly 32.
        @Test
        void sorts_many_quantities_that_include_repeated_large_values() {
            List<Integer> expected = new ArrayList<>();
            for (int sku = 0; sku < 32; sku++) {
                int quantity = sku % 3 == 0 ? 1000 : sku;
                ledger.receive("SKU-" + sku, quantity);
                expected.add(quantity);
            }

            assertThat(ledger.sortedQuantities()).containsExactlyElementsOf(expected.stream().sorted().toList());
        }

        @Test
        void sorted_quantities_are_a_new_list_each_time() {
            ledger.receive("A", 7);

            List<Integer> first = ledger.sortedQuantities();

            assertThat(first).containsExactly(7);
            assertThat(ledger.sortedQuantities()).isNotSameAs(first);
        }
    }

    @Nested
    class Totals {

        @Test
        void an_empty_ledger_holds_zero_units() {
            assertThat(ledger.totalUnits()).isZero();
        }

        @Test
        void adds_the_units_of_all_skus() {
            ledger.receive("MUG-WHITE", 5);
            ledger.receive("PLATE-BLUE", 2);
            ledger.receive("MUG-WHITE", 3);

            assertThat(ledger.totalUnits()).isEqualTo(10L);
        }

        @Test
        void the_total_is_not_limited_to_the_int_range() {
            ledger.receive("A", 1_000_000_000);
            ledger.receive("B", 1_000_000_000);
            ledger.receive("C", 1_000_000_000);

            assertThat(ledger.totalUnits()).isEqualTo(3_000_000_000L);
        }

        // Bug 5 (performance only): the boxed Long total gives the same answer, so no test can fail on it.
        // The fix is in the reference solution, and the md explains why a benchmark would show it.
        @Test
        void sums_many_skus_correctly() {
            for (int sku = 0; sku < 1_000; sku++) {
                ledger.receive("SKU-" + sku, 1_000);
            }

            assertThat(ledger.totalUnits()).isEqualTo(1_000_000L);
        }
    }
}
