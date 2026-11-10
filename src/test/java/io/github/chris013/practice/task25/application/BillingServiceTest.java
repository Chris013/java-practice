package io.github.chris013.practice.task25.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task25.adapter.inmemory.InMemoryInvoices;
import io.github.chris013.practice.task25.adapter.inmemory.InMemorySubscriptions;
import io.github.chris013.practice.task25.adapter.inmemory.SequentialInvoiceIds;
import io.github.chris013.practice.task25.adapter.inmemory.SequentialSubscriptionIds;
import io.github.chris013.practice.task25.application.FakePaymentProvider.Charge;
import io.github.chris013.practice.task25.domain.BillingPeriod;
import io.github.chris013.practice.task25.domain.CustomerId;
import io.github.chris013.practice.task25.domain.Invoice;
import io.github.chris013.practice.task25.domain.InvoiceId;
import io.github.chris013.practice.task25.domain.InvoiceLine;
import io.github.chris013.practice.task25.domain.Plan;
import io.github.chris013.practice.task25.domain.Subscription;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.SubscriptionReactivated;
import io.github.chris013.practice.task25.domain.SubscriptionEvent.SubscriptionStarted;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import io.github.chris013.practice.task25.domain.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BillingServiceTest {

    private static final CustomerId ANNA = new CustomerId("anna");
    private static final CustomerId BEN = new CustomerId("ben");
    private static final LocalDate APRIL_1 = LocalDate.of(2026, 4, 1);
    private static final LocalDate APRIL_14 = LocalDate.of(2026, 4, 14);
    private static final LocalDate MAY_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate JUNE_1 = LocalDate.of(2026, 6, 1);
    private static final BillingRunReport NOTHING_HAPPENED = new BillingRunReport(0, 0, 0, 0, 0);

    private final MutableClock clock = new MutableClock(APRIL_1);
    private final InMemorySubscriptions subscriptions = new InMemorySubscriptions();
    private final InMemoryInvoices invoices = new InMemoryInvoices();
    private final FakePaymentProvider payments = new FakePaymentProvider();
    private final List<String> log = new ArrayList<>();
    private final RecordingEventPublisher publisher = new RecordingEventPublisher(log);
    private final BillingService billing = serviceWith(subscriptions);

    private BillingService serviceWith(SubscriptionRepository repository) {
        return new BillingService(
                repository, invoices, payments, publisher,
                new SequentialSubscriptionIds(), new SequentialInvoiceIds(), clock);
    }

    private Subscription subscription(SubscriptionId id) {
        return subscriptions.findById(id).orElseThrow();
    }

    /** Wraps the real store and writes "saved" to the shared log, so the order of saves and publishes can be checked. */
    private record LoggingSubscriptions(SubscriptionRepository delegate, List<String> log)
            implements SubscriptionRepository {

        @Override
        public Optional<Subscription> findById(SubscriptionId id) {
            return delegate.findById(id);
        }

        @Override
        public List<Subscription> findDueForRenewal(LocalDate today) {
            return delegate.findDueForRenewal(today);
        }

        @Override
        public void save(Subscription subscription) {
            log.add("saved");
            delegate.save(subscription);
        }
    }

    @Nested
    class Subscribing {

        @Test
        void gives_each_new_subscription_the_next_sequential_id() {
            SubscriptionId first = billing.subscribe(ANNA, Plan.TEAM);
            SubscriptionId second = billing.subscribe(BEN, Plan.FREE);

            assertThat(first.value()).isEqualTo("SUB-0001");
            assertThat(second.value()).isEqualTo("SUB-0002");
            assertThat(subscription(first).plan()).isEqualTo(Plan.TEAM);
            assertThat(subscription(second).customerId()).isEqualTo(BEN);
        }

        @Test
        void a_new_subscription_is_active_on_the_day_it_starts() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);

            assertThat(subscription(id).status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription(id).currentPeriod()).isEqualTo(new BillingPeriod(APRIL_1, MAY_1));
        }
    }

    @Nested
    class Upgrading {

        @Test
        void an_upgrade_issues_a_proration_invoice_and_charges_the_net_amount() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(APRIL_14);

            billing.changePlan(id, Plan.BUSINESS);

            assertThat(subscription(id).plan()).isEqualTo(Plan.BUSINESS);
            Invoice invoice = invoices.findBySubscription(id).getFirst();
            assertThat(invoice.issuedOn()).isEqualTo(APRIL_14);
            assertThat(invoice.lines()).containsExactly(
                    new InvoiceLine("Unused time on TEAM (17 of 30 days)", new BigDecimal("-16.43")),
                    new InvoiceLine("Remaining time on BUSINESS (17 of 30 days)", new BigDecimal("44.77")));
            assertThat(payments.charges()).containsExactly(new Charge(ANNA, new BigDecimal("28.34"), invoice.id()));
        }

        @Test
        void a_declined_upgrade_charge_puts_the_subscription_past_due_but_keeps_the_new_plan() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(APRIL_14);
            payments.declineEverything();

            billing.changePlan(id, Plan.BUSINESS);

            assertThat(subscription(id).status()).isEqualTo(SubscriptionStatus.PAST_DUE);
            assertThat(subscription(id).plan()).isEqualTo(Plan.BUSINESS);
        }

        @Test
        void a_downgrade_creates_no_invoice_and_no_charge() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.BUSINESS);

            billing.changePlan(id, Plan.TEAM);

            assertThat(invoices.findBySubscription(id)).isEmpty();
            assertThat(payments.charges()).isEmpty();
            assertThat(subscription(id).scheduledPlan()).contains(Plan.TEAM);
        }

        @Test
        void rejects_an_unknown_subscription() {
            assertThatThrownBy(() -> billing.changePlan(new SubscriptionId("SUB-9999"), Plan.TEAM))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class DailyBilling {

        @Test
        void renews_a_due_subscription_and_bills_the_new_period() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(MAY_1);

            BillingRunReport report = billing.runDailyBilling();

            assertThat(report).isEqualTo(new BillingRunReport(1, 0, 1, 0, 0));
            Invoice invoice = invoices.findBySubscription(id).getFirst();
            assertThat(invoice.issuedOn()).isEqualTo(MAY_1);
            assertThat(invoice.lines()).containsExactly(
                    new InvoiceLine("TEAM plan, 2026-05-01 to 2026-05-31", new BigDecimal("29.00")));
            assertThat(payments.charges()).containsExactly(new Charge(ANNA, new BigDecimal("29.00"), invoice.id()));
            assertThat(subscription(id).currentPeriod()).isEqualTo(new BillingPeriod(MAY_1, JUNE_1));
        }

        @Test
        void does_nothing_before_the_period_ends() {
            billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(LocalDate.of(2026, 4, 30));

            assertThat(billing.runDailyBilling()).isEqualTo(NOTHING_HAPPENED);
            assertThat(payments.charges()).isEmpty();
        }

        @Test
        void re_running_the_same_day_does_not_charge_twice() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(MAY_1);
            billing.runDailyBilling();

            BillingRunReport second = billing.runDailyBilling();

            assertThat(second).isEqualTo(NOTHING_HAPPENED);
            assertThat(payments.charges()).hasSize(1);
            assertThat(invoices.findBySubscription(id)).hasSize(1);
        }

        @Test
        void after_a_crash_the_invoice_is_neither_created_nor_charged_again() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(MAY_1);
            // The job saved the May invoice, then crashed before it saved the subscription.
            invoices.save(new Invoice(
                    new InvoiceId("INV-CRASHED"), id, ANNA, MAY_1,
                    List.of(new InvoiceLine("TEAM plan, 2026-05-01 to 2026-05-31", new BigDecimal("29.00")))));

            BillingRunReport report = billing.runDailyBilling();

            assertThat(report).isEqualTo(new BillingRunReport(1, 0, 0, 0, 1));
            assertThat(payments.charges()).isEmpty();
            assertThat(invoices.findBySubscription(id)).hasSize(1);
            assertThat(subscription(id).currentPeriod().start()).isEqualTo(MAY_1);
        }

        @Test
        void a_free_subscription_renews_without_an_invoice() {
            billing.subscribe(ANNA, Plan.FREE);
            clock.moveTo(MAY_1);

            assertThat(billing.runDailyBilling()).isEqualTo(new BillingRunReport(1, 0, 0, 0, 0));
            assertThat(payments.charges()).isEmpty();
        }

        @Test
        void a_declined_renewal_marks_the_subscription_past_due_and_is_counted() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(MAY_1);
            payments.declineEverything();

            assertThat(billing.runDailyBilling()).isEqualTo(new BillingRunReport(1, 0, 1, 1, 0));
            assertThat(subscription(id).status()).isEqualTo(SubscriptionStatus.PAST_DUE);
        }

        @Test
        void a_subscription_two_periods_behind_gets_two_invoices() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(JUNE_1);

            assertThat(billing.runDailyBilling()).isEqualTo(new BillingRunReport(2, 0, 2, 0, 0));
            assertThat(invoices.findBySubscription(id)).extracting(Invoice::issuedOn).containsExactly(MAY_1, JUNE_1);
            assertThat(subscription(id).currentPeriod().start()).isEqualTo(JUNE_1);
        }

        @Test
        void a_cancelled_subscription_ends_without_an_invoice_and_is_not_due_again() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            billing.cancel(id);
            clock.moveTo(MAY_1);

            assertThat(billing.runDailyBilling()).isEqualTo(new BillingRunReport(0, 1, 0, 0, 0));
            assertThat(subscription(id).status()).isEqualTo(SubscriptionStatus.CANCELLED);
            assertThat(invoices.findBySubscription(id)).isEmpty();
            assertThat(billing.runDailyBilling()).isEqualTo(NOTHING_HAPPENED);
        }
    }

    @Nested
    class Dunning {

        @Test
        void a_paid_renewal_reactivates_a_past_due_subscription() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(MAY_1);
            payments.declineEverything();
            billing.runDailyBilling();
            assertThat(subscription(id).status()).isEqualTo(SubscriptionStatus.PAST_DUE);

            payments.acceptEverything();
            clock.moveTo(JUNE_1);
            billing.runDailyBilling();

            assertThat(subscription(id).status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(publisher.published()).anySatisfy(event ->
                    assertThat(event).isInstanceOf(SubscriptionReactivated.class));
        }

        @Test
        void a_second_declined_renewal_after_14_days_suspends() {
            SubscriptionId id = billing.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(MAY_1);
            payments.declineEverything();
            billing.runDailyBilling();

            clock.moveTo(JUNE_1);
            billing.runDailyBilling();

            assertThat(subscription(id).status()).isEqualTo(SubscriptionStatus.SUSPENDED);
        }
    }

    @Nested
    class Publishing {

        @Test
        void publishes_renewal_events_only_after_the_subscription_is_saved() {
            BillingService logged = serviceWith(new LoggingSubscriptions(subscriptions, log));
            logged.subscribe(ANNA, Plan.TEAM);
            clock.moveTo(MAY_1);
            log.clear();

            logged.runDailyBilling();

            assertThat(log).containsExactly("saved", "published SubscriptionRenewed");
        }

        @Test
        void publishes_the_start_of_a_subscription() {
            billing.subscribe(ANNA, Plan.TEAM);

            assertThat(publisher.published()).singleElement().isInstanceOf(SubscriptionStarted.class);
        }
    }
}
