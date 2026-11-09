package io.github.chris013.practice.task08;

import java.util.List;

/**
 * An order in our coffee webshop. The aggregate root: all changes go through this class, and it guarantees the
 * business rules hold after every method call.
 *
 * <p>Task 08: read {@code tasks/08-order-aggregate.md} first.
 */
public final class Order {

    /** Starts a new, empty draft order. */
    public static Order startFor(OrderId id, CustomerId customer) {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public void addProduct(ProductId product, Money unitPrice, int quantity) {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    /** Sets the quantity of a product already on the order; 0 removes it. */
    public void changeQuantity(ProductId product, int newQuantity) {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public void removeProduct(ProductId product) {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public void place() {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public void markPaid(Money amount) {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public void ship(String trackingNumber) {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public void cancel(String reason) {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public OrderId id() {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public CustomerId customerId() {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public OrderStatus status() {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public List<OrderLine> lines() {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    public Money total() {
        throw new UnsupportedOperationException("TODO: Task 08");
    }

    /** Returns the events recorded since the last call, oldest first, and forgets them. */
    public List<OrderEvent> pullEvents() {
        throw new UnsupportedOperationException("TODO: Task 08");
    }
}
