package io.github.chris013.practice.task27;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    private final ProductRepository repository;
    private final int batchSize;
    private final int maxErrors;
    private final FileOpener opener;

    /**
     * @param batchSize how many valid products go to the repository in one call; at least 1
     * @param maxErrors how many row errors are tolerated; the import aborts on the error after this many; at least 0
     */
    public CatalogImporter(ProductRepository repository, int batchSize, int maxErrors) {
        this(repository, batchSize, maxErrors, file -> Files.newBufferedReader(file, StandardCharsets.UTF_8));
    }

    /** The same as the public constructor, but with a seam for the file opener, so tests can observe closing. */
    CatalogImporter(ProductRepository repository, int batchSize, int maxErrors, FileOpener opener) {
        this.repository = Objects.requireNonNull(repository, "repository");
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1, was " + batchSize);
        }
        if (maxErrors < 0) {
            throw new IllegalArgumentException("maxErrors must not be negative, was " + maxErrors);
        }
        this.batchSize = batchSize;
        this.maxErrors = maxErrors;
        this.opener = Objects.requireNonNull(opener, "opener");
    }

    /**
     * Imports from a reader that the caller owns. This method reads it but must NOT close it.
     *
     * @throws CsvFormatException if the header is missing or a required column is missing or duplicated
     * @throws UncheckedIOException if reading fails
     */
    public ImportReport importFrom(Reader reader) {
        Objects.requireNonNull(reader, "reader");
        try {
            // No try-with-resources here: closing the reader is the caller's job, not ours.
            return run(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Imports a UTF-8 file. The file is always closed, also when the repository throws.
     *
     * @throws CsvFormatException if the header is missing or a required column is missing or duplicated
     * @throws UncheckedIOException if opening, reading or closing the file fails
     */
    public ImportReport importFrom(Path file) {
        Objects.requireNonNull(file, "file");
        // try-with-resources closes the reader on every path. If the body throws and close() throws too,
        // the close exception is added to the body's exception as a suppressed one, not lost.
        try (Reader reader = opener.open(file)) {
            return run(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ImportReport run(Reader source) throws IOException {
        BufferedReader in = new BufferedReader(source);
        CsvHeader header = readHeader(in);
        RowValidator validator = new RowValidator();
        BatchWriter batches = new BatchWriter(repository, batchSize);
        List<RowError> errors = new ArrayList<>();
        int lineNumber = 1; // the header was line 1
        int rowsRead = 0;
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            lineNumber++;
            if (line.isBlank()) {
                continue; // skipped, not counted, but the line number above still moves on
            }
            rowsRead++;
            RowOutcome outcome = parseRow(lineNumber, line, header, validator);
            switch (outcome) {
                case RowOutcome.Valid valid -> batches.add(valid.product());
                case RowOutcome.Invalid invalid -> {
                    errors.addAll(invalid.errors());
                    if (errors.size() > maxErrors) {
                        // Stop now. The pending batch stays unsaved: a file this bad should not be half-imported
                        // without a decision by a person. Batches that were already saved stay saved.
                        return new ImportReport(rowsRead, batches.saved(), errors, true);
                    }
                }
            }
        }
        batches.flush();
        return new ImportReport(rowsRead, batches.saved(), errors, false);
    }

    private static CsvHeader readHeader(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) {
            throw new CsvFormatException("the file is empty: it has no header line");
        }
        if (line.isBlank()) {
            throw new CsvFormatException("the header line is blank");
        }
        Optional<List<String>> names = CsvParser.split(line);
        if (names.isEmpty()) {
            throw new CsvFormatException("the header line has malformed quotes");
        }
        return CsvHeader.of(names.get());
    }

    private static RowOutcome parseRow(int lineNumber, String line, CsvHeader header, RowValidator validator) {
        Optional<List<String>> fields = CsvParser.split(line);
        if (fields.isEmpty()) {
            return new RowOutcome.Invalid(List.of(new RowError(lineNumber, "line", "malformed quotes")));
        }
        List<String> values = fields.get();
        if (values.size() != header.columnCount()) {
            String message = "expected %d fields but found %d".formatted(header.columnCount(), values.size());
            return new RowOutcome.Invalid(List.of(new RowError(lineNumber, "line", message)));
        }
        return validator.validate(
                lineNumber,
                header.value(values, "sku"),
                header.value(values, "name"),
                header.value(values, "price"),
                header.value(values, "stock"));
    }
}
