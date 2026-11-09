package io.github.chris013.practice.task17;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A test handler that remembers every event it received, in order. */
final class RecordingHandler<E> implements Consumer<E> {

    private final List<E> received = new ArrayList<>();

    @Override
    public void accept(E event) {
        received.add(event);
    }

    List<E> received() {
        return List.copyOf(received);
    }
}
