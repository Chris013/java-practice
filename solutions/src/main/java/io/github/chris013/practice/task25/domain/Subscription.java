package io.github.chris013.practice.task25.domain;

import static io.github.chris013.practice.task25.domain.SubscriptionStatus.ACTIVE;
import static io.github.chris013.practice.task25.domain.SubscriptionStatus.CANCELLED;
import static io.github.chris013.practice.task25.domain.SubscriptionStatus.PAST_DUE;
import static io.github.chris013.practice.task25.domain.SubscriptionStatus.SUSPENDED;

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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
 */
public final class Subscription {

    /** Days after the first failed payment before the subscription is suspended. */
    static final int GRACE_PERIOD_DAYS = 14;

    private final SubscriptionId id;
    private final CustomerId customerId;
    private final List<SubscriptionEvent> events = new ArrayList<>();
    private Plan plan;
    private SubscriptionStatus status;
    private BillingPeriod currentPeriod;
    private Plan scheduledPlan;
    private boolean cancelAtPeriodEnd;
    private LocalDate firstFailedPaymentOn;

    private Subscription(SubscriptionId id, CustomerId customerId, Plan plan, BillingPeriod currentPeriod) {
        this.id = id;
        this.customerId = customerId;
        this.plan = plan;
        this.currentPeriod = currentPeriod;
        this.status = ACTIVE;
    }

