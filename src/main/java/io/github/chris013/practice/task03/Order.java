package io.github.chris013.practice.task03;

import java.time.LocalDate;
import java.util.List;

public record Order(String id, String customerId, LocalDate date, OrderStatus status, List<OrderLine> lines) {

    public Order {
        lines = List.copyOf(lines);
    }
}
