package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.CustomerId;
import io.github.chris013.practice.task25.domain.Invoice;
import io.github.chris013.practice.task25.domain.InvoiceLine;
import io.github.chris013.practice.task25.domain.Plan;
import io.github.chris013.practice.task25.domain.RenewalOutcome;
import io.github.chris013.practice.task25.domain.Subscription;
import io.github.chris013.practice.task25.domain.SubscriptionEvent;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The application service: the use cases of the billing module. It loads aggregates, asks them to change,
 * turns their answers into invoices, charges, saves and publishes. It does not decide any billing rule itself.
 *
 * <p>Ordering rules that keep the system safe to re-run:
 * <ul>
 *   <li>an invoice is saved before it is charged, so a crash after the charge cannot create a second charge;</li>
 *   <li>the subscription is saved before its events are published, so no listener reacts to a change that was lost;</li>
 *   <li>a renewal invoice is recognised by {@link InvoiceRepository#existsFor}, so a second run skips it.</li>
 * </ul>
 */
public final class BillingService {

    private final SubscriptionRepository subscriptions;
    private final InvoiceRepository invoices;
    private final PaymentProvider payments;
    private final EventPublisher eventPublisher;
    private final SubscriptionIds subscriptionIds;
    private final InvoiceIds invoiceIds;
    private final Clock clock;

    public BillingService(
            SubscriptionRepository subscriptions,
            InvoiceRepository invoices,
            PaymentProvider payments,
            EventPublisher eventPublisher,
            SubscriptionIds subscriptionIds,
            InvoiceIds invoiceIds,
            Clock clock) {
        this.subscriptions = Objects.requireNonNull(subscriptions, "subscriptions");
        this.invoices = Objects.requireNonNull(invoices, "invoices");
        this.payments = Objects.requireNonNull(payments, "payments");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.subscriptionIds = Objects.requireNonNull(subscriptionIds, "subscriptionIds");
        this.invoiceIds = Objects.requireNonNull(invoiceIds, "invoiceIds");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Creates a subscription that starts today. The first period is not billed here (see the task text). */
    public SubscriptionId subscribe(CustomerId customerId, Plan plan) {
        Subscription subscription = Subscription.start(subscriptionIds.next(), customerId, plan, today());
        subscriptions.save(subscription);
        publishPendingEvents(subscription);
        return subscription.id();
    }

    /** An upgrade is billed at once, with proration. A downgrade is only scheduled. */
    public void changePlan(SubscriptionId id, Plan newPlan) {
        Subscription subscription = load(id);
        LocalDate today = today();
        List<InvoiceLine> lines = subscription.changePlan(newPlan, today);
        if (!lines.isEmpty()) {
            Invoice invoice = newInvoice(subscription, today, lines);
            invoices.save(invoice);
            collect(subscription, invoice, today);
        }
        subscriptions.save(subscription);
        publishPendingEvents(subscription);
    }

    /** Cancellation takes effect at the end of the current period. */
    public void cancel(SubscriptionId id) {
        Subscription subscription = load(id);
        subscription.cancelAtPeriodEnd(today());
        subscriptions.save(subscription);
        publishPendingEvents(subscription);
    }

    /**
     * The nightly job. Safe to run again after a crash: a renewal whose invoice already exists is skipped, so
     * nobody is charged twice. An invoice that was saved but never charged is skipped too and stays unpaid; the
     * task's key decisions explain this gap. A subscription that is several periods behind gets one invoice per
     * period.
     */
    public BillingRunReport runDailyBilling() {
        LocalDate today = today();
        Tally tally = new Tally();
        for (Subscription subscription : subscriptions.findDueForRenewal(today)) {
            renewUntilCurrent(subscription, today, tally);
            subscriptions.save(subscription);
            publishPendingEvents(subscription);
        }
        return tally.toReport();
    }

    private void renewUntilCurrent(Subscription subscription, LocalDate today, Tally tally) {
        while (!subscription.currentPeriod().endExclusive().isAfter(today)) {
            switch (subscription.renew(today)) {
                case RenewalOutcome.Ended _ -> {
                    tally.ended++;
                    return;
                }
                case RenewalOutcome.Renewed renewed -> {
                    tally.renewed++;
                    invoiceRenewal(subscription, renewed, today, tally);
                }
            }
        }
    }

    private void invoiceRenewal(Subscription subscription, RenewalOutcome.Renewed renewal, LocalDate today, Tally tally) {
        if (renewal.lines().isEmpty()) {
            return; // FREE: nothing to bill.
        }
        LocalDate periodStart = subscription.currentPeriod().start();
        if (invoices.existsFor(subscription.id(), periodStart)) {
            tally.skippedAlreadyInvoiced++;
            return;
        }
        Invoice invoice = newInvoice(subscription, periodStart, renewal.lines());
        invoices.save(invoice);
        tally.invoiced++;
        if (collect(subscription, invoice, today)) {
            tally.paymentsFailed++;
        }
    }

    /** Charges the invoice if it asks for money. Returns true if the card was declined. */
    private boolean collect(Subscription subscription, Invoice invoice, LocalDate today) {
        if (invoice.total().signum() <= 0) {
            return false;
        }
        return switch (payments.charge(invoice.customerId(), invoice.total(), invoice.id())) {
            case PaymentResult.Paid _ -> {
                subscription.paymentSucceeded();
                yield false;
            }
            case PaymentResult.Declined _ -> {
                subscription.paymentFailed(today);
                yield true;
            }
        };
    }

    private Invoice newInvoice(Subscription subscription, LocalDate issuedOn, List<InvoiceLine> lines) {
        return new Invoice(invoiceIds.next(), subscription.id(), subscription.customerId(), issuedOn, lines);
    }

    private void publishPendingEvents(Subscription subscription) {
        for (SubscriptionEvent event : subscription.pullEvents()) {
            eventPublisher.publish(event);
        }
    }

    private Subscription load(SubscriptionId id) {
        Objects.requireNonNull(id, "id");
        return subscriptions.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown subscription " + id.value()));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Mutable counters for one run. The caller only ever sees the immutable {@link BillingRunReport}. */
    private static final class Tally {
        private int renewed;
        private int ended;
        private int invoiced;
        private int paymentsFailed;
        private int skippedAlreadyInvoiced;

        BillingRunReport toReport() {
            return new BillingRunReport(renewed, ended, invoiced, paymentsFailed, skippedAlreadyInvoiced);
        }
    }
}
