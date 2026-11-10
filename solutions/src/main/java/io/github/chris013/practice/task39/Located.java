package io.github.chris013.practice.task39;

/** Given. Another team's floor-plan helper: anything in a room can be shown by its room name. */
public interface Located {

    String room();

    default String label() {
        return room();
    }
}
