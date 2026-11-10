package io.github.chris013.practice.task30;

import java.util.Objects;

/** A JSON string. The value is the text as it should appear, not yet escaped. */
public record JsonString(String value) implements JsonValue {

    public JsonString {
        Objects.requireNonNull(value, "value");
    }
}
