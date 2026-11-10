package io.github.chris013.practice.task27;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The column names of the first line, with the position of each one. Column names are case-sensitive and may
 * come in any order. Columns that are not required are allowed and ignored.
 */
final class CsvHeader {

    private static final List<String> REQUIRED = List.of("sku", "name", "price", "stock");

    private final Map<String, Integer> positions;
    private final int columnCount;

    private CsvHeader(Map<String, Integer> positions, int columnCount) {
        this.positions = positions;
        this.columnCount = columnCount;
    }

    /**
     * Checks the header once, up front, so that every row can rely on it. A problem here means the whole file
     * is wrong, so this throws instead of collecting row errors.
     */
    static CsvHeader of(List<String> names) {
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            if (positions.putIfAbsent(names.get(i), i) != null) {
                throw new CsvFormatException("duplicate column: " + names.get(i));
            }
        }
        for (String column : REQUIRED) {
            if (!positions.containsKey(column)) {
                throw new CsvFormatException("missing required column: " + column);
            }
        }
        return new CsvHeader(positions, names.size());
    }

    /** The number of columns in the header, including the ignored ones. Every row must have this many fields. */
    int columnCount() {
        return columnCount;
    }

    /** The field of a required column. Only call this for a row that already has {@link #columnCount()} fields. */
    String value(List<String> fields, String column) {
        return fields.get(positions.get(column));
    }
}
