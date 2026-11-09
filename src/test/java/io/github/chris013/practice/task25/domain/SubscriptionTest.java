package io.github.chris013.practice.task25.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task25.domain.RenewalOutcome.Ended;
import io.github.chris013.practice.task25.domain.RenewalOutcome.Renewed;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.PlanChangeScheduled;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.PlanUpgraded;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.SubscriptionEnded;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.SubscriptionPastDue;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.SubscriptionReactivated;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.SubscriptionRenewed;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.SubscriptionStarted;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.SubscriptionSuspended;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SubscriptionTest {

    private static final SubscriptionId ID = new SubscriptionId("SUB-0001");
    private static final CustomerId ANNA = new CustomerId("anna");
    private static final LocalDate APRIL_1 = LocalDate.of(2026, 4, 1);
    private static final LocalDate APRIL_14 = LocalDate.of(2026, 4, 14);
    private static final LocalDate MAY_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate JUNE_1 = LocalDate.of(2026, 6, 1);

    /** A subscription that started on April 1st. Its start event is still pending. */
    private static Subscription subscription(Plan plan) {
        return Subscription.start(ID, ANNA, plan, APRIL_1);
    }

    /** Same as {@link #subscription}, but with the start event already pulled, so tests can assert on new events only. */
    private static Subscription subscriptionWithoutEvents(Plan plan) {
        Subscription subscription = subscription(plan);
        subscription.pullEvents();
        return subscription;
    }

    @Nested
    class Starting {

        @Test
        void begins_active_with_a_period_that_starts_today() {
            Subscription subscription = subscription(Plan.TEAM);

            assertThat(subscription.id()).isEqualTo(ID);
            assertThat(subscription.customerId()).isEqualTo(ANNA);
            assertThat(subscription.plan()).isEqualTo(Plan.TEAM);
            assertThat(subscription.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription.currentPeriod()).isEqualTo(new BillingPeriod(APRIL_1, MAY_1));
            assertThat(subscription.scheduledPlan()).isEmpty();
            assertThat(subscription.cancelAtPeriodEnd()).isFalse();
        }

        @Test
        void allows_the_free_plan() {
            assertThat(subscription(Plan.FREE).plan()).isEqualTo(Plan.FREE);
        }

        @Test
        void records_subscription_started_as_its_first_event() {
            assertThat(subscription(Plan.TEAM).pullEvents())
                    .containsExactly(new SubscriptionStarted(ID, ANNA, Plan.TEAM, APRIL_1));
        }
    }

    @Nested
    class Upgrading {

        @Test
        void prorates_both_plans_on_day_14_of_a_30_day_period() {
            // TEAM unused: 29.00 x 17 / 30 = 16.433... -> 16.43, credited.
            // BUSINESS remaining: 79.00 x 17 / 30 = 44.766... -> 44.77, charged.
            List<InvoiceLine> lines = subscription(Plan.TEAM).changePlan(Plan.BUSINESS, APRIL_14);

            assertThat(lines).containsExactly(
                    new InvoiceLine("Unused time on TEAM (17 of 30 days)", new BigDecimal("-16.43")),
                    new InvoiceLine("Remaining time on BUSINESS (17 of 30 days)", new BigDecimal("44.77")));
        }

        @Test
        void rounds_each_line_separately_so_the_total_is_28_34_not_the_exact_28_33() {
            // Exact difference: (79.00 - 29.00) x 17 / 30 = 28.333... Rounded lines: 44.77 - 16.43 = 28.34.
            List<InvoiceLine> lines = subscription(Plan.TEAM).changePlan(Plan.BUSINESS, APRIL_14);

            BigDecimal total = new Invoice(new InvoiceId("INV-1"), ID, ANNA, APRIL_14, lines).total();

            assertThat(total).isEqualByComparingTo("28.34");
        }

        @Test
        void an_upgrade_on_the_first_day_credits_and_charges_whole_months() {
            List<InvoiceLine> lines = subscription(Plan.TEAM).changePlan(Plan.BUSINESS, APRIL_1);

            assertThat(lines).containsExactly(
                    new InvoiceLine("Unused time on TEAM (30 of 30 days)", new BigDecimal("-29.00")),
                    new InvoiceLine("Remaining time on BUSINESS (30 of 30 days)", new BigDecimal("79.00")));
        }

        @Test
        void an_upgrade_from_free_has_a_zero_credit_line() {
            // TEAM on day 15: 16 days remain. 29.00 x 16 / 30 = 15.466... -> 15.47.
            List<InvoiceLine> lines = subscription(Plan.FREE).changePlan(Plan.TEAM, LocalDate.of(2026, 4, 15));

            assertThat(lines).containsExactly(
                    new InvoiceLine("Unused time on FREE (16 of 30 days)", new BigDecimal("0.00")),
                    new InvoiceLine("Remaining time on TEAM (16 of 30 days)", new BigDecimal("15.47")));
        }

        @Test
        void takes_effect_immediately() {
            Subscription subscription = subscription(Plan.TEAM);

            subscription.changePlan(Plan.BUSINESS, APRIL_14);

            assertThat(subscription.plan()).isEqualTo(Plan.BUSINESS);
        }

        @Test
        void records_a_plan_upgraded_event() {
            Subscription subscription = subscriptionWithoutEvents(Plan.TEAM);

            subscription.changePlan(Plan.BUSINESS, APRIL_14);

            assertThat(subscription.pullEvents())
                    .containsExactly(new PlanUpgraded(ID, Plan.TEAM, Plan.BUSINESS, APRIL_14));
        }

        @Test
        void clears_a_scheduled_downgrade() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.changePlan(Plan.FREE, APRIL_1);
            assertThat(subscription.scheduledPlan()).contains(Plan.FREE);

            subscription.changePlan(Plan.BUSINESS, APRIL_14);

            assertThat(subscription.scheduledPlan()).isEmpty();
        }

        @Test
        void rejects_the_plan_the_subscription_already_has() {
            assertThatThrownBy(() -> subscription(Plan.TEAM).changePlan(Plan.TEAM, APRIL_14))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_change_unless_the_subscription_is_active() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.paymentFailed(APRIL_1);

            assertThatThrownBy(() -> subscription.changePlan(Plan.BUSINESS, APRIL_14))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void rejects_a_change_once_the_period_has_ended_and_renewal_is_due() {
            assertThatThrownBy(() -> subscription(Plan.TEAM).changePlan(Plan.BUSINESS, MAY_1))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Downgrading {

        @Test
        void is_scheduled_for_the_next_period_and_returns_no_lines() {
            Subscription subscription = subscription(Plan.BUSINESS);

            List<InvoiceLine> lines = subscription.changePlan(Plan.TEAM, APRIL_14);

            assertThat(lines).isEmpty();
            assertThat(subscription.scheduledPlan()).contains(Plan.TEAM);
        }

        @Test
        void keeps_the_higher_plan_until_the_period_ends() {
            Subscription subscription = subscription(Plan.BUSINESS);

            subscription.changePlan(Plan.TEAM, APRIL_14);

            assertThat(subscription.plan()).isEqualTo(Plan.BUSINESS);
        }

        @Test
        void records_a_plan_change_scheduled_event_dated_at_the_period_end() {
            Subscription subscription = subscriptionWithoutEvents(Plan.BUSINESS);

            subscription.changePlan(Plan.TEAM, APRIL_14);

            assertThat(subscription.pullEvents())
                    .containsExactly(new PlanChangeScheduled(ID, Plan.BUSINESS, Plan.TEAM, MAY_1));
        }

        @Test
        void a_second_downgrade_replaces_the_first() {
            Subscription subscription = subscription(Plan.BUSINESS);

            subscription.changePlan(Plan.TEAM, APRIL_14);
            subscription.changePlan(Plan.FREE, APRIL_14);

            assertThat(subscription.scheduledPlan()).contains(Plan.FREE);
        }
    }

    @Nested
    class CancellingAndResuming {

        @Test
        void cancelling_sets_the_flag_but_keeps_the_subscription_active() {
            Subscription subscription = subscription(Plan.TEAM);

            subscription.cancelAtPeriodEnd(APRIL_14);

            assertThat(subscription.cancelAtPeriodEnd()).isTrue();
            assertThat(subscription.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription.plan()).isEqualTo(Plan.TEAM);
        }

        @Test
        void cancelling_twice_changes_nothing() {
            Subscription subscription = subscriptionWithoutEvents(Plan.TEAM);

            subscription.cancelAtPeriodEnd(APRIL_14);
            subscription.cancelAtPeriodEnd(APRIL_14);

            assertThat(subscription.cancelAtPeriodEnd()).isTrue();
            assertThat(subscription.pullEvents()).isEmpty();
        }

        @Test
        void resuming_undoes_the_cancellation() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.cancelAtPeriodEnd(APRIL_14);

            subscription.resume();

            assertThat(subscription.cancelAtPeriodEnd()).isFalse();
            assertThat(subscription.renew(MAY_1)).isEqualTo(
                    new Renewed(List.of(new InvoiceLine("TEAM plan, 2026-05-01 to 2026-05-31", new BigDecimal("29.00")))));
        }

        @Test
        void a_suspended_subscription_cannot_be_cancelled() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.paymentFailed(APRIL_1);
            subscription.paymentFailed(APRIL_1.plusDays(14));

            assertThatThrownBy(() -> subscription.cancelAtPeriodEnd(APRIL_14))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void a_cancellation_is_rejected_once_the_period_has_ended() {
            assertThatThrownBy(() -> subscription(Plan.TEAM).cancelAtPeriodEnd(MAY_1))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Renewal {

        @Test
        void is_not_allowed_the_day_before_the_period_ends() {
            assertThatThrownBy(() -> subscription(Plan.TEAM).renew(MAY_1.minusDays(1)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void starts_the_next_period_and_bills_it_on_the_day_the_period_ends() {
            Subscription subscription = subscription(Plan.TEAM);

            RenewalOutcome outcome = subscription.renew(MAY_1);

            assertThat(outcome).isEqualTo(
                    new Renewed(List.of(new InvoiceLine("TEAM plan, 2026-05-01 to 2026-05-31", new BigDecimal("29.00")))));
            assertThat(subscription.currentPeriod()).isEqualTo(new BillingPeriod(MAY_1, JUNE_1));
        }

        @Test
        void applies_a_scheduled_downgrade_and_bills_the_new_plan() {
            Subscription subscription = subscription(Plan.BUSINESS);
            subscription.changePlan(Plan.TEAM, APRIL_14);

            RenewalOutcome outcome = subscription.renew(MAY_1);

            assertThat(outcome).isEqualTo(
                    new Renewed(List.of(new InvoiceLine("TEAM plan, 2026-05-01 to 2026-05-31", new BigDecimal("29.00")))));
            assertThat(subscription.plan()).isEqualTo(Plan.TEAM);
            assertThat(subscription.scheduledPlan()).isEmpty();
        }

        @Test
        void a_free_plan_renews_without_any_lines() {
            Subscription subscription = subscription(Plan.FREE);

            assertThat(subscription.renew(MAY_1)).isEqualTo(new Renewed(List.of()));
            assertThat(subscription.currentPeriod()).isEqualTo(new BillingPeriod(MAY_1, JUNE_1));
        }

        @Test
        void moves_one_period_per_call_even_when_several_are_due() {
            Subscription subscription = subscription(Plan.TEAM);

            subscription.renew(JUNE_1);

            assertThat(subscription.currentPeriod()).isEqualTo(new BillingPeriod(MAY_1, JUNE_1));
        }

        @Test
        void records_a_renewed_event_with_the_new_period() {
            Subscription subscription = subscriptionWithoutEvents(Plan.TEAM);

            subscription.renew(MAY_1);

            assertThat(subscription.pullEvents())
                    .containsExactly(new SubscriptionRenewed(ID, Plan.TEAM, new BillingPeriod(MAY_1, JUNE_1)));
        }

        @Test
        void a_cancelled_subscription_ends_instead_of_renewing() {
            Subscription subscription = subscriptionWithoutEvents(Plan.TEAM);
            subscription.cancelAtPeriodEnd(APRIL_14);

            RenewalOutcome outcome = subscription.renew(MAY_1);

            assertThat(outcome).isEqualTo(new Ended());
            assertThat(subscription.status()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(subscription.pullEvents()).containsExactly(new SubscriptionEnded(ID, MAY_1));
        }

        @Test
        void an_ended_subscription_cannot_be_renewed_or_resumed() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.cancelAtPeriodEnd(APRIL_14);
            subscription.renew(MAY_1);

            assertThatThrownBy(() -> subscription.renew(JUNE_1)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(subscription::resume).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Dunning {

        @Test
        void the_first_failed_payment_makes_the_subscription_past_due() {
            Subscription subscription = subscriptionWithoutEvents(Plan.TEAM);

            subscription.paymentFailed(APRIL_14);

            assertThat(subscription.status()).isEqualTo(SubscriptionStatus.PAST_DUE);
            assertThat(subscription.pullEvents()).containsExactly(new SubscriptionPastDue(ID, APRIL_14));
        }

        @ParameterizedTest
        @CsvSource({"0, PAST_DUE", "13, PAST_DUE", "14, SUSPENDED", "30, SUSPENDED"})
        void suspends_the_subscription_once_14_days_have_passed_since_the_first_failure(
                int daysAfterFirstFailure, SubscriptionStatus expected) {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.paymentFailed(APRIL_1);

            subscription.paymentFailed(APRIL_1.plusDays(daysAfterFirstFailure));

            assertThat(subscription.status()).isEqualTo(expected);
        }

        @Test
        void later_failures_do_not_move_the_first_failure_date() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.paymentFailed(APRIL_1);
            subscription.paymentFailed(APRIL_1.plusDays(10));

            subscription.paymentFailed(APRIL_1.plusDays(14));

            assertThat(subscription.status()).isEqualTo(SubscriptionStatus.SUSPENDED);
        }

        @Test
        void a_suspension_is_recorded_as_an_event() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.paymentFailed(APRIL_1);
            subscription.pullEvents();

            subscription.paymentFailed(APRIL_1.plusDays(14));

            assertThat(subscription.pullEvents())
                    .containsExactly(new SubscriptionSuspended(ID, APRIL_1.plusDays(14)));
        }

        @Test
        void a_successful_payment_reactivates_a_past_due_subscription_and_restarts_the_grace_period() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.paymentFailed(APRIL_1);

            subscription.paymentSucceeded();
            assertThat(subscription.status()).isEqualTo(SubscriptionStatus.ACTIVE);

            subscription.paymentFailed(APRIL_1.plusDays(20));
            assertThat(subscription.status()).isEqualTo(SubscriptionStatus.PAST_DUE);
        }

        @Test
        void a_successful_payment_reactivates_a_suspended_subscription() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.paymentFailed(APRIL_1);
            subscription.paymentFailed(APRIL_1.plusDays(14));

            subscription.paymentSucceeded();

            assertThat(subscription.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        void a_reactivation_is_recorded_as_an_event() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.paymentFailed(APRIL_1);
            subscription.pullEvents();

            subscription.paymentSucceeded();

            assertThat(subscription.pullEvents()).containsExactly(new SubscriptionReactivated(ID));
        }

        @Test
        void a_successful_payment_on_an_active_subscription_changes_nothing() {
            Subscription subscription = subscriptionWithoutEvents(Plan.TEAM);

            subscription.paymentSucceeded();

            assertThat(subscription.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription.pullEvents()).isEmpty();
        }

        @Test
        void a_failed_payment_on_a_cancelled_subscription_is_rejected() {
            Subscription subscription = subscription(Plan.TEAM);
            subscription.cancelAtPeriodEnd(APRIL_14);
            subscription.renew(MAY_1);

            assertThatThrownBy(() -> subscription.paymentFailed(MAY_1)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Events {

        @Test
        void pull_events_hands_over_each_event_only_once() {
            Subscription subscription = subscription(Plan.TEAM);

            assertThat(subscription.pullEvents()).hasSize(1);
            assertThat(subscription.pullEvents()).isEmpty();
        }

        @Test
        void events_come_out_in_the_order_they_happened() {
            Subscription subscription = subscription(Plan.TEAM);

            subscription.changePlan(Plan.BUSINESS, APRIL_14);
            subscription.paymentFailed(APRIL_14);
            subscription.renew(MAY_1);

            assertThat(subscription.pullEvents()).containsExactly(
                    new SubscriptionStarted(ID, ANNA, Plan.TEAM, APRIL_1),
                    new PlanUpgraded(ID, Plan.TEAM, Plan.BUSINESS, APRIL_14),
                    new SubscriptionPastDue(ID, APRIL_14),
                    new SubscriptionRenewed(ID, Plan.BUSINESS, new BillingPeriod(MAY_1, JUNE_1)));
        }
    }
}
