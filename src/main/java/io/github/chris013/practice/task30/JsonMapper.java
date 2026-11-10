package io.github.chris013.practice.task30;

/**
 * Turns plain Java objects into a {@link JsonValue} tree.
 *
 * <p>Task 30: read {@code tasks/30-json-writer.md} first.
 */
public final class JsonMapper {

    private JsonMapper() {}

    /**
     * Maps {@code value} to JSON. See the task for the mapping rules and for the exceptions.
     *
     * @throws IllegalArgumentException for NaN or infinite numbers, non-String map keys, unsupported
     *     types and cycles
     * @throws IllegalStateException if a record accessor throws; the exception is the cause
     */
    public static JsonValue toJson(Object value) {
        throw new UnsupportedOperationException("TODO: Task 30");
    }
}
