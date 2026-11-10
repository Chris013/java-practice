package io.github.chris013.practice.task27;

import static io.github.chris013.practice.task27.CsvLines.HEADER;
import static io.github.chris013.practice.task27.CsvLines.row;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CatalogImporterTest {

    private static final String SKU_RULE = "must be 3 to 20 characters A-Z, 0-9 or -";
    private static final String PRICE_RULE = "must be a positive price like 12,99";
    private static final String STOCK_RULE = "must be a whole number from 0 to 1000000";
    private static final String NAME_RULE = "must not be blank and at most 100 characters";

    private final RecordingRepository repository = new RecordingRepository();

    private CatalogImporter importer(int batchSize, int maxErrors) {
        return new CatalogImporter(repository, batchSize, maxErrors);
    }

    private ImportReport importLines(int batchSize, int maxErrors, String... lines) {
        return importer(batchSize, maxErrors).importFrom(new StringReader(CsvLines.of(lines)));
    }

    @Nested
    class Header {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
            "name;price;stock|sku",
            "sku;price;stock|name",
            "sku;name;stock|price",
            "sku;name;price|stock"})
        void rejects_a_file_without_a_required_column(String header, String missingColumn) {
            assertThatThrownBy(() -> importLines(10, 0, header))
                    .isInstanceOf(CsvFormatException.class)
                    .hasMessageContaining(missingColumn);
        }

        @Test
        void rejects_a_column_that_is_named_twice() {
            assertThatThrownBy(() -> importLines(10, 0, "sku;name;price;stock;sku"))
                    .isInstanceOf(CsvFormatException.class)
                    .hasMessageContaining("duplicate column: sku");
        }

        @Test
        void rejects_an_empty_file() {
            assertThatThrownBy(() -> importer(10, 0).importFrom(new StringReader("")))
                    .isInstanceOf(CsvFormatException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        void accepts_the_required_columns_in_any_order() {
            ImportReport report = importLines(10, 0, "stock;price;name;sku", "5;12,99;Coffee;COF-1");

            assertThat(report.isClean()).isTrue();
            assertThat(repository.allProducts())
                    .containsExactly(new Product("COF-1", "Coffee", new BigDecimal("12.99"), 5));
        }

        @Test
        void ignores_extra_columns() {
            ImportReport report = importLines(10, 0, "sku;name;note;price;stock", "COF-1;Coffee;fresh;12,99;5");

            assertThat(report.isClean()).isTrue();
            assertThat(repository.allProducts())
                    .containsExactly(new Product("COF-1", "Coffee", new BigDecimal("12.99"), 5));
        }
    }

    @Nested
    class ValidRows {

        @Test
        void reads_the_types_and_the_scale_of_every_field() {
            ImportReport report = importLines(10, 0, HEADER, row("COF-1", "Coffee", "5", "7"));

            assertThat(repository.allProducts())
                    .containsExactly(new Product("COF-1", "Coffee", new BigDecimal("5.00"), 7));
            assertThat(report.rowsRead()).isEqualTo(1);
            assertThat(report.imported()).isEqualTo(1);
        }

        @Test
        void saves_valid_rows_in_batches_of_the_batch_size() {
            ImportReport report = importLines(2, 0,
                    HEADER,
                    row("A-1", "One", "1", "1"),
                    row("A-2", "Two", "2", "2"),
                    row("A-3", "Three", "3", "3"),
                    row("A-4", "Four", "4", "4"),
                    row("A-5", "Five", "5", "5"));

            assertThat(repository.batchSizes()).containsExactly(2, 2, 1);
            assertThat(report.rowsRead()).isEqualTo(5);
            assertThat(report.imported()).isEqualTo(5);
            assertThat(report.isClean()).isTrue();
        }

        @Test
        void sends_only_full_batches_when_the_rows_divide_evenly() {
            importLines(2, 0, HEADER,
                    row("A-1", "One", "1", "1"),
                    row("A-2", "Two", "2", "2"),
                    row("A-3", "Three", "3", "3"),
                    row("A-4", "Four", "4", "4"));

            assertThat(repository.batchSizes()).containsExactly(2, 2);
        }

        @Test
        void never_calls_the_repository_when_no_row_is_valid() {
            importLines(10, 10, HEADER, "ab;Tea;1;1");

            assertThat(repository.batchSizes()).isEmpty();
        }

        @Test
        void keeps_semicolons_and_doubled_quotes_inside_a_quoted_field() {
            importLines(10, 0, HEADER, "COF-1;\"Coffee; \"\"dark\"\" roast\";12,99;5");

            assertThat(repository.allProducts()).extracting(Product::name)
                    .containsExactly("Coffee; \"dark\" roast");
        }

        @Test
        void trims_unquoted_fields() {
            importLines(10, 0, HEADER, "  COF-1 ; Coffee ;12,99; 5 ");

            assertThat(repository.allProducts())
                    .containsExactly(new Product("COF-1", "Coffee", new BigDecimal("12.99"), 5));
        }

        @Test
        void keeps_the_spaces_inside_a_quoted_field() {
            importLines(10, 0, HEADER, "COF-1;\" Coffee \";12,99;5");

            assertThat(repository.allProducts()).extracting(Product::name).containsExactly(" Coffee ");
        }
    }

    @Nested
    class RowErrors {

        @ParameterizedTest
        @ValueSource(strings = {"ABC", "ABCDEFGHIJ0123456789", "A-1"})
        void accepts_skus_at_the_length_and_character_boundaries(String sku) {
            ImportReport report = importLines(10, 10, HEADER, row(sku, "Coffee", "1", "1"));

            assertThat(report.isClean()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"AB", "ABCDEFGHIJ01234567890", "abc", "A_B", "A B"})
        void rejects_a_sku_outside_the_rules(String sku) {
            ImportReport report = importLines(10, 10, HEADER, row(sku, "Coffee", "1", "1"));

            assertThat(report.errors()).containsExactly(new RowError(2, "sku", SKU_RULE));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "\"   \""})
        void rejects_a_blank_name(String name) {
            ImportReport report = importLines(10, 10, HEADER, row("COF-1", name, "1", "1"));

            assertThat(report.errors()).containsExactly(new RowError(2, "name", NAME_RULE));
        }

        @Test
        void accepts_a_name_of_exactly_100_characters() {
            ImportReport report = importLines(10, 10, HEADER, row("COF-1", "x".repeat(100), "1", "1"));

            assertThat(report.isClean()).isTrue();
        }

        @Test
        void rejects_a_name_of_101_characters() {
            ImportReport report = importLines(10, 10, HEADER, row("COF-1", "x".repeat(101), "1", "1"));

            assertThat(report.errors()).containsExactly(new RowError(2, "name", NAME_RULE));
        }

        @ParameterizedTest
        @CsvSource(delimiterString = ";", value = {"12,99;12.99", "5;5.00", "0,5;0.50", "12,5;12.50", "0,01;0.01"})
        void reads_german_prices_as_money_with_two_decimals(String price, String expected) {
            importLines(10, 0, HEADER, row("COF-1", "Coffee", price, "1"));

            assertThat(repository.allProducts()).extracting(Product::price)
                    .containsExactly(new BigDecimal(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0,00", "-1", "12,999", "1.000,50", "12,", ",5", "abc", "1 000"})
        void rejects_a_price_that_is_not_a_positive_german_decimal(String price) {
            ImportReport report = importLines(10, 10, HEADER, row("COF-1", "Coffee", price, "1"));

            assertThat(report.errors()).containsExactly(new RowError(2, "price", PRICE_RULE));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "1000000"})
        void accepts_stock_at_both_ends_of_the_range(String stock) {
            ImportReport report = importLines(10, 10, HEADER, row("COF-1", "Coffee", "1", stock));

            assertThat(report.isClean()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"-1", "1000001", "1,5", "1.5", "abc", "99999999999"})
        void rejects_stock_outside_a_whole_number_from_0_to_1000000(String stock) {
            ImportReport report = importLines(10, 10, HEADER, row("COF-1", "Coffee", "1", stock));

            assertThat(report.errors()).containsExactly(new RowError(2, "stock", STOCK_RULE));
        }

        @Test
        void collects_every_field_error_of_a_row_in_column_order() {
            ImportReport report = importLines(10, 10, HEADER, "ab;;0;-1");

            assertThat(report.errors()).extracting(RowError::field).containsExactly("sku", "name", "price", "stock");
            assertThat(report.errors()).extracting(RowError::lineNumber).containsOnly(2);
            assertThat(report.imported()).isZero();
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
            "COF-1;Coffee;12,99|expected 4 fields but found 3",
            "COF-1;Coffee;12,99;5;extra|expected 4 fields but found 5"})
        void reports_a_wrong_number_of_fields_on_the_line(String row, String message) {
            ImportReport report = importLines(10, 10, HEADER, row);

            assertThat(report.errors()).containsExactly(new RowError(2, "line", message));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "COF-1;\"Coffee\"x;12,99;5",
            "COF-1;\"Coffee;12,99;5",
            "COF-1;Cof\"fee;12,99;5",
            "COF-1; \"Coffee\";12,99;5",
            "COF-1;\"Coffee\" ;12,99;5"})
        void reports_malformed_quotes_as_a_line_error(String row) {
            ImportReport report = importLines(10, 10, HEADER, row);

            assertThat(report.errors()).containsExactly(new RowError(2, "line", "malformed quotes"));
        }

        @Test
        void reports_a_duplicate_sku_against_the_first_line_that_had_it() {
            ImportReport report = importLines(10, 10, HEADER,
                    row("COF-1", "Coffee", "12,99", "5"),
                    row("TEA-1", "Tea", "3,50", "5"),
                    row("COF-1", "Other", "1,00", "1"));

            assertThat(report.errors()).containsExactly(new RowError(4, "sku", "duplicate of line 2"));
            assertThat(repository.allProducts()).hasSize(2);
        }

        @Test
        void a_row_that_failed_does_not_register_its_sku() {
            ImportReport report = importLines(10, 10, HEADER,
                    row("COF-1", "Coffee", "0", "5"),
                    row("COF-1", "Coffee", "12,99", "5"));

            // Line 2 is the only error. Line 3 is not a duplicate, because line 2 never registered COF-1.
            assertThat(report.errors()).containsExactly(new RowError(2, "price", PRICE_RULE));
            assertThat(report.imported()).isEqualTo(1);
        }

        @Test
        void skips_blank_lines_but_keeps_the_physical_line_numbers() {
            ImportReport report = importLines(10, 10, HEADER,
                    "",
                    row("COF-1", "Coffee", "12,99", "5"),
                    "   ",
                    "ab;Tea;1;1");

            assertThat(report.rowsRead()).isEqualTo(2);
            assertThat(report.errors()).containsExactly(new RowError(5, "sku", SKU_RULE));
        }
    }

    @Nested
    class Abort {

        @Test
        void stops_at_the_row_that_exceeds_max_errors_and_leaves_the_rest_unread() {
            ImportReport report = importLines(2, 2, HEADER,
                    row("COF-1", "Coffee", "12,99", "5"),
                    row("TEA-1", "Tea", "3,50", "5"),
                    row("MIL-1", "Milk", "1,20", "5"),
                    row("x", "Bad", "1", "1"),
                    row("y", "Bad", "1", "1"),
                    row("z", "Bad", "1", "1"),
                    row("TEA-2", "Tea", "1", "1"));

            assertThat(report.aborted()).isTrue();
            assertThat(report.errors()).extracting(RowError::lineNumber).containsExactly(5, 6, 7);
            assertThat(report.rowsRead()).isEqualTo(6);
            // The first batch was saved. The pending MIL-1 row was not, because the import stopped before a flush.
            assertThat(repository.batchSizes()).containsExactly(2);
            assertThat(report.imported()).isEqualTo(2);
        }

        @Test
        void does_not_abort_while_the_error_count_is_at_the_limit() {
            ImportReport report = importLines(10, 2, HEADER,
                    "x;a;1;1",
                    "y;b;1;1",
                    row("COF-1", "Coffee", "12,99", "5"));

            assertThat(report.aborted()).isFalse();
            assertThat(report.errors()).hasSize(2);
            assertThat(report.imported()).isEqualTo(1);
        }

        @Test
        void aborts_at_the_first_error_when_max_errors_is_zero() {
            ImportReport report = importLines(10, 0, HEADER,
                    "x;a;1;1",
                    row("COF-1", "Coffee", "12,99", "5"));

            assertThat(report.aborted()).isTrue();
            assertThat(report.errors()).hasSize(1);
            assertThat(report.imported()).isZero();
        }

        @Test
        void stops_reading_a_garbage_file_long_before_its_end() {
            String badRow = "x;" + "Coffee".repeat(5) + ";1;1";
            String text = HEADER + "\n" + (badRow + "\n").repeat(10_000);
            CountingReader reader = new CountingReader(text);

            ImportReport report = importer(10, 2).importFrom(reader);

            assertThat(report.aborted()).isTrue();
            assertThat(repository.allProducts()).isEmpty();
            // The importer reads in chunks of a few thousand characters. It must not read the whole 10,000 lines.
            assertThat(reader.charsRead()).isLessThan(text.length() / 10);
        }

        @Test
        void keeps_every_error_of_the_row_that_crosses_the_limit() {
            // Line 2 has one error and fills the limit of 1. Line 3 has four errors, so the total becomes 5.
            // The whole row is added, so the report holds all four of its errors, not just the first.
            ImportReport report = importLines(10, 1, HEADER, "x;a;1;1", "ab;;0;-1", row("COF-1", "Coffee", "1", "1"));

            assertThat(report.aborted()).isTrue();
            assertThat(report.errors()).hasSize(5);
            assertThat(report.rowsRead()).isEqualTo(2);
        }
    }

    @Nested
    class Resources {

        @Test
        void importFrom_a_reader_does_not_close_it() {
            CountingReader reader = new CountingReader(CsvLines.of(HEADER, row("COF-1", "Coffee", "12,99", "5")));

            importer(10, 0).importFrom(reader);

            assertThat(reader.isClosed()).isFalse();
        }

        @Test
        void importFrom_a_path_reads_utf_8_umlauts_correctly(@TempDir Path directory) throws IOException {
            Path file = directory.resolve("prices.csv");
            Files.writeString(file,
                    CsvLines.of(HEADER, row("KAF-1", "Kaffee für Gäste, Größe L", "12,99", "5")),
                    StandardCharsets.UTF_8);

            importer(10, 0).importFrom(file);

            assertThat(repository.allProducts()).extracting(Product::name)
                    .containsExactly("Kaffee für Gäste, Größe L");
        }

        @Test
        void importFrom_a_path_closes_the_file_after_a_normal_import() {
            CountingReader file = new CountingReader(CsvLines.of(HEADER, row("COF-1", "Coffee", "12,99", "5")));

            importerOpening(file).importFrom(Path.of("prices.csv"));

            assertThat(file.isClosed()).isTrue();
        }

        @Test
        void importFrom_a_path_closes_the_file_when_the_repository_fails() {
            repository.failWith(new IllegalStateException("database is down"));
            CountingReader file = new CountingReader(CsvLines.of(HEADER, row("COF-1", "Coffee", "12,99", "5")));

            assertThatThrownBy(() -> importerOpening(file).importFrom(Path.of("prices.csv")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("database is down");
            assertThat(file.isClosed()).isTrue();
        }

        @Test
        void attaches_a_failure_to_close_as_suppressed_to_the_repository_failure() {
            repository.failWith(new IllegalStateException("database is down"));
            CountingReader file = new CountingReader(
                    CsvLines.of(HEADER, row("COF-1", "Coffee", "12,99", "5")), new IOException("close failed"));

            assertThatThrownBy(() -> importerOpening(file).importFrom(Path.of("prices.csv")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("database is down")
                    .satisfies(failure -> assertThat(failure.getSuppressed())
                            .extracting(Throwable::getMessage)
                            .containsExactly("close failed"));
        }

        @Test
        void wraps_an_io_failure_from_opening_the_file() {
            FileOpener failingOpener = file -> {
                throw new IOException("no such file");
            };
            CatalogImporter importer = new CatalogImporter(repository, 10, 0, failingOpener);

            assertThatThrownBy(() -> importer.importFrom(Path.of("missing.csv")))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        void wraps_an_io_failure_while_reading_from_a_reader_or_a_path() {
            Reader broken = new Reader() {
                @Override
                public int read(char[] buffer, int offset, int length) throws IOException {
                    throw new IOException("disk failed");
                }

                @Override
                public void close() {
                    // nothing to close
                }
            };

            assertThatThrownBy(() -> importer(10, 0).importFrom(broken))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasCauseInstanceOf(IOException.class);
            assertThatThrownBy(() -> importerOpening(broken).importFrom(Path.of("prices.csv")))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        void wraps_an_io_failure_from_closing_after_a_good_import() {
            CountingReader file = new CountingReader(
                    CsvLines.of(HEADER, row("COF-1", "Coffee", "12,99", "5")), new IOException("close failed"));

            assertThatThrownBy(() -> importerOpening(file).importFrom(Path.of("prices.csv")))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasCauseInstanceOf(IOException.class);
        }

        @Test
        void importFrom_a_path_closes_the_file_when_the_header_is_wrong() {
            CountingReader file = new CountingReader("name;price;stock\n");

            assertThatThrownBy(() -> importerOpening(file).importFrom(Path.of("prices.csv")))
                    .isInstanceOf(CsvFormatException.class)
                    .hasMessageContaining("sku");
            assertThat(file.isClosed()).isTrue();
        }

        @Test
        void importFrom_a_path_closes_the_file_when_reading_fails() {
            CountingReader file = new FailingReader();

            assertThatThrownBy(() -> importerOpening(file).importFrom(Path.of("prices.csv")))
                    .isInstanceOf(UncheckedIOException.class)
                    .hasCauseInstanceOf(IOException.class);
            assertThat(file.isClosed()).isTrue();
        }

        private CatalogImporter importerOpening(Reader file) {
            return new CatalogImporter(repository, 10, 0, path -> file);
        }

        /** A reader whose every read fails, to see that the file is still closed. */
        private static final class FailingReader extends CountingReader {

            FailingReader() {
                super("");
            }

            @Override
            public int read(char[] buffer, int offset, int length) throws IOException {
                throw new IOException("disk failed");
            }
        }
    }

    @Nested
    class Arguments {

        @Test
        void rejects_a_batch_size_below_one() {
            assertThatThrownBy(() -> new CatalogImporter(repository, 0, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_negative_max_errors() {
            assertThatThrownBy(() -> new CatalogImporter(repository, 1, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void requires_a_repository_and_a_reader() {
            assertThatThrownBy(() -> new CatalogImporter(null, 1, 0))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("repository");
            assertThatThrownBy(() -> importer(1, 0).importFrom((Reader) null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("reader");
        }
    }
}
