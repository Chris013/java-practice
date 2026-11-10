package io.github.chris013.practice.task27;

/** Builds CSV input text the way a supplier file looks on disk: one line per row, ending with a newline. */
final class CsvLines {

    static final String HEADER = "sku;name;price;stock";

    private CsvLines() {
    }

    static String of(String... lines) {
        return String.join("\n", lines) + "\n";
    }

    static String row(String sku, String name, String price, String stock) {
        return String.join(";", sku, name, price, stock);
    }
}
