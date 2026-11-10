package io.github.chris013.practice.task30;

import java.util.List;
import java.util.Objects;

/** A JSON array. The elements are copied, so later changes to the list we were given do not leak in. */
public record JsonArray(List<JsonValue> elements) implements JsonValue {

    public JsonArray {
        elements = List.copyOf(Objects.requireNonNull(elements, "elements"));
    }
}
