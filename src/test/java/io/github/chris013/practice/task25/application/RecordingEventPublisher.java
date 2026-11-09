package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.SubscriptionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Remembers every published event. It also appends a line to a shared log, so a test can check that
 * events were published after the subscription was saved.
 */
final class RecordingEventPublisher implements EventPublisher {

    private final List<SubscriptionEvent> published = new ArrayList<>();
    private final List<String> log;

    RecordingEventPublisher(List<String> log) {
        this.log = log;
    }

    @Override
    public void publish(SubscriptionEvent event) {
        published.add(event);
        log.add("published " + event.getClass().getSimpleName());
    }

    List<SubscriptionEvent> published() {
        return List.copyOf(published);
    }
}
