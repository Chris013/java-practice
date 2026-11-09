package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.SubscriptionEvent;

/** Given. Port: publishes events to the rest of the system (a message bus, in production). */
public interface EventPublisher {

    void publish(SubscriptionEvent event);
}
