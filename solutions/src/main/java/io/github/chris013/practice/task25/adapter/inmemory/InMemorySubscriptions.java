package io.github.chris013.practice.task25.adapter.inmemory;

import io.github.chris013.practice.task25.application.SubscriptionRepository;
import io.github.chris013.practice.task25.domain.Subscription;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import io.github.chris013.practice.task25.domain.SubscriptionStatus;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Keeps subscriptions in a map. It stores the same object that it is given, so it cannot show you a missing
 * {@code save} call. Use it for tests and local runs, not for a production-like check.
 */
public final class InMemorySubscriptions implements SubscriptionRepository {

    private final Map<SubscriptionId, Subscription> byId = new LinkedHashMap<>();

    @Override
    public Optional<Subscription> findById(SubscriptionId id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Subscription> findDueForRenewal(LocalDate today) {
        Objects.requireNonNull(today, "today");
        return byId.values().stream()
                .filter(subscription -> subscription.status() != SubscriptionStatus.CANCELLED)
                .filter(subscription -> !subscription.currentPeriod().endExclusive().isAfter(today))
                .toList();
    }

    @Override
    public void save(Subscription subscription) {
        Objects.requireNonNull(subscription, "subscription");
        byId.put(subscription.id(), subscription);
    }
}
