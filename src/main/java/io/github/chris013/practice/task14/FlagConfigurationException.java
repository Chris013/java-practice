package io.github.chris013.practice.task14;

import java.io.Serial;

/**
 * The configuration text is invalid. The message always starts with {@code line <n>: } so that the person
 * fixing the file knows where to look.
 */
public final class FlagConfigurationException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    FlagConfigurationException(int lineNumber, String detail) {
        throw new UnsupportedOperationException("TODO: Task 14");
    }

    /** The 1-based line in the configuration text where the problem was found. */
    public int lineNumber() {
        throw new UnsupportedOperationException("TODO: Task 14");
    }
}
