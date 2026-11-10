package io.github.chris013.practice.task33;

import java.math.BigDecimal;
import java.util.List;

/**
 * A fixed catalogue of eight products, used by all tests. The list order in {@link #all()} is the
 * catalogue (insertion) order. The values are chosen so that boundaries and ties are easy to see.
 */
final class SampleProducts {

    static final Product CLAW_HAMMER = product("T-100", "Claw hammer", Category.TOOLS, "12.50", 4, 600);
    static final Product CORDLESS_DRILL = product("T-200", "Cordless drill", Category.TOOLS, "89.00", 0, 1800);
    static final Product ROLLED_OATS = product("F-010", "Rolled oats 1kg", Category.FOOD, "2.49", 30, 1000);
    static final Product OLIVE_OIL = product("F-020", "Olive oil", Category.FOOD, "7.90", 12, 750);
    static final Product PLUSH_BEAR = product("Y-001", "Plush bear", Category.TOYS, "15.00", 8, 300);
    static final Product WOODEN_TRAIN = product("Y-002", "Wooden train set", Category.TOYS, "15.00", 0, 1200);
    static final Product HAMMER_HANDBOOK = product("B-001", "Hammer handbook", Category.BOOKS, "19.99", 2, 450);
    static final Product GARDENING_BOOK = product("B-002", "Gardening for beginners", Category.BOOKS, "5.00", 6, 400);

    private SampleProducts() {
    }

    static List<Product> all() {
        return List.of(CLAW_HAMMER, CORDLESS_DRILL, ROLLED_OATS, OLIVE_OIL,
                PLUSH_BEAR, WOODEN_TRAIN, HAMMER_HANDBOOK, GARDENING_BOOK);
    }

    static Product product(String sku, String name, Category category, String price, int stock, int weightGrams) {
        return new Product(sku, name, category, new BigDecimal(price), stock, weightGrams);
    }
}