    /** Starts an active subscription whose first period begins today. Any plan, including FREE, is allowed. */
    public static Subscription start(SubscriptionId id, CustomerId customerId, Plan plan, LocalDate today) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(today, "today");
        Subscription subscription = new Subscription(id, customerId, plan, BillingPeriod.monthStartingOn(today));
        subscription.events.add(new SubscriptionStarted(id, customerId, plan, today));
        return subscription;
    }

    /**
     * Upgrades take effect now, with proration. Downgrades wait until the period ends.
     *
     * @return the proration lines for an upgrade, or an empty list for a downgrade
     */
    public List<InvoiceLine> changePlan(Plan newPlan, LocalDate today) {
        Objects.requireNonNull(newPlan, "newPlan");
        Objects.requireNonNull(today, "today");
        if (newPlan == plan) {
            throw new IllegalArgumentException("The subscription is already on %s".formatted(plan));
        }
        requireStatus(ACTIVE, "change the plan");
        requireInCurrentPeriod(today, "change the plan");
        if (newPlan.isUpgradeFrom(plan)) {
            return upgradeTo(newPlan, today);
        }
        scheduledPlan = newPlan;
        events.add(new PlanChangeScheduled(id, plan, newPlan, currentPeriod.endExclusive()));
        return List.of();
    }

    /**
     * Stops the subscription at the end of the current period. Calling it again changes nothing.
     * Cancelling is a promise about the future, so it does not refund or change the current period.
     */
    public void cancelAtPeriodEnd(LocalDate today) {
        Objects.requireNonNull(today, "today");
        if (status != ACTIVE && status != PAST_DUE) {
            throw new IllegalStateException("A %s subscription cannot be cancelled".formatted(status));
        }
        requireInCurrentPeriod(today, "cancel");
        cancelAtPeriodEnd = true;
    }

    /** Undoes a cancellation that has not taken effect yet. */
    public void resume() {
        if (status == CANCELLED) {
            throw new IllegalStateException("A cancelled subscription cannot be resumed");
        }
        cancelAtPeriodEnd = false;
    }

    /**
     * Ends the current period and starts the next one. Renews one period per call. A caller that is several
     * periods behind calls again until {@link #currentPeriod()} reaches today.
     */
    public RenewalOutcome renew(LocalDate today) {
        Objects.requireNonNull(today, "today");
        if (status == CANCELLED) {
            throw new IllegalStateException("A cancelled subscription cannot be renewed");
        }
        if (today.isBefore(currentPeriod.endExclusive())) {
            throw new IllegalStateException(
                    "The period runs until %s, so renewal is not due on %s".formatted(currentPeriod.endExclusive(), today));
        }
        if (cancelAtPeriodEnd) {
            status = CANCELLED;
            events.add(new SubscriptionEnded(id, today));
            return new Ended();
        }
        if (scheduledPlan != null) {
            plan = scheduledPlan;
            scheduledPlan = null;
        }
        currentPeriod = currentPeriod.next();
        events.add(new SubscriptionRenewed(id, plan, currentPeriod));
        return new Renewed(linesFor(plan, currentPeriod));
    }

    /**
     * The first failure makes the subscription PAST_DUE. Once {@link #GRACE_PERIOD_DAYS} have passed since that
     * first failure, a further failure suspends it. Later failures do not move the first failure date.
     */
    public void paymentFailed(LocalDate today) {
        Objects.requireNonNull(today, "today");
        switch (status) {
            case ACTIVE -> {
                status = PAST_DUE;
                firstFailedPaymentOn = today;
                events.add(new SubscriptionPastDue(id, today));
            }
            case PAST_DUE -> {
                if (ChronoUnit.DAYS.between(firstFailedPaymentOn, today) >= GRACE_PERIOD_DAYS) {
                    status = SUSPENDED;
                    events.add(new SubscriptionSuspended(id, today));
                }
            }
            case SUSPENDED -> {
                // Already suspended. Nothing more to do.
            }
            case CANCELLED -> throw new IllegalStateException("A cancelled subscription has no payments to fail");
        }
    }

    /** A successful payment clears the failure and starts a new grace period from zero next time. */
    public void paymentSucceeded() {
        switch (status) {
            case ACTIVE -> {
                // Nothing to recover from.
            }
            case PAST_DUE, SUSPENDED -> {
                status = ACTIVE;
                firstFailedPaymentOn = null;
                events.add(new SubscriptionReactivated(id));
            }
            case CANCELLED -> throw new IllegalStateException("A cancelled subscription has no payments to succeed");
        }
    }

    /** Returns the events recorded since the last call, and forgets them. */
    public List<SubscriptionEvent> pullEvents() {
        List<SubscriptionEvent> recorded = List.copyOf(events);
        events.clear();
        return recorded;
    }

    public SubscriptionId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public Plan plan() {
        return plan;
    }

    public SubscriptionStatus status() {
        return status;
    }

    public BillingPeriod currentPeriod() {
        return currentPeriod;
    }

    public Optional<Plan> scheduledPlan() {
        return Optional.ofNullable(scheduledPlan);
    }

    public boolean cancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    private List<InvoiceLine> upgradeTo(Plan newPlan, LocalDate today) {
        int daysLeft = currentPeriod.daysRemainingFrom(today);
        int periodDays = currentPeriod.lengthInDays();
        List<InvoiceLine> lines = List.of(
                new InvoiceLine(
                        "Unused time on %s (%d of %d days)".formatted(plan, daysLeft, periodDays),
                        prorated(plan, daysLeft, periodDays).negate()),
                new InvoiceLine(
                        "Remaining time on %s (%d of %d days)".formatted(newPlan, daysLeft, periodDays),
                        prorated(newPlan, daysLeft, periodDays)));
        events.add(new PlanUpgraded(id, plan, newPlan, today));
        plan = newPlan;
        scheduledPlan = null;
        return lines;
    }

    /**
     * Multiplies first and divides once, so there is no floating point error. Each line is rounded on its own,
     * half-up to cents, so the lines on an invoice add up to what the customer sees on each line.
     */
    private static BigDecimal prorated(Plan plan, int days, int periodDays) {
        return plan.monthlyPrice()
                .multiply(BigDecimal.valueOf(days))
                .divide(BigDecimal.valueOf(periodDays), 2, RoundingMode.HALF_UP);
    }

    private static List<InvoiceLine> linesFor(Plan plan, BillingPeriod period) {
        if (plan == Plan.FREE) {
            return List.of();
        }
        String description = "%s plan, %s to %s".formatted(plan, period.start(), period.endExclusive().minusDays(1));
        return List.of(new InvoiceLine(description, plan.monthlyPrice()));
    }

    private void requireStatus(SubscriptionStatus required, String action) {
        if (status != required) {
            throw new IllegalStateException("Cannot %s while the subscription is %s".formatted(action, status));
        }
    }

    private void requireInCurrentPeriod(LocalDate today, String action) {
        if (!today.isBefore(currentPeriod.endExclusive())) {
            throw new IllegalStateException(
                    "Cannot %s: the period ended on %s and renewal is due, run renewal first"
                            .formatted(action, currentPeriod.endExclusive()));
        }
    }
}
