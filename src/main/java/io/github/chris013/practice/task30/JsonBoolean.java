package io.github.chris013.practice.task30;

/** A JSON {@code true} or {@code false}. */
public record JsonBoolean(boolean value) implements JsonValue {

    public static final JsonBoolean TRUE = new JsonBoolean(true);
    public static final JsonBoolean FALSE = new JsonBoolean(false);
}
