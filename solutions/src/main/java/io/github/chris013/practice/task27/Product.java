package io.github.chris013.practice.task27;

import java.math.BigDecimal;

/** One valid row of a supplier price list, ready to be stored. */
public record Product(String sku, String name, BigDecimal price, int stock) {}
