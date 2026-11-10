package io.github.chris013.practice.task32;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** An in-memory directory that counts how often it was asked. */
final class FakePickupPointDirectory implements PickupPointDirectory {

    private final Map<String, PickupPoint> pickupPoints = new HashMap<>();
    private int lookups;

    FakePickupPointDirectory with(PickupPoint pickupPoint) {
        pickupPoints.put(pickupPoint.id(), pickupPoint);
        return this;
    }

    int lookups() {
        return lookups;
    }

    @Override
    public Optional<PickupPoint> find(String id) {
        lookups++;
        return Optional.ofNullable(pickupPoints.get(id));
    }
}
