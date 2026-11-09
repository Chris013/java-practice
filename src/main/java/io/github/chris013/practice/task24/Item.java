package io.github.chris013.practice.task24;

import java.math.BigDecimal;

/** One line of a checkout request. {@code unitPrice} is in euros, with cents. */
public record Item(String sku, int quantity, BigDecimal unitPrice) {}
