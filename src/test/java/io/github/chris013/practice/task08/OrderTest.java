package io.github.chris013.practice.task08;

import static io.github.chris013.practice.task08.Money.euros;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task08.OrderEvent.OrderCancelled;
import io.github.chris013.practice.task08.OrderEvent.OrderPaid;
import io.github.chris013.practice.task08.OrderEvent.OrderPlaced;
import io.github.chris013.practice.task08.OrderEvent.OrderShipped;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final ProductId ETHIOPIA = new ProductId("ETHIOPIA-YIRGACHEFFE-250G");
    private static final ProductId BRAZIL = new ProductId("BRAZIL-CERRADO-1KG");
    private static final CustomerId ANNA = new CustomerId("anna");

    private final OrderId id = OrderId.random();
    private final Order order = Order.startFor(id, ANNA);

    /** 2 x 8.50 + 1 x 21.00 = 38.00 */
    private Order placedOrder() {
        order.addProduct(ETHIOPIA, euros("8.50"), 2);
        order.addProduct(BRAZIL, euros("21.00"), 1);
        order.place();
        order.pullEvents();
        return order;
    }

    private Order paidOrder() {
        placedOrder().markPaid(euros("38.00"));
        order.pullEvents();
        return order;
    }

    @Nested
    class Draft {

        @Test
        void starts_empty_and_without_events() {
            assertThat(order.id()).isEqualTo(id);
            assertThat(order.customerId()).isEqualTo(ANNA);
            assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
            assertThat(order.lines()).isEmpty();
            assertThat(order.total()).isEqualTo(Money.ZERO);
            assertThat(order.pullEvents()).isEmpty();
        }

        @Test
        void adds_products_and_sums_the_total() {
            order.addProduct(ETHIOPIA, euros("8.50"), 2);
            order.addProduct(BRAZIL, euros("21.00"), 1);

            assertThat(order.lines()).containsExactly(
                    new OrderLine(ETHIOPIA, euros("8.50"), 2), new OrderLine(BRAZIL, euros("21.00"), 1));
            assertThat(order.total()).isEqualTo(euros("38.00"));
        }

        @Test
        void merges_the_same_product_into_one_line() {
            order.addProduct(ETHIOPIA, euros("8.50"), 2);
            order.addProduct(ETHIOPIA, euros("8.50"), 3);

            assertThat(order.lines()).containsExactly(new OrderLine(ETHIOPIA, euros("8.50"), 5));
        }

        @Test
        void refuses_the_same_product_at_a_different_price() {
            order.addProduct(ETHIOPIA, euros("8.50"), 2);

            assertThatThrownBy(() -> order.addProduct(ETHIOPIA, euros("7.90"), 1))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(order.lines()).containsExactly(new OrderLine(ETHIOPIA, euros("8.50"), 2));
        }

        @Test
        void allows_1_to_99_items_per_product() {
            assertThatThrownBy(() -> order.addProduct(ETHIOPIA, euros("8.50"), 0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> order.addProduct(ETHIOPIA, euros("8.50"), 100))
                    .isInstanceOf(IllegalArgumentException.class);

            order.addProduct(ETHIOPIA, euros("8.50"), 60);
            assertThatThrownBy(() -> order.addProduct(ETHIOPIA, euros("8.50"), 40))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(order.lines()).containsExactly(new OrderLine(ETHIOPIA, euros("8.50"), 60));
        }

        @Test
        void changes_quantities() {
            order.addProduct(ETHIOPIA, euros("8.50"), 2);

            order.changeQuantity(ETHIOPIA, 4);

            assertThat(order.lines()).containsExactly(new OrderLine(ETHIOPIA, euros("8.50"), 4));
        }

        @Test
        void removes_a_product_when_its_quantity_becomes_zero() {
            order.addProduct(ETHIOPIA, euros("8.50"), 2);

            order.changeQuantity(ETHIOPIA, 0);

            assertThat(order.lines()).isEmpty();
        }

        @Test
        void removes_products() {
            order.addProduct(ETHIOPIA, euros("8.50"), 2);
            order.addProduct(BRAZIL, euros("21.00"), 1);

            order.removeProduct(ETHIOPIA);

            assertThat(order.lines()).extracting(OrderLine::productId).containsExactly(BRAZIL);
        }

        @Test
        void cannot_change_or_remove_a_product_that_is_not_on_the_order() {
            assertThatThrownBy(() -> order.changeQuantity(BRAZIL, 2)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> order.removeProduct(BRAZIL)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void does_not_let_anyone_modify_its_lines_from_outside() {
            order.addProduct(ETHIOPIA, euros("8.50"), 2);

            assertThatThrownBy(() -> order.lines().clear()).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Placing {

        @Test
        void announces_the_order_with_its_total() {
            order.addProduct(ETHIOPIA, euros("8.50"), 2);

            order.place();

            assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
            assertThat(order.pullEvents()).containsExactly(new OrderPlaced(id, ANNA, euros("17.00")));
        }

        @Test
        void needs_at_least_one_product() {
            assertThatThrownBy(order::place).isInstanceOf(IllegalStateException.class);
            assertThat(order.status()).isEqualTo(OrderStatus.DRAFT);
        }

        @Test
        void needs_the_minimum_order_value_of_15_euros() {
            order.addProduct(ETHIOPIA, euros("14.99"), 1);
            assertThatThrownBy(order::place).isInstanceOf(IllegalStateException.class).hasMessageContaining("15.00");

            order.changeQuantity(ETHIOPIA, 0);
            order.addProduct(BRAZIL, euros("15.00"), 1);
            order.place();
            assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        }

        @Test
        void freezes_the_lines() {
            placedOrder();

            assertThatThrownBy(() -> order.addProduct(ETHIOPIA, euros("8.50"), 1)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> order.changeQuantity(ETHIOPIA, 1)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> order.removeProduct(ETHIOPIA)).isInstanceOf(IllegalStateException.class);
            assertThat(order.total()).isEqualTo(euros("38.00"));
        }

        @Test
        void happens_only_once() {
            placedOrder();

            assertThatThrownBy(order::place).isInstanceOf(IllegalStateException.class);
            assertThat(order.pullEvents()).isEmpty();
        }
    }

    @Nested
    class Paying {

        @Test
        void requires_exactly_the_order_total() {
            placedOrder();

            order.markPaid(euros("38.00"));

            assertThat(order.status()).isEqualTo(OrderStatus.PAID);
            assertThat(order.pullEvents()).containsExactly(new OrderPaid(id, euros("38.00")));
        }

        @Test
        void rejects_a_wrong_amount_without_changing_anything() {
            placedOrder();

            assertThatThrownBy(() -> order.markPaid(euros("37.99"))).isInstanceOf(IllegalArgumentException.class);
            assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
            assertThat(order.pullEvents()).isEmpty();
        }

        @Test
        void is_only_possible_for_placed_orders() {
            order.addProduct(BRAZIL, euros("21.00"), 1);

            assertThatThrownBy(() -> order.markPaid(euros("21.00"))).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Shipping {

        @Test
        void ships_paid_orders_with_a_tracking_number() {
            paidOrder();

            order.ship("DHL-123456");

            assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
            assertThat(order.pullEvents()).containsExactly(new OrderShipped(id, "DHL-123456"));
        }

        @Test
        void never_ships_unpaid_orders() {
            placedOrder();

            assertThatThrownBy(() -> order.ship("DHL-123456")).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void needs_a_tracking_number() {
            paidOrder();

            assertThatThrownBy(() -> order.ship(" ")).isInstanceOf(IllegalArgumentException.class);
            assertThat(order.status()).isEqualTo(OrderStatus.PAID);
        }
    }

    @Nested
    class Cancelling {

        @Test
        void a_draft_quietly_because_nobody_else_knows_about_it() {
            order.addProduct(ETHIOPIA, euros("8.50"), 2);

            order.cancel("changed my mind");

            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.pullEvents()).isEmpty();
        }

        @Test
        void a_placed_order_without_refund() {
            placedOrder();

            order.cancel("out of stock");

            assertThat(order.pullEvents()).containsExactly(new OrderCancelled(id, "out of stock", false));
        }

        @Test
        void a_paid_order_with_refund() {
            paidOrder();

            order.cancel("out of stock");

            assertThat(order.pullEvents()).containsExactly(new OrderCancelled(id, "out of stock", true));
        }

        @Test
        void is_impossible_once_shipped() {
            paidOrder();
            order.ship("DHL-123456");

            assertThatThrownBy(() -> order.cancel("too late")).isInstanceOf(IllegalStateException.class);
            assertThat(order.status()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        void twice_is_harmless() {
            placedOrder();
            order.cancel("out of stock");
            order.pullEvents();

            order.cancel("out of stock");

            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.pullEvents()).isEmpty();
        }

        @Test
        void ends_the_life_of_the_order() {
            placedOrder();
            order.cancel("out of stock");

            assertThatThrownBy(() -> order.markPaid(euros("38.00"))).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> order.ship("DHL-1")).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Events {

        @Test
        void are_recorded_in_the_order_they_happened() {
            order.addProduct(BRAZIL, euros("21.00"), 1);
            order.place();
            order.markPaid(euros("21.00"));
            order.ship("DHL-42");

            assertThat(order.pullEvents()).containsExactly(
                    new OrderPlaced(id, ANNA, euros("21.00")),
                    new OrderPaid(id, euros("21.00")),
                    new OrderShipped(id, "DHL-42"));
        }

        @Test
        void are_handed_out_only_once() {
            placedOrder();
            order.markPaid(euros("38.00"));

            assertThat(order.pullEvents()).hasSize(1);
            assertThat(order.pullEvents()).isEmpty();
        }
    }
}
