package io.github.chris013.practice.task25.adapter.inmemory;

import io.github.chris013.practice.task25.application.SubscriptionIds;
import io.github.chris013.practice.task25.domain.SubscriptionId;

/**
 * Produces SUB-0001, SUB-0002, and so on.
 *
 * <p>Task 25: read {@code tasks/25-capstone-subscription-billing.md} first.
 */
public final class SequentialSubscriptionIds implements SubscriptionIds {

    @Override
    public SubscriptionId next() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }
}
