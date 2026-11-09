package io.github.chris013.practice.task25.domain;

import java.time.LocalDate;

/**
 * Given. Things that happened to a subscription. The aggregate records them; the service publishes them
 * after the subscription has been saved.
 */
public sealed interface SubscriptionEvent {

    SubscriptionId subscriptionId();

    record SubscriptionStarted(SubscriptionId subscriptionId, CustomerId customerId, Plan plan, LocalDate startedOn)
            implements SubscriptionEvent {}

    record PlanUpgraded(SubscriptionId subscriptionId, Plan from, Plan to, LocalDate effectiveOn)
            implements SubscriptionEvent {}

    record PlanChangeScheduled(SubscriptionId subscriptionId, Plan from, Plan to, LocalDate effectiveOn)
            implements SubscriptionEvent {}

    record SubscriptionRenewed(SubscriptionId subscriptionId, Plan plan, BillingPeriod period)
            implements SubscriptionEvent {}

    record SubscriptionEnded(SubscriptionId subscriptionId, LocalDate endedOn) implements SubscriptionEvent {}

    record SubscriptionPastDue(SubscriptionId subscriptionId, LocalDate failedOn) implements SubscriptionEvent {}

    record SubscriptionSuspended(SubscriptionId subscriptionId, LocalDate suspendedOn) implements SubscriptionEvent {}

    record SubscriptionReactivated(SubscriptionId subscriptionId) implements SubscriptionEvent {}
}
