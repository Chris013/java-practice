package io.github.chris013.practice.task37;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Keeps track of how many units of each SKU we hold, and which warehouse bins are open for picking.
 *
 * <p>Task 37: the reference fixes are marked where they happen. Each one is the smallest change that removes
 * the bug. The class is otherwise unchanged.
 */
public final class StockLedger {

    private final Map<String, Integer> stock = new HashMap<>();
    private final List<Integer> openBins = new ArrayList<>();

    public StockLedger() {
    }

    /** Adds {@code quantity} units of {@code sku} to the stock. The quantity must be positive. */
    public void receive(String sku, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive, was " + quantity);
        }
        stock.merge(sku, quantity, Integer::sum);
    }

    /** Returns the units on hand for {@code sku}, or 0 when we have never received that SKU. */
    public int quantityOf(String sku) {
        // Fix 1: getOrDefault instead of unboxing a null that get() returns for an unknown SKU.
        return stock.getOrDefault(sku, 0);
    }

    /** Two bin numbers are the same bin when their values are equal. Two missing bins are the same. */
    public static boolean sameBin(Integer binA, Integer binB) {
        // Fix 2: Objects.equals compares the values. == compares references, and only -128..127 are cached.
        return Objects.equals(binA, binB);
    }

    /** Opens a bin for picking. */
    public void openBin(int binNumber) {
        openBins.add(binNumber);
    }

    /** Closes the bin with this number. Closing a bin that is not open does nothing. */
    public void closeBin(int binNumber) {
        // Fix 3: Integer.valueOf picks remove(Object). A plain int would pick remove(int), which takes an index.
        openBins.remove(Integer.valueOf(binNumber));
    }

    /** The open bins, in the order they were opened. The list cannot be changed. */
    public List<Integer> openBins() {
        return List.copyOf(openBins);
    }

    /** Orders quantities by their numeric value, smallest first. */
    // Fix 4: the natural order of Integer is correct for every value. The hand-written lambda was not.
    public static final Comparator<Integer> BY_QUANTITY = Comparator.naturalOrder();

    /** All quantities we hold, sorted by {@link #BY_QUANTITY}. */
    public List<Integer> sortedQuantities() {
        List<Integer> quantities = new ArrayList<>(stock.values());
        quantities.sort(BY_QUANTITY);
        return quantities;
    }

    /** The total number of units across all SKUs. */
    public long totalUnits() {
        // Fix 5: a primitive long. The boxed Long was unboxed and boxed again on every iteration.
        long total = 0L;
        for (int quantity : stock.values()) {
            total += quantity;
        }
        return total;
    }
}
