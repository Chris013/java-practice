package io.github.chris013.practice.task25.adapter.inmemory;

import io.github.chris013.practice.task25.application.SubscriptionIds;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import java.util.concurrent.atomic.AtomicInteger;

/** Produces SUB-0001, SUB-0002, and so on. */
public final class SequentialSubscriptionIds implements SubscriptionIds {

    private final AtomicInteger last = new AtomicInteger();

    @Override
    public SubscriptionId next() {
        return new SubscriptionId("SUB-%04d".formatted(last.incrementAndGet()));
    }
}
