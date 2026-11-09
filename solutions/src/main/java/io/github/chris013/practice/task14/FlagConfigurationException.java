package io.github.chris013.practice.task14;

import java.io.Serial;

/**
 * The configuration text is invalid. The message always starts with {@code line <n>: } so that the person
 * fixing the file knows where to look.
 */
public final class FlagConfigurationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int lineNumber;

    FlagConfigurationException(int lineNumber, String detail) {
        super("line " + lineNumber + ": " + detail);
        this.lineNumber = lineNumber;
    }

    /** The 1-based line in the configuration text where the problem was found. */
    public int lineNumber() {
        return lineNumber;
    }
}
