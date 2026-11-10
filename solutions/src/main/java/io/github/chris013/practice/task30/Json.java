package io.github.chris013.practice.task30;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Prints a {@link JsonValue} as JSON text as RFC 8259 describes it.
 *
 * <p>Escaping is exactly what the RFC requires and nothing more: {@code "}, {@code \} and the control
 * characters U+0000 to U+001F are escaped; {@code /}, U+007F and all non-ASCII characters are written as they
 * are. The output is meant to be written as UTF-8. A string holding an unpaired surrogate is copied unchanged,
 * so the text may then not be valid UTF-8. A stricter writer would reject such a string instead.
 */
public final class Json {

    private static final String HEX_DIGITS = "0123456789abcdef";
    private static final String INDENT = "  ";

    private Json() {}

    /** Compact output with no whitespace, for log lines. */
    public static String write(JsonValue value) {
        var out = new StringBuilder();
        append(Objects.requireNonNull(value, "value"), out, false, 0);
        return out.toString();
    }

    /** Two-space indentation, one array element or object member per line, no trailing newline. */
    public static String writePretty(JsonValue value) {
        var out = new StringBuilder();
        append(Objects.requireNonNull(value, "value"), out, true, 0);
        return out.toString();
    }

    // The exhaustive switch is the reason JsonValue is sealed: no default branch, and the compiler
    // complains when a new case is added and not handled here.
    private static void append(JsonValue value, StringBuilder out, boolean pretty, int depth) {
        switch (value) {
            case JsonNull _ -> out.append("null");
            case JsonBoolean bool -> out.append(bool.value());
            case JsonNumber number -> out.append(number.value().toPlainString());
            case JsonString string -> appendString(string.value(), out);
            case JsonArray array -> appendArray(array.elements(), out, pretty, depth);
            case JsonObject object -> appendObject(object.members(), out, pretty, depth);
        }
    }

    private static void appendArray(List<JsonValue> elements, StringBuilder out, boolean pretty, int depth) {
        if (elements.isEmpty()) {
            out.append("[]");
            return;
        }
        out.append('[');
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            newline(out, pretty, depth + 1);
            append(elements.get(i), out, pretty, depth + 1);
        }
        newline(out, pretty, depth);
        out.append(']');
    }

    private static void appendObject(Map<String, JsonValue> members, StringBuilder out, boolean pretty, int depth) {
        if (members.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append('{');
        boolean first = true;
        for (Map.Entry<String, JsonValue> member : members.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            newline(out, pretty, depth + 1);
            appendString(member.getKey(), out);
            out.append(pretty ? ": " : ":");
            append(member.getValue(), out, pretty, depth + 1);
        }
        newline(out, pretty, depth);
        out.append('}');
    }

    private static void newline(StringBuilder out, boolean pretty, int depth) {
        if (pretty) {
            out.append('\n').append(INDENT.repeat(depth));
        }
    }

    private static void appendString(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        // Every other control character becomes a six-character escape with four lowercase hex digits.
                        out.append("\\u00").append(HEX_DIGITS.charAt(c >> 4)).append(HEX_DIGITS.charAt(c & 0xF));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
