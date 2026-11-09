package io.github.chris013.practice.task03;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.teeing;
import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

/** Answers the questions the sales team asks every Monday. Cancelled orders never count as sales. */
public final class SalesReport {

    /** Most first; ties broken alphabetically by title. */
    private static final Comparator<Map.Entry<Book, Long>> MOST_FIRST_THEN_BY_TITLE =
            Map.Entry.<Book, Long>comparingByValue().reversed().thenComparing(entry -> entry.getKey().title());

    private final List<Order> orders;

    public SalesReport(List<Order> orders) {
        this.orders = List.copyOf(orders);
    }

    public BigDecimal revenue() {
        return sales().collect(sumOf(Order::total));
    }

    public Map<Genre, BigDecimal> revenueByGenre() {
        return soldLines().collect(groupingBy(
                line -> line.book().genre(), () -> new EnumMap<>(Genre.class), sumOf(OrderLine::total)));
    }

    public List<Book> topSellingBooks(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must not be negative, was " + limit);
        }
        Map<Book, Long> copiesSold = soldLines().collect(groupingBy(OrderLine::book, summingLong(OrderLine::quantity)));
        return ranked(copiesSold).limit(limit).toList();
    }

    public Map<YearMonth, String> bestCustomerPerMonth() {
        return sales().collect(groupingBy(
                order -> YearMonth.from(order.date()),
                TreeMap::new,
                collectingAndThen(groupingBy(Order::customerId, sumOf(Order::total)), SalesReport::biggestSpender)));
    }

    public Optional<BigDecimal> averageOrderValue() {
        return sales().collect(teeing(
                sumOf(Order::total),
                counting(),
                (revenue, orderCount) -> orderCount == 0
                        ? Optional.empty()
                        : Optional.of(revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP))));
    }

    public Map<OrderStatus, Long> orderCountByStatus() {
        return Arrays.stream(OrderStatus.values()).collect(toMap(
                identity(),
                status -> orders.stream().filter(order -> order.status() == status).count(),
                (first, second) -> first,
                () -> new EnumMap<>(OrderStatus.class)));
    }

    public List<Book> booksOftenBoughtWith(String isbn) {
        Map<Book, Long> sharedOrders = sales()
                .filter(order -> order.contains(isbn))
                .flatMap(Order::distinctBooks)
                .filter(book -> !book.isbn().equals(isbn))
                .collect(groupingBy(identity(), counting()));
        return ranked(sharedOrders).toList();
    }

    public SalesSummary summary() {
        return soldLines().collect(teeing(
                sumOf(OrderLine::total), summingLong(OrderLine::quantity), SalesSummary::new));
    }

    private Stream<Order> sales() {
        return orders.stream().filter(Order::countsAsSale);
    }

    private Stream<OrderLine> soldLines() {
        return sales().flatMap(order -> order.lines().stream());
    }

    private static Stream<Book> ranked(Map<Book, Long> counts) {
        return counts.entrySet().stream().sorted(MOST_FIRST_THEN_BY_TITLE).map(Map.Entry::getKey);
    }

    private static String biggestSpender(Map<String, BigDecimal> revenueByCustomer) {
        Comparator<Map.Entry<String, BigDecimal>> byRevenueThenAlphabeticallyFirst = Map.Entry
                .<String, BigDecimal>comparingByValue()
                .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder()));
        return revenueByCustomer.entrySet().stream()
                .max(byRevenueThenAlphabeticallyFirst)
                .orElseThrow()
                .getKey();
    }

    private static <T> Collector<T, ?, BigDecimal> sumOf(Function<? super T, BigDecimal> amount) {
        return reducing(BigDecimal.ZERO, amount, BigDecimal::add);
    }
}
