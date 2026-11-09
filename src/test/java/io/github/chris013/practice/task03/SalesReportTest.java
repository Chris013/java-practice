package io.github.chris013.practice.task03;

import static io.github.chris013.practice.task03.Genre.HISTORY;
import static io.github.chris013.practice.task03.Genre.SCIENCE_FICTION;
import static io.github.chris013.practice.task03.Genre.SOFTWARE;
import static io.github.chris013.practice.task03.OrderStatus.CANCELLED;
import static io.github.chris013.practice.task03.OrderStatus.DELIVERED;
import static io.github.chris013.practice.task03.OrderStatus.PLACED;
import static io.github.chris013.practice.task03.OrderStatus.SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SalesReportTest {

    static final Book DDD = new Book("978-0321125217", "Domain-Driven Design", "Eric Evans", SOFTWARE);
    static final Book EFFECTIVE_JAVA = new Book("978-0134685991", "Effective Java", "Joshua Bloch", SOFTWARE);
    static final Book DUNE = new Book("978-0441172719", "Dune", "Frank Herbert", SCIENCE_FICTION);
    static final Book FOUNDATION = new Book("978-0553293357", "Foundation", "Isaac Asimov", SCIENCE_FICTION);
    static final Book SAPIENS = new Book("978-0062316097", "Sapiens", "Yuval Noah Harari", HISTORY);

    /*
     * order  customer  date        status     lines                                   total
     * o1     anna      2026-01-05  DELIVERED  DDD 1x54.99, EJ 1x44.99                  99.98
     * o2     ben       2026-01-12  DELIVERED  Dune 2x9.99                              19.98
     * o3     ben       2026-01-20  CANCELLED  Sapiens 1x14.99, DDD 1x54.99            (69.98, never counts)
     * o4     carla     2026-02-02  SHIPPED    EJ 2x44.99, Dune 1x9.99                  99.97
     * o5     anna      2026-02-14  PLACED     Foundation 3x8.99                        26.97
     * o6     ben       2026-02-20  DELIVERED  EJ 1x39.99, Sapiens 1x14.99              54.98
     * o7     dave      2026-02-25  DELIVERED  EJ 1x44.99, Dune 1x9.99                  54.98
     */
    static final List<Order> ORDERS = List.of(
            order("o1", "anna", "2026-01-05", DELIVERED, line(DDD, 1, "54.99"), line(EFFECTIVE_JAVA, 1, "44.99")),
            order("o2", "ben", "2026-01-12", DELIVERED, line(DUNE, 2, "9.99")),
            order("o3", "ben", "2026-01-20", CANCELLED, line(SAPIENS, 1, "14.99"), line(DDD, 1, "54.99")),
            order("o4", "carla", "2026-02-02", SHIPPED, line(EFFECTIVE_JAVA, 2, "44.99"), line(DUNE, 1, "9.99")),
            order("o5", "anna", "2026-02-14", PLACED, line(FOUNDATION, 3, "8.99")),
            order("o6", "ben", "2026-02-20", DELIVERED, line(EFFECTIVE_JAVA, 1, "39.99"), line(SAPIENS, 1, "14.99")),
            order("o7", "dave", "2026-02-25", DELIVERED, line(EFFECTIVE_JAVA, 1, "44.99"), line(DUNE, 1, "9.99")));

    private final SalesReport report = new SalesReport(ORDERS);
    private final SalesReport emptyReport = new SalesReport(List.of());

    @Nested
    class Revenue {

        @Test
        void sums_all_orders_except_cancelled_ones() {
            assertThat(report.revenue()).isEqualByComparingTo("356.86");
        }

        @Test
        void is_zero_without_orders() {
            assertThat(emptyReport.revenue()).isEqualByComparingTo("0");
        }

        @Test
        void is_broken_down_by_genre() {
            assertThat(report.revenueByGenre())
                    .containsOnlyKeys(SOFTWARE, SCIENCE_FICTION, HISTORY)
                    .hasEntrySatisfying(SOFTWARE, revenue -> assertThat(revenue).isEqualByComparingTo("274.94"))
                    .hasEntrySatisfying(SCIENCE_FICTION, revenue -> assertThat(revenue).isEqualByComparingTo("66.93"))
                    .hasEntrySatisfying(HISTORY, revenue -> assertThat(revenue).isEqualByComparingTo("14.99"));
        }
    }

    @Nested
    class TopSellingBooks {

        @Test
        void ranks_books_by_copies_sold() {
            assertThat(report.topSellingBooks(3)).containsExactly(EFFECTIVE_JAVA, DUNE, FOUNDATION);
        }

        @Test
        void breaks_ties_by_title() {
            // DDD and Sapiens both sold 1 copy (the cancelled order doesn't count).
            assertThat(report.topSellingBooks(4)).containsExactly(EFFECTIVE_JAVA, DUNE, FOUNDATION, DDD);
        }

        @Test
        void returns_fewer_books_if_fewer_were_sold() {
            assertThat(report.topSellingBooks(100)).hasSize(5);
            assertThat(emptyReport.topSellingBooks(3)).isEmpty();
        }

        @Test
        void refuses_a_negative_limit() {
            assertThatThrownBy(() -> report.topSellingBooks(-1)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class BestCustomerPerMonth {

        @Test
        void finds_the_biggest_spender_of_each_month_in_chronological_order() {
            assertThat(report.bestCustomerPerMonth())
                    .containsExactly(entry(YearMonth.of(2026, 1), "anna"), entry(YearMonth.of(2026, 2), "carla"));
        }

        @Test
        void gives_ties_to_the_alphabetically_first_customer() {
            SalesReport tie = new SalesReport(List.of(
                    order("x1", "zoe", "2026-03-01", DELIVERED, line(DUNE, 1, "9.99")),
                    order("x2", "max", "2026-03-02", DELIVERED, line(DUNE, 1, "9.99"))));

            assertThat(tie.bestCustomerPerMonth()).containsExactly(entry(YearMonth.of(2026, 3), "max"));
        }
    }

    @Nested
    class AverageOrderValue {

        @Test
        void is_rounded_half_up_to_cents() {
            // 356.86 / 6 = 59.4766...
            assertThat(report.averageOrderValue()).hasValueSatisfying(
                    average -> assertThat(average).isEqualByComparingTo("59.48"));
        }

        @Test
        void is_empty_when_nothing_was_sold() {
            assertThat(emptyReport.averageOrderValue()).isEmpty();
        }
    }

    @Nested
    class OrderCountByStatus {

        @Test
        void counts_every_order_including_cancelled_ones() {
            assertThat(report.orderCountByStatus())
                    .containsExactly(entry(PLACED, 1L), entry(SHIPPED, 1L), entry(DELIVERED, 4L), entry(CANCELLED, 1L));
        }

        @Test
        void contains_every_status_even_without_orders() {
            assertThat(emptyReport.orderCountByStatus())
                    .containsExactly(entry(PLACED, 0L), entry(SHIPPED, 0L), entry(DELIVERED, 0L), entry(CANCELLED, 0L));
        }
    }

    @Nested
    class BooksOftenBoughtWith {

        @Test
        void ranks_by_number_of_shared_orders_then_title() {
            assertThat(report.booksOftenBoughtWith(EFFECTIVE_JAVA.isbn())).containsExactly(DUNE, DDD, SAPIENS);
        }

        @Test
        void ignores_cancelled_orders() {
            assertThat(report.booksOftenBoughtWith(DDD.isbn())).containsExactly(EFFECTIVE_JAVA);
        }

        @Test
        void is_empty_for_an_unknown_book() {
            assertThat(report.booksOftenBoughtWith("000-0000000000")).isEmpty();
        }
    }

    @Test
    void summarizes_revenue_and_copies_sold() {
        SalesSummary summary = report.summary();

        assertThat(summary.revenue()).isEqualByComparingTo("356.86");
        assertThat(summary.unitsSold()).isEqualTo(14);
    }

    @Test
    void is_not_affected_by_later_changes_to_the_input_list() {
        List<Order> orders = new ArrayList<>(ORDERS);
        SalesReport snapshot = new SalesReport(orders);

        orders.clear();

        assertThat(snapshot.revenue()).isEqualByComparingTo("356.86");
    }

    private static Order order(String id, String customer, String date, OrderStatus status, OrderLine... lines) {
        return new Order(id, customer, LocalDate.parse(date), status, List.of(lines));
    }

    private static OrderLine line(Book book, int quantity, String unitPrice) {
        return new OrderLine(book, quantity, new BigDecimal(unitPrice));
    }
}
