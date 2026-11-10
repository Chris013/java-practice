package io.github.chris013.practice.task27;

import java.io.Reader;
import java.nio.file.Path;

/**
 * Imports a supplier price list (semicolon-separated CSV) into a {@link ProductRepository}.
 *
 * <p>The importer streams: it reads one line at a time and never holds the whole file in memory. It collects
 * every row error with its physical line number instead of stopping at the first one. It gives up early when
 * the number of errors exceeds {@code maxErrors}, because a file with that many errors is probably garbage.
 *
 * <p>Task 27: read {@code tasks/27-csv-import-report.md} first.
 */
public final class CatalogImporter {

    /**
     * @param batchSize how many valid products go to the repository in one call; at least 1
     * @param maxErrors how many row errors are tolerated; the import aborts on the error after this many; at least 0
     */
    public CatalogImporter(ProductRepository repository, int batchSize, int maxErrors) {
        throw new UnsupportedOperationException("TODO: Task 27");
    }

    /** The same as the public constructor, but with a seam for the file opener, so tests can observe closing. */
    CatalogImporter(ProductRepository repository, int batchSize, int maxErrors, FileOpener opener) {
        throw new UnsupportedOperationException("TODO: Task 27");
    }

    /**
     * Imports from a reader that the caller owns. This method reads it but must NOT close it.
     *
     * @throws CsvFormatException if the header is missing or a required column is missing or duplicated
     * @throws java.io.UncheckedIOException if reading fails
     */
    public ImportReport importFrom(Reader reader) {
        throw new UnsupportedOperationException("TODO: Task 27");
    }

    /**
     * Imports a UTF-8 file. The file is always closed, also when the repository throws.
     *
     * @throws CsvFormatException if the header is missing or a required column is missing or duplicated
     * @throws java.io.UncheckedIOException if opening, reading or closing the file fails
     */
    public ImportReport importFrom(Path file) {
        throw new UnsupportedOperationException("TODO: Task 27");
    }
}
