package io.github.chris013.practice.task24;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Keeps saved orders in a list. */
final class InMemoryOrderStore implements OrderStore {

    record Saved(String orderId, String customerId, BigDecimal total, String status) {}

    private final List<Saved> saved = new ArrayList<>();

    List<Saved> saved() {
        return List.copyOf(saved);
    }

    @Override
    public void save(String orderId, String customerId, BigDecimal total, String status) {
        saved.add(new Saved(orderId, customerId, total, status));
    }
}
