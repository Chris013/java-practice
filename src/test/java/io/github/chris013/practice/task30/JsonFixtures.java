package io.github.chris013.practice.task30;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;

/** Short builders so that the tests read like the JSON they expect. */
final class JsonFixtures {

    private JsonFixtures() {}

    static JsonNumber num(String text) {
        return new JsonNumber(new BigDecimal(text));
    }

    static JsonString str(String text) {
        return new JsonString(text);
    }

    static JsonArray array(JsonValue... elements) {
        return new JsonArray(Arrays.asList(elements));
    }

    /** Alternating keys and values, in the order given: {@code object("a", num("1"), "b", JsonNull.INSTANCE)}. */
    static JsonObject object(Object... keysAndValues) {
        var members = new LinkedHashMap<String, JsonValue>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            members.put((String) keysAndValues[i], (JsonValue) keysAndValues[i + 1]);
        }
        return new JsonObject(members);
    }
}
