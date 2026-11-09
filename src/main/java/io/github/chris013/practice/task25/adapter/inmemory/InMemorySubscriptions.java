package io.github.chris013.practice.task25.adapter.inmemory;

import io.github.chris013.practice.task25.application.SubscriptionRepository;
import io.github.chris013.practice.task25.domain.Subscription;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Keeps subscriptions in a map. It stores the same object that it is given, so it cannot show you a missing
 * {@code save} call. Use it for tests and local runs, not for a production-like check.
 *
 * <p>Task 25: read {@code tasks/25-capstone-subscription-billing.md} first.
 */
public final class InMemorySubscriptions implements SubscriptionRepository {

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    @Override
    public List<Subscription> findDueForRenewal(LocalDate today) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    @Override
    public void save(Subscription subscription) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }
}
