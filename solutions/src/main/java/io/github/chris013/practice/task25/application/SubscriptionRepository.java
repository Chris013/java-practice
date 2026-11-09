package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.Subscription;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Given. Port: where subscriptions are stored. */
public interface SubscriptionRepository {

    Optional<Subscription> findById(SubscriptionId id);

    /** Subscriptions that are not cancelled and whose current period ended on or before {@code today}. */
    List<Subscription> findDueForRenewal(LocalDate today);

    void save(Subscription subscription);
}
