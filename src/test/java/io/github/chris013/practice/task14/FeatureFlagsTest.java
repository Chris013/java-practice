package io.github.chris013.practice.task14;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FeatureFlagsTest {

    private static final EvaluationContext ALICE = EvaluationContext.user("alice", "DE");
    private static final EvaluationContext BOB = EvaluationContext.user("bob", "FR");
    private static final EvaluationContext CAROL = EvaluationContext.user("carol", null);
    private static final EvaluationContext ANONYMOUS = EvaluationContext.anonymous(null);

    @Nested
    class Parsing {

        @Test
        void ignores_comments_and_blank_lines() {
            FeatureFlags flags = FeatureFlags.parse("""
                    # rollout plan for March

                    # the kill switch lives in the ops runbook
                    flag dark-mode
                      # indented comments are comments too
                      default on

                    """);

            assertThat(flags.isEnabled("dark-mode", ANONYMOUS)).isTrue();
        }

        @Test
        void reads_several_flags_and_lists_their_names() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                    flag dark-mode
                      default on
                    flag legacy-export
                      off
                    """);

            assertThat(flags.flagNames()).containsExactlyInAnyOrder("new-checkout", "dark-mode", "legacy-export");
        }

        @Test
        void an_empty_configuration_has_no_flags() {
            FeatureFlags flags = FeatureFlags.parse("# nothing yet\n\n");

            assertThat(flags.flagNames()).isEmpty();
        }

        @Test
        void accepts_flag_names_with_digits_and_hyphens() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag v2-checkout-3
                      default on
                    """);

            assertThat(flags.isEnabled("v2-checkout-3", ANONYMOUS)).isTrue();
        }

        @Test
        void accepts_tab_indented_rules() {
            FeatureFlags flags = FeatureFlags.parse("flag dark-mode\n\tdefault on");

            assertThat(flags.isEnabled("dark-mode", ANONYMOUS)).isTrue();
        }

        @Test
        void a_flag_without_any_rule_is_off() {
            FeatureFlags flags = FeatureFlags.parse("flag empty-flag");

            assertThat(flags.isEnabled("empty-flag", ALICE)).isFalse();
        }

        @Test
        void rejects_a_null_configuration() {
            assertThatThrownBy(() -> FeatureFlags.parse(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Users {

        private final FeatureFlags flags = FeatureFlags.parse("""
                flag new-checkout
                  users alice, bob
                """);

        @Test
        void is_on_for_each_listed_user() {
            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isTrue();
        }

        @Test
        void is_off_for_other_users() {
            assertThat(flags.isEnabled("new-checkout", CAROL)).isFalse();
        }

        @Test
        void never_matches_anonymous_visitors() {
            assertThat(flags.isEnabled("new-checkout", ANONYMOUS)).isFalse();
        }

        @Test
        void matches_user_ids_exactly() {
            assertThat(flags.isEnabled("new-checkout", EvaluationContext.user("Alice", "DE"))).isFalse();
        }
    }

    @Nested
    class Countries {

        private final FeatureFlags flags = FeatureFlags.parse("""
                flag new-checkout
                  countries DE, AT
                """);

        @Test
        void is_on_for_users_from_a_listed_country() {
            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", EvaluationContext.user("dora", "AT"))).isTrue();
        }

        @Test
        void is_off_for_users_from_other_countries() {
            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
        }

        @Test
        void does_not_match_when_the_country_is_unknown() {
            assertThat(flags.isEnabled("new-checkout", CAROL)).isFalse();
        }

        @Test
        void also_matches_anonymous_visitors_by_country() {
            assertThat(flags.isEnabled("new-checkout", EvaluationContext.anonymous("DE"))).isTrue();
            assertThat(flags.isEnabled("new-checkout", EvaluationContext.anonymous("CH"))).isFalse();
        }
    }

    @Nested
    class Percent {

        private static final String FLAG = "new-checkout";

        private static FeatureFlags withPercent(int percent) {
            return FeatureFlags.parse("flag " + FLAG + "\n  percent " + percent);
        }

        private static List<EvaluationContext> users(int count) {
            return IntStream.range(0, count)
                    .mapToObj(i -> EvaluationContext.user("user-" + i, "DE"))
                    .toList();
        }

        @Test
        void zero_percent_matches_nobody() {
            FeatureFlags flags = withPercent(0);

            assertThat(users(1_000)).noneMatch(user -> flags.isEnabled(FLAG, user));
        }

        @Test
        void hundred_percent_matches_every_known_user() {
            FeatureFlags flags = withPercent(100);

            assertThat(users(1_000)).allMatch(user -> flags.isEnabled(FLAG, user));
        }

        @Test
        void anonymous_visitors_never_match_a_percentage() {
            assertThat(withPercent(100).isEnabled(FLAG, ANONYMOUS)).isFalse();
        }

        @Test
        void roughly_the_configured_share_of_users_is_on() {
            FeatureFlags flags = withPercent(50);

            long on = users(10_000).stream().filter(user -> flags.isEnabled(FLAG, user)).count();

            assertThat(on).isBetween(4_700L, 5_300L);
        }

        @Test
        void the_same_user_gets_the_same_answer_every_time() {
            FeatureFlags first = withPercent(50);
            FeatureFlags second = withPercent(50);

            for (EvaluationContext user : users(1_000)) {
                boolean answer = first.isEnabled(FLAG, user);
                assertThat(first.isEnabled(FLAG, user)).isEqualTo(answer);
                assertThat(second.isEnabled(FLAG, user)).isEqualTo(answer);
            }
        }

        @Test
        void a_user_on_at_25_percent_is_still_on_at_50_percent() {
            FeatureFlags at25 = withPercent(25);
            FeatureFlags at50 = withPercent(50);

            for (EvaluationContext user : users(10_000)) {
                if (at25.isEnabled(FLAG, user)) {
                    assertThat(at50.isEnabled(FLAG, user)).as(user.userId().orElseThrow()).isTrue();
                }
            }
        }

        @Test
        void a_user_is_on_exactly_when_their_bucket_is_below_the_percentage() {
            FeatureFlags flags = withPercent(25);

            for (EvaluationContext user : users(1_000)) {
                String userId = user.userId().orElseThrow();
                // The bucket formula is part of the contract: the flag name and the user id are hashed together.
                boolean expected = Math.floorMod((FLAG + ":" + userId).hashCode(), 100) < 25;
                assertThat(flags.isEnabled(FLAG, user)).as(userId).isEqualTo(expected);
            }
        }

        @Test
        void different_flags_split_the_users_differently() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      percent 50
                    flag dark-mode
                      percent 50
                    """);

            long differing = users(1_000).stream()
                    .filter(user -> flags.isEnabled(FLAG, user) != flags.isEnabled("dark-mode", user))
                    .count();

            assertThat(differing).isGreaterThan(0);
        }
    }

    @Nested
    class Defaults {

        @Test
        void the_default_is_off_when_it_is_not_written() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                    """);

            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
        }

        @Test
        void default_on_applies_when_no_rule_matches() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      default on
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isTrue();
            assertThat(flags.isEnabled("new-checkout", ANONYMOUS)).isTrue();
        }

        @Test
        void default_off_can_be_written_explicitly() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      default off
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
        }

        @Test
        void a_flag_with_only_a_default_is_on_for_everyone() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag dark-mode
                      default on
                    """);

            assertThat(flags.isEnabled("dark-mode", ANONYMOUS)).isTrue();
            assertThat(flags.isEnabled("dark-mode", CAROL)).isTrue();
        }
    }

    @Nested
    class KillSwitch {

        @Test
        void off_turns_the_flag_off_for_everyone_whatever_the_other_lines_say() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      countries DE
                      percent 100
                      default on
                      off
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isFalse();
            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
            assertThat(flags.isEnabled("new-checkout", ANONYMOUS)).isFalse();
        }

        @Test
        void off_works_when_it_is_written_first() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      off
                      default on
                    """);

            assertThat(flags.isEnabled("new-checkout", BOB)).isFalse();
        }

        @Test
        void off_only_affects_its_own_flag() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag legacy-export
                      off
                    flag dark-mode
                      default on
                    """);

            assertThat(flags.isEnabled("legacy-export", ALICE)).isFalse();
            assertThat(flags.isEnabled("dark-mode", ALICE)).isTrue();
        }
    }

    @Nested
    class RuleOrder {

        @Test
        void a_flag_is_on_when_any_rule_matches_wherever_the_rule_is_written() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      countries FR
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isTrue();
            assertThat(flags.isEnabled("new-checkout", CAROL)).isFalse();
        }

        @Test
        void reversing_the_rules_does_not_change_any_answer() {
            FeatureFlags forward = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      countries FR
                      percent 30
                    """);
            FeatureFlags backward = FeatureFlags.parse("""
                    flag new-checkout
                      percent 30
                      countries FR
                      users alice
                    """);

            for (EvaluationContext user : List.of(ALICE, BOB, CAROL, ANONYMOUS)) {
                assertThat(backward.isEnabled("new-checkout", user))
                        .isEqualTo(forward.isEnabled("new-checkout", user));
            }
        }

        @Test
        void the_default_applies_only_when_no_rule_matched() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      users alice
                      default off
                      countries FR
                    """);

            assertThat(flags.isEnabled("new-checkout", ALICE)).isTrue();
            assertThat(flags.isEnabled("new-checkout", BOB)).isTrue();
            assertThat(flags.isEnabled("new-checkout", CAROL)).isFalse();
        }
    }

    @Nested
    class UnknownFlag {

        @Test
        void an_unknown_flag_is_off_for_everyone() {
            FeatureFlags flags = FeatureFlags.parse("""
                    flag new-checkout
                      default on
                    """);

            assertThat(flags.isEnabled("missing-flag", ALICE)).isFalse();
            assertThat(flags.isEnabled("missing-flag", ANONYMOUS)).isFalse();
        }

        @Test
        void every_flag_is_unknown_in_an_empty_configuration() {
            FeatureFlags flags = FeatureFlags.parse("");

            assertThat(flags.isEnabled("new-checkout", ALICE)).isFalse();
        }

        @Test
        void a_null_flag_name_is_a_programming_error_not_an_unknown_flag() {
            FeatureFlags flags = FeatureFlags.parse("");

            assertThatThrownBy(() -> flags.isEnabled(null, ALICE)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class EvaluationContexts {

        @Test
        void a_user_has_an_id_and_may_have_an_unknown_country() {
            EvaluationContext carol = EvaluationContext.user("carol", null);

            assertThat(carol.userId()).contains("carol");
            assertThat(carol.country()).isEmpty();
        }

        @Test
        void an_anonymous_visitor_has_no_user_id() {
            EvaluationContext visitor = EvaluationContext.anonymous("DE");

            assertThat(visitor.userId()).isEmpty();
            assertThat(visitor.country()).contains("DE");
        }

        @Test
        void an_anonymous_visitor_may_have_no_country() {
            assertThat(EvaluationContext.anonymous(null).country()).isEmpty();
        }

        @Test
        void a_user_needs_an_id_that_is_not_blank() {
            assertThatThrownBy(() -> EvaluationContext.user(null, "DE")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> EvaluationContext.user("  ", "DE")).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
