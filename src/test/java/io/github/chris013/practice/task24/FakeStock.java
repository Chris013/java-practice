package io.github.chris013.practice.task24;

import java.util.HashMap;
import java.util.Map;

/** A hand-written fake warehouse: a counter per SKU that reserve and release move up and down. */
final class FakeStock implements StockService {

    private final Map<String, Integer> available = new HashMap<>();

    FakeStock with(String sku, int quantity) {
        available.put(sku, quantity);
        return this;
    }

    int availableOf(String sku) {
        return available.getOrDefault(sku, 0);
    }

    @Override
    public int available(String sku) {
        return availableOf(sku);
    }

    @Override
    public void reserve(String sku, int qty) {
        if (availableOf(sku) < qty) {
            throw new IllegalStateException("Reserved more than is available: " + sku);
        }
        available.put(sku, availableOf(sku) - qty);
    }

    @Override
    public void release(String sku, int qty) {
        available.put(sku, availableOf(sku) + qty);
    }
}
