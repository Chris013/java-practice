package io.github.chris013.practice.task30;

/**
 * Prints a {@link JsonValue} as JSON text as RFC 8259 describes it.
 *
 * <p>Task 30: read {@code tasks/30-json-writer.md} first.
 */
public final class Json {

    private Json() {}

    /**
     * Compact output with no whitespace, for log lines: {@code {"a":1,"b":[true,null]}}.
     *
     * <p>Strings are escaped as RFC 8259 requires: {@code "}, {@code \} and U+0000 to U+001F only.
     */
    public static String write(JsonValue value) {
        throw new UnsupportedOperationException("TODO: Task 30");
    }

    /**
     * Two-space indentation, one array element or object member per line, {@code "key": value}, empty
     * containers as {@code []} and {@code {}}, and no trailing newline.
     */
    public static String writePretty(JsonValue value) {
        throw new UnsupportedOperationException("TODO: Task 30");
    }
}
