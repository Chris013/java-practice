package io.github.chris013.practice.task30;

/**
 * A JSON value, modelled as a closed set of cases. The sealed hierarchy lets the printer use an exhaustive
 * {@code switch} with no {@code default}: adding a new kind of value is a compile error until every printer
 * handles it.
 *
 * <p>Task 30: read {@code tasks/30-json-writer.md} first.
 */
public sealed interface JsonValue permits JsonNull, JsonBoolean, JsonNumber, JsonString, JsonArray, JsonObject {}
