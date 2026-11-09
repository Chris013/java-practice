package io.github.chris013.practice.task25.domain;

/** Given. Where a subscription is in its lifecycle. Only {@link Subscription} moves it from one status to another. */
public enum SubscriptionStatus {

    /** Paying normally. */
    ACTIVE,
    /** A payment failed. The grace period is running. */
    PAST_DUE,
    /** The grace period ran out. The customer has lost access. */
    SUSPENDED,
    /** Ended at the end of a period. Nothing is billed any more. */
    CANCELLED
}
