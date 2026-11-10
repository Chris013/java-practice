package io.github.chris013.practice.task39;

/** Given. Another team's display helper: anything with an id can be shown as "Sensor id". */
public interface Labelled {

    String id();

    default String label() {
        return "Sensor " + id();
    }
}
