package io.github.chris013.practice.task30;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.SequencedMap;

/**
 * A JSON object. Members keep their insertion order, because a reader of a log line expects the fields
 * in the order the code wrote them. JSON itself does not promise an order, but stable output is easier to diff.
 */
public record JsonObject(SequencedMap<String, JsonValue> members) implements JsonValue {

    public JsonObject {
        Objects.requireNonNull(members, "members");
        var copy = new LinkedHashMap<String, JsonValue>();
        members.forEach((key, value) -> copy.put(
                Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value")));
        members = Collections.unmodifiableSequencedMap(copy);
    }
}
