package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.SubscriptionId;

/** Given. Port: hands out new subscription ids. */
public interface SubscriptionIds {

    SubscriptionId next();
}
