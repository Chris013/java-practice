package io.github.chris013.practice.task27;

import java.util.List;

/**
 * The result of an import. {@code aborted} means the importer stopped early because the file had too many
 * errors; the lines after the abort were never read. The error list is copied, so the report cannot change.
 */
public record ImportReport(int rowsRead, int imported, List<RowError> errors, boolean aborted) {

    public ImportReport {
        errors = List.copyOf(errors);
    }

    /** True when every row was valid and the import ran to the end. */
    public boolean isClean() {
        return errors.isEmpty() && !aborted;
    }
}
