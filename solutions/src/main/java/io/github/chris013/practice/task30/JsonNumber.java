package io.github.chris013.practice.task30;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A JSON number. We keep the {@link BigDecimal} so that no precision is lost on the way to the log archive.
 */
public record JsonNumber(BigDecimal value) implements JsonValue {

    public JsonNumber {
        Objects.requireNonNull(value, "value");
    }
}
