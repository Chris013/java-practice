package io.github.chris013.practice.task13;

/**
 * The two German VAT rates a supermarket receipt can show. The codes {@code A} and {@code B} are what the
 * printed receipt says next to each line.
 *
 * <p>Task 13: read {@code tasks/13-receipt-enums.md} first.
 */
public enum VatRate {

    STANDARD('A', 19),
    REDUCED('B', 7);

    private final char code;
    private final int percent;

    VatRate(char code, int percent) {
        this.code = code;
        this.percent = percent;
    }

    /**
     * The one-letter code printed on the receipt.
     */
    public char code() {
        throw new UnsupportedOperationException("TODO: Task 13");
    }

    /**
     * The rate in percent, for example {@code 19} for {@link #STANDARD}.
     */
    public int percent() {
        throw new UnsupportedOperationException("TODO: Task 13");
    }

    /**
     * Finds the rate for a printed code. Codes are case-sensitive.
     *
     * @throws IllegalArgumentException if no rate has this code
     */
    public static VatRate fromCode(char code) {
        throw new UnsupportedOperationException("TODO: Task 13");
    }
}
