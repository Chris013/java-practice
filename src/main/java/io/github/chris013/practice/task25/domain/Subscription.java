package io.github.chris013.practice.task25.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * A customer's subscription, and the aggregate root for every change to its plan, period and status.
 *
 * <p>The billing service only orchestrates: it loads, calls a method here, saves, charges and publishes. The rules
 * live here because they are facts about the subscription. Whoever calls, the same upgrade is allowed or refused
 * and a renewal moves the same way. Money is a consequence: this class returns invoice lines, and the service turns
 * them into an {@link Invoice}.
 *
 * <p>Dates arrive as parameters. The aggregate never reads the system clock, so the caller decides what "today" is.
 *
 * <p>Task 25: read {@code tasks/25-capstone-subscription-billing.md} first.
 */
public final class Subscription {

    private Subscription() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /** Starts an active subscription whose first period begins today. Any plan, including FREE, is allowed. */
    public static Subscription start(SubscriptionId id, CustomerId customerId, Plan plan, LocalDate today) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /**
     * Upgrades take effect now, with proration. Downgrades wait until the period ends.
     *
     * @return the proration lines for an upgrade, or an empty list for a downgrade
     */
    public List<InvoiceLine> changePlan(Plan newPlan, LocalDate today) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /**
     * Stops the subscription at the end of the current period. Calling it again changes nothing.
     * Cancelling is a promise about the future, so it does not refund or change the current period.
     */
    public void cancelAtPeriodEnd(LocalDate today) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /** Undoes a cancellation that has not taken effect yet. */
    public void resume() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /**
     * Ends the current period and starts the next one. Renews one period per call. A caller that is several
     * periods behind calls again until {@link #currentPeriod()} reaches today.
     */
    public RenewalOutcome renew(LocalDate today) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /**
     * The first failure makes the subscription PAST_DUE. Once 14 days have passed since that first failure, a
     * further failure suspends it. Later failures do not move the first failure date.
     */
    public void paymentFailed(LocalDate today) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /** A successful payment clears the failure and returns the subscription to ACTIVE. */
    public void paymentSucceeded() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    /** Returns the events recorded since the last call, and forgets them. */
    public List<SubscriptionEvent> pullEvents() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    public SubscriptionId id() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    public CustomerId customerId() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    public Plan plan() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    public SubscriptionStatus status() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    public BillingPeriod currentPeriod() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    public Optional<Plan> scheduledPlan() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    public boolean cancelAtPeriodEnd() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }
}
