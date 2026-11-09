package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.List;

/** Test helper: builds basket lines in one line each, plus the grocery run used in the worked example. */
final class Lines {

    static final Sku PASTA = new Sku("PASTA-500G");
    static final Sku SOURDOUGH = new Sku("BAKERY-SOURDOUGH");
    static final Sku ROLLS = new Sku("BAKERY-ROLLS");
    static final Sku JUICE = new Sku("DRINKS-ORANGE-1L");
    static final Sku BUTTER = new Sku("DAIRY-BUTTER");
    static final Sku SOAP = new Sku("HOUSE-SOAP");

    private Lines() {
    }

    static BasketLine line(Sku sku, Category category, String unitPrice, int quantity) {
        return new BasketLine(sku, sku.value(), category, new BigDecimal(unitPrice), quantity);
    }

    /** Six lines with a subtotal of 20.64. */
    static List<BasketLine> groceryRun() {
        return List.of(
                line(PASTA, Category.PASTA, "1.49", 3),
                line(SOURDOUGH, Category.BAKERY, "2.40", 1),
                line(ROLLS, Category.BAKERY, "0.89", 1),
                line(JUICE, Category.DRINKS, "1.99", 2),
                line(BUTTER, Category.DAIRY, "3.20", 2),
                line(SOAP, Category.HOUSEHOLD, "2.50", 1));
    }
}
