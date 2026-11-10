package io.github.chris013.practice.task27;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Splits one physical line into fields. The rules are small but exact, so a tiny state machine reads better
 * than a regular expression: a quote only means something as the first character of a field, a quoted field
 * ends at a quote that is not doubled, and nothing may follow a closing quote except a separator.
 *
 * <p>Lines are never joined. Quoted fields cannot span lines in this importer (see the task for the trade-off).
 */
final class CsvParser {

    private static final char SEPARATOR = ';';
    private static final char QUOTE = '"';

    private enum State {
        /** At the start of a field, nothing read yet. */
        FIELD_START,
        /** Inside an unquoted field. */
        UNQUOTED,
        /** Inside a quoted field. */
        IN_QUOTES,
        /** Just read a quote inside a quoted field: either the closing quote or the first half of {@code ""}. */
        AFTER_QUOTE
    }

    private CsvParser() {
    }

    /**
     * Returns the fields of {@code line}, or an empty Optional when the quotes are malformed. Unquoted fields
     * are trimmed. Quoted fields are returned exactly as written between their quotes.
     */
    static Optional<List<String>> split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        State state = State.FIELD_START;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (state == State.IN_QUOTES) {
                // Separators and everything else are literal inside quotes.
                if (c == QUOTE) {
                    state = State.AFTER_QUOTE;
                } else {
                    value.append(c);
                }
            } else if (c == SEPARATOR) {
                fields.add(finishField(value, state));
                value.setLength(0);
                state = State.FIELD_START;
            } else if (state == State.AFTER_QUOTE) {
                if (c != QUOTE) {
                    return Optional.empty(); // text after a closing quote
                }
                value.append(QUOTE); // "" inside quotes is one quote character
                state = State.IN_QUOTES;
            } else if (c == QUOTE) {
                if (state == State.UNQUOTED) {
                    return Optional.empty(); // a quote in the middle of an unquoted field
                }
                state = State.IN_QUOTES; // an opening quote, as the first character of the field
            } else {
                value.append(c);
                state = State.UNQUOTED;
            }
        }
        if (state == State.IN_QUOTES) {
            return Optional.empty(); // the line ended before the closing quote
        }
        fields.add(finishField(value, state));
        return Optional.of(fields);
    }

    private static String finishField(StringBuilder value, State state) {
        return state == State.AFTER_QUOTE ? value.toString() : value.toString().strip();
    }
}
