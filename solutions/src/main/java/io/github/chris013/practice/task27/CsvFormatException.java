package io.github.chris013.practice.task27;

import java.io.Serial;

/**
 * The file as a whole cannot be imported: no header, or a required column is missing or named twice.
 * This is thrown instead of returning a report, because no row of such a file can be trusted.
 */
public final class CsvFormatException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CsvFormatException(String message) {
        super(message);
    }
}
