package io.github.chris013.practice.task13;

import java.math.BigDecimal;

/**
 * One scanned product, with the number of items bought.
 *
 * <p>Prices are gross: they include VAT, as the shelf price does.
 */
public record LineItem(String name, Category category, BigDecimal unitPriceGross, int quantity) {

    public LineItem {
        throw new UnsupportedOperationException("TODO: Task 13");
    }

    /** The gross price of all items on this line: unit price times quantity. */
    public BigDecimal totalGross() {
        throw new UnsupportedOperationException("TODO: Task 13");
    }
}
