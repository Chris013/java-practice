package io.github.chris013.practice.task03;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

public record Order(String id, String customerId, LocalDate date, OrderStatus status, List<OrderLine> lines) {

    public Order {
        lines = List.copyOf(lines);
    }

    public boolean countsAsSale() {
        return status != OrderStatus.CANCELLED;
    }

    public BigDecimal total() {
        return lines.stream().map(OrderLine::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Each book in this order once, even if it appears on several lines. */
    public Stream<Book> distinctBooks() {
        return lines.stream().map(OrderLine::book).distinct();
    }

    public boolean contains(String isbn) {
        return lines.stream().anyMatch(line -> line.book().isbn().equals(isbn));
    }
}
