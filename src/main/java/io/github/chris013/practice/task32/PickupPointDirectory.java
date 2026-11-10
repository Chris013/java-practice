package io.github.chris013.practice.task32;

import java.util.Optional;

/** Looks up pickup points by their id. An unknown id is an empty result, not an exception. */
public interface PickupPointDirectory {

    Optional<PickupPoint> find(String id);
}
