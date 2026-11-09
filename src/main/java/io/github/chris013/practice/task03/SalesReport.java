package io.github.chris013.practice.task03;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Answers the questions the sales team asks every Monday.
 *
 * <p>Task 03: read {@code tasks/03-bookshop-sales-report.md} first. Cancelled orders never count as sales.
 */
public final class SalesReport {

    public SalesReport(List<Order> orders) {
        throw new UnsupportedOperationException("TODO: Task 03");
    }

    /** Total revenue of all orders that are not cancelled. */
    public BigDecimal revenue() {
        throw new UnsupportedOperationException("TODO: Task 03");
    }

    /** Revenue per genre. Genres without any sales are not in the map. */
    public Map<Genre, BigDecimal> revenueByGenre() {
        throw new UnsupportedOperationException("TODO: Task 03");
    }

    /** The {@code limit} books with the most copies sold. Ties are broken by title (alphabetically). */
    public List<Book> topSellingBooks(int limit) {
        throw new UnsupportedOperationException("TODO: Task 03");
    }

    /**
     * For each month with sales, the id of the customer who spent the most that month. Ties go to the alphabetically
     * smallest customer id. Iterates in chronological order.
     */
    public Map<YearMonth, String> bestCustomerPerMonth() {
        throw new UnsupportedOperationException("TODO: Task 03");
    }

    /** Average value of the orders that count, rounded half-up to cents. Empty if nothing was sold. */
    public Optional<BigDecimal> averageOrderValue() {
        throw new UnsupportedOperationException("TODO: Task 03");
    }

    /** Number of orders per status, including cancelled ones. Every status is present, even with a count of 0. */
    public Map<OrderStatus, Long> orderCountByStatus() {
        throw new UnsupportedOperationException("TODO: Task 03");
    }

    /**
     * Books that were bought in the same order as the book with the given ISBN, most frequent first (counted once per
     * order), ties broken by title. The book itself is not included.
     */
    public List<Book> booksOftenBoughtWith(String isbn) {
        throw new UnsupportedOperationException("TODO: Task 03");
    }

    /** Revenue and number of copies sold, computed in a single pass. */
    public SalesSummary summary() {
        throw new UnsupportedOperationException("TODO: Task 03");
    }
}
