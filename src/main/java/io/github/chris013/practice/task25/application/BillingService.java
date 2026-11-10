package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.CustomerId;
import io.github.chris013.practice.task25.domain.Plan;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import java.time.Clock;

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
 *
 * <p>Task 25: read {@code tasks/25-capstone-subscription-billing.md} first.
 */
public final class BillingService {

    public BillingService(
            SubscriptionRepository subscriptions,
            InvoiceRepository invoices,
            PaymentProvider payments,
            EventPublisher eventPublisher,
            SubscriptionIds subscriptionIds,
            InvoiceIds invoiceIds,
            Clock clock) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /** Creates a subscription that starts today. The first period is not billed here (see the task text). */
    public SubscriptionId subscribe(CustomerId customerId, Plan plan) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /** An upgrade is billed at once, with proration. A downgrade is only scheduled. */
    public void changePlan(SubscriptionId id, Plan newPlan) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /** Cancellation takes effect at the end of the current period. */
    public void cancel(SubscriptionId id) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /**
     * The nightly job. Safe to run again after a crash: a renewal whose invoice already exists is skipped, so
     * nobody is charged twice. An invoice that was saved but never charged is skipped too and stays unpaid; the
     * task's key decisions explain this gap. A subscription that is several periods behind gets one invoice per
     * period.
     */
    public BillingRunReport runDailyBilling() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }
}
