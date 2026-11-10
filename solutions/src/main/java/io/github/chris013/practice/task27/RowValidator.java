package io.github.chris013.practice.task27;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Applies the field rules to one row and collects one error per failing field, in column order.
 *
 * <p>It also remembers the SKUs of the valid rows seen so far, because a duplicate SKU can only be detected by
 * looking back. One validator belongs to one import, so the memory is released when the import ends.
 */
final class RowValidator {

    private static final Pattern SKU = Pattern.compile("[A-Z0-9-]{3,20}");
    private static final Pattern PRICE = Pattern.compile("\\d+(,\\d{1,2})?");
    private static final Pattern STOCK = Pattern.compile("\\d{1,9}");
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_STOCK = 1_000_000;

    private final Map<String, Integer> lineBySku = new HashMap<>();

    /**
     * Validates the four required fields. Every problem is reported, not only the first one, so the supplier
     * can fix the whole row in one go. A row with any problem does not register its SKU.
     */
    RowOutcome validate(int lineNumber, String sku, String name, String price, String stock) {
        List<RowError> errors = new ArrayList<>();
        checkSku(lineNumber, sku, errors);
        if (name.isBlank() || name.length() > MAX_NAME_LENGTH) {
            errors.add(new RowError(lineNumber, "name", "must not be blank and at most 100 characters"));
        }
        Optional<BigDecimal> parsedPrice = parsePrice(price);
        if (parsedPrice.isEmpty()) {
            errors.add(new RowError(lineNumber, "price", "must be a positive price like 12,99"));
        }
        Optional<Integer> parsedStock = parseStock(stock);
        if (parsedStock.isEmpty()) {
            errors.add(new RowError(lineNumber, "stock", "must be a whole number from 0 to 1000000"));
        }
        if (!errors.isEmpty()) {
            return new RowOutcome.Invalid(errors);
        }
        lineBySku.put(sku, lineNumber);
        return new RowOutcome.Valid(new Product(sku, name, parsedPrice.orElseThrow(), parsedStock.orElseThrow()));
    }

    private void checkSku(int lineNumber, String sku, List<RowError> errors) {
        if (!SKU.matcher(sku).matches()) {
            errors.add(new RowError(lineNumber, "sku", "must be 3 to 20 characters A-Z, 0-9 or -"));
        } else if (lineBySku.containsKey(sku)) {
            // The first valid row keeps the SKU, so later rows point back to it.
            errors.add(new RowError(lineNumber, "sku", "duplicate of line " + lineBySku.get(sku)));
        }
    }

    /** German decimal without thousands separators. "12,99" becomes 12.99 with scale 2, never a double. */
    private static Optional<BigDecimal> parsePrice(String text) {
        if (!PRICE.matcher(text).matches()) {
            return Optional.empty();
        }
        BigDecimal value = new BigDecimal(text.replace(',', '.')).setScale(2);
        return value.signum() > 0 ? Optional.of(value) : Optional.empty();
    }

    /** Only digits are accepted, so no sign and no separator. At most 9 digits, so parseInt cannot overflow. */
    private static Optional<Integer> parseStock(String text) {
        if (!STOCK.matcher(text).matches()) {
            return Optional.empty();
        }
        int value = Integer.parseInt(text);
        return value <= MAX_STOCK ? Optional.of(value) : Optional.empty();
    }
}
