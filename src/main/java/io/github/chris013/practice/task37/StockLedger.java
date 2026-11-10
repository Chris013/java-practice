package io.github.chris013.practice.task37;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps track of how many units of each SKU we hold, and which warehouse bins are open for picking.
 *
 * <p>Task 37: read {@code tasks/37-boxed-primitive-traps.md} first. This is legacy code. It passes its tests,
 * but it has five bugs that only show up with bin numbers above 127, unknown SKUs and large quantities.
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
        int quantity = stock.get(sku);
        return quantity;
    }

    /** Two bin numbers are the same bin when their values are equal. Two missing bins are the same. */
    public static boolean sameBin(Integer binA, Integer binB) {
        return binA == binB;
    }

    /** Opens a bin for picking. */
    public void openBin(int binNumber) {
        openBins.add(binNumber);
    }

    /** Closes the bin with this number. Closing a bin that is not open does nothing. */
    public void closeBin(int binNumber) {
        openBins.remove(binNumber);
    }

    /** The open bins, in the order they were opened. The list cannot be changed. */
    public List<Integer> openBins() {
        return List.copyOf(openBins);
    }

    /** Orders quantities by their numeric value, smallest first. */
    public static final Comparator<Integer> BY_QUANTITY = (i, j) -> (i < j) ? -1 : (i == j ? 0 : 1);

    /** All quantities we hold, sorted by {@link #BY_QUANTITY}. */
    public List<Integer> sortedQuantities() {
        List<Integer> quantities = new ArrayList<>(stock.values());
        quantities.sort(BY_QUANTITY);
        return quantities;
    }

    /** The total number of units across all SKUs. */
    public long totalUnits() {
        Long total = 0L;
        for (Integer quantity : stock.values()) {
            total += quantity;
        }
        return total;
    }
}
