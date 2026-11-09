package io.github.chris013.practice.task24.application;

import io.github.chris013.practice.task24.StockService;
import io.github.chris013.practice.task24.domain.OrderLine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Owns the rule "check every line, then reserve every line" and its reverse. Extracted from the processor so the
 * rule lives next to the only code that can break it.
 */
final class StockReservation {

    private final StockService stock;

    StockReservation(StockService stock) {
        this.stock = Objects.requireNonNull(stock, "stock");
    }

    /**
     * Reserves all lines, or none. Returns the first SKU that is short, in request order, and reserves nothing
     * in that case. Checking first is what keeps a half-filled order out of the warehouse.
     */
    Optional<String> reserveAll(List<OrderLine> lines) {
        for (OrderLine line : lines) {
            if (stock.available(line.sku()) < line.quantity()) {
                return Optional.of(line.sku());
            }
        }
        for (OrderLine line : lines) {
            stock.reserve(line.sku(), line.quantity());
        }
        return Optional.empty();
    }

    void releaseAll(List<OrderLine> lines) {
        for (OrderLine line : lines) {
            stock.release(line.sku(), line.quantity());
        }
    }
}
