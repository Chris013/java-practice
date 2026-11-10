package io.github.chris013.practice.task27;

/**
 * One problem found in one line of the file. {@code field} is the column name, or {@code line} when the whole
 * line is wrong (for example, a wrong number of fields). {@code lineNumber} is the physical line, header = 1.
 */
public record RowError(int lineNumber, String field, String message) {}
