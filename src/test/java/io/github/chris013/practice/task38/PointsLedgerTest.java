package io.github.chris013.practice.task38;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PointsLedgerTest {

    private final PointsLedger ledger = new PointsLedger();

    @Nested
    class Earn {

        @Test
        void gives_a_new_member_the_points() {
            ledger.earn("anna", 120);

            assertThat(ledger.balance("anna")).isEqualTo(120);
        }

        @Test
        void adds_points_to_an_existing_balance() {
            ledger.earn("anna", 120);
            ledger.earn("anna", 30);

            assertThat(ledger.balance("anna")).isEqualTo(150);
            assertThat(ledger.memberCount()).isEqualTo(1);
        }

        @Test
        void ignores_surrounding_whitespace_in_the_name() {
            ledger.earn("  anna ", 10);

            assertThat(ledger.balance("anna")).isEqualTo(10);
            assertThat(ledger.isMember(" anna")).isTrue();
        }

        @Test
        void refuses_to_overflow_and_keeps_the_old_balance() {
            ledger.earn("anna", Integer.MAX_VALUE);

            assertThatThrownBy(() -> ledger.earn("anna", 1)).isInstanceOf(ArithmeticException.class);
            assertThat(ledger.balance("anna")).isEqualTo(Integer.MAX_VALUE);
        }

        @ParameterizedTest
        @CsvSource({"0", "-5"})
        void rejects_points_that_are_not_positive(int points) {
            assertThatThrownBy(() -> ledger.earn("anna", points)).isInstanceOf(IllegalArgumentException.class);
            assertThat(ledger.memberCount()).isZero();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejects_a_blank_member_name(String member) {
            assertThatThrownBy(() -> ledger.earn(member, 10)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_null_member_name() {
            assertThatThrownBy(() -> ledger.earn(null, 10)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Membership {

        @Test
        void an_unknown_member_has_zero_points_and_is_not_a_member() {
            assertThat(ledger.balance("nobody")).isZero();
            assertThat(ledger.isMember("nobody")).isFalse();
        }

        @Test
        void counts_only_members_with_a_balance() {
            ledger.earn("anna", 10);
            ledger.earn("ben", 20);

            assertThat(ledger.memberCount()).isEqualTo(2);
        }
    }

    @Nested
    class Redeem {

        @Test
        void reduces_the_balance() {
            ledger.earn("anna", 500);

            ledger.redeem("anna", 200);

            assertThat(ledger.balance("anna")).isEqualTo(300);
        }

        @Test
        void removes_the_member_when_the_balance_reaches_exactly_zero() {
            ledger.earn("anna", 200);

            ledger.redeem("anna", 200);

            assertThat(ledger.isMember("anna")).isFalse();
            assertThat(ledger.memberCount()).isZero();
        }

        @Test
        void keeps_the_member_when_one_point_is_left() {
            ledger.earn("anna", 200);

            ledger.redeem("anna", 199);

            assertThat(ledger.isMember("anna")).isTrue();
            assertThat(ledger.balance("anna")).isEqualTo(1);
        }

        @Test
        void rejects_redeeming_more_than_the_balance_with_a_clear_message() {
            ledger.earn("anna", 120);

            assertThatThrownBy(() -> ledger.redeem("anna", 200))
                    .isInstanceOf(InsufficientPointsException.class)
                    .hasMessage("anna has 120 points, cannot redeem 200");
        }

        @Test
        void the_insufficient_points_exception_carries_the_numbers() {
            ledger.earn("anna", 120);

            assertThatThrownBy(() -> ledger.redeem("anna", 200))
                    .isInstanceOfSatisfying(InsufficientPointsException.class, ex -> {
                        assertThat(ex.member()).isEqualTo("anna");
                        assertThat(ex.balance()).isEqualTo(120);
                        assertThat(ex.requested()).isEqualTo(200);
                    });
        }

        @Test
        void leaves_the_ledger_unchanged_when_the_redemption_is_rejected() {
            ledger.earn("anna", 120);
            ledger.earn("ben", 50);

            assertThatThrownBy(() -> ledger.redeem("anna", 200)).isInstanceOf(InsufficientPointsException.class);

            assertThat(ledger.balance("anna")).isEqualTo(120);
            assertThat(ledger.balance("ben")).isEqualTo(50);
            assertThat(ledger.memberCount()).isEqualTo(2);
        }

        @Test
        void rejects_redeeming_from_an_unknown_member() {
            assertThatThrownBy(() -> ledger.redeem("ben", 10))
                    .isInstanceOf(InsufficientPointsException.class)
                    .hasMessage("ben has 0 points, cannot redeem 10");
            assertThat(ledger.isMember("ben")).isFalse();
        }

        @ParameterizedTest
        @CsvSource({"0", "-1"})
        void rejects_points_that_are_not_positive(int points) {
            ledger.earn("anna", 100);

            assertThatThrownBy(() -> ledger.redeem("anna", points)).isInstanceOf(IllegalArgumentException.class);
            assertThat(ledger.balance("anna")).isEqualTo(100);
        }
    }

    @Nested
    class Expire {

        @Test
        void removes_members_strictly_below_the_threshold() {
            ledger.earn("anna", 120);
            ledger.earn("ben", 500);
            ledger.earn("cy", 1000);

            ledger.expireBelow(500);

            assertThat(ledger.isMember("anna")).isFalse();
            assertThat(ledger.isMember("ben")).isTrue();
            assertThat(ledger.isMember("cy")).isTrue();
            assertThat(ledger.memberCount()).isEqualTo(2);
        }

        @Test
        void keeps_a_member_whose_balance_equals_the_threshold() {
            ledger.earn("ben", 500);

            ledger.expireBelow(500);

            assertThat(ledger.balance("ben")).isEqualTo(500);
        }

        @ParameterizedTest
        @CsvSource({"0", "-1"})
        void rejects_a_threshold_that_is_not_positive(int threshold) {
            assertThatThrownBy(() -> ledger.expireBelow(threshold)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Bonus {

        @ParameterizedTest
        @CsvSource({
            "1005, 10, 1105",
            "99, 1, 99",
            "2000, 1, 2020",
            "1, 1, 1",
            "100, 100, 200"
        })
        void adds_the_percentage_rounded_down(int start, int percent, int expected) {
            ledger.earn("anna", start);

            ledger.applyBonusPercent(percent);

            assertThat(ledger.balance("anna")).isEqualTo(expected);
        }

        @Test
        void applies_the_bonus_to_every_member() {
            ledger.earn("anna", 1000);
            ledger.earn("ben", 2000);

            ledger.applyBonusPercent(10);

            assertThat(ledger.balance("anna")).isEqualTo(1100);
            assertThat(ledger.balance("ben")).isEqualTo(2200);
        }

        @ParameterizedTest
        @CsvSource({"0", "101"})
        void rejects_a_percentage_outside_1_to_100(int percent) {
            ledger.earn("anna", 1000);

            assertThatThrownBy(() -> ledger.applyBonusPercent(percent)).isInstanceOf(IllegalArgumentException.class);
            assertThat(ledger.balance("anna")).isEqualTo(1000);
        }

        @Test
        void refuses_to_overflow_and_changes_no_balance() {
            ledger.earn("anna", Integer.MAX_VALUE);
            ledger.earn("ben", 1);

            assertThatThrownBy(() -> ledger.applyBonusPercent(1)).isInstanceOf(ArithmeticException.class);

            assertThat(ledger.balance("anna")).isEqualTo(Integer.MAX_VALUE);
            assertThat(ledger.balance("ben")).isEqualTo(1);
        }
    }

    @Nested
    class Tiers {

        @Test
        void groups_members_by_tier_with_names_sorted_and_tiers_in_enum_order() {
            ledger.earn("dora", 999);
            ledger.earn("anna", 5000);
            ledger.earn("ben", 1000);
            ledger.earn("cy", 120);
            ledger.earn("aaron", 6000);

            assertThat(ledger.membersByTier()).containsExactly(
                    entry(Tier.BRONZE, List.of("cy", "dora")),
                    entry(Tier.SILVER, List.of("ben")),
                    entry(Tier.GOLD, List.of("aaron", "anna")));
        }

        @Test
        void leaves_out_tiers_without_members() {
            ledger.earn("cy", 120);

            assertThat(ledger.membersByTier().keySet()).containsExactly(Tier.BRONZE);
        }

        @Test
        void is_empty_for_an_empty_ledger() {
            assertThat(ledger.membersByTier()).isEmpty();
        }

        @Test
        void returns_an_unmodifiable_map() {
            ledger.earn("cy", 120);
            Map<Tier, List<String>> byTier = ledger.membersByTier();

            assertThatThrownBy(() -> byTier.put(Tier.GOLD, List.of())).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void returns_unmodifiable_member_lists() {
            ledger.earn("cy", 120);
            List<String> bronze = ledger.membersByTier().get(Tier.BRONZE);

            assertThatThrownBy(() -> bronze.add("mallory")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void is_a_copy_that_does_not_follow_later_changes() {
            ledger.earn("cy", 120);
            Map<Tier, List<String>> before = ledger.membersByTier();

            ledger.earn("dora", 10);

            assertThat(before.get(Tier.BRONZE)).containsExactly("cy");
        }
    }

    @Nested
    class Snapshot {

        @Test
        void copies_every_balance() {
            ledger.earn("anna", 120);
            ledger.earn("ben", 50);

            assertThat(ledger.snapshot()).containsExactlyInAnyOrderEntriesOf(Map.of("anna", 120, "ben", 50));
        }

        @Test
        void is_independent_of_later_changes() {
            ledger.earn("anna", 120);
            Map<String, Integer> before = ledger.snapshot();

            ledger.earn("anna", 30);
            ledger.earn("ben", 50);
            ledger.redeem("anna", 150);

            assertThat(before).containsExactly(entry("anna", 120));
        }

        @Test
        void cannot_be_modified() {
            Map<String, Integer> snapshot = ledger.snapshot();

            assertThatThrownBy(() -> snapshot.put("mallory", 1_000_000))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
