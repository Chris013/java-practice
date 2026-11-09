package io.github.chris013.practice.task08;

import static io.github.chris013.practice.task08.OrderStatus.CANCELLED;
import static io.github.chris013.practice.task08.OrderStatus.DRAFT;
import static io.github.chris013.practice.task08.OrderStatus.PAID;
import static io.github.chris013.practice.task08.OrderStatus.PLACED;
import static io.github.chris013.practice.task08.OrderStatus.SHIPPED;

import io.github.chris013.practice.task08.OrderEvent.OrderCancelled;
import io.github.chris013.practice.task08.OrderEvent.OrderPaid;
import io.github.chris013.practice.task08.OrderEvent.OrderPlaced;
import io.github.chris013.practice.task08.OrderEvent.OrderShipped;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An order in our coffee webshop, and the root of its aggregate.
 *
 * <p>Invariants, true after every public method returns:
 * <ul>
 *   <li>lines change only while the order is a {@link OrderStatus#DRAFT draft};</li>
 *   <li>each product appears on at most one line, with 1 to 99 items;</li>
 *   <li>an order that has left DRAFT through {@code place()} has at least one line and a total of at least
 *       {@link #MINIMUM_ORDER_VALUE};</li>
 *   <li>the status only moves along DRAFT → PLACED → PAID → SHIPPED, or to CANCELLED before shipping.</li>
 * </ul>
 */
public final class Order {

    static final Money MINIMUM_ORDER_VALUE = Money.euros("15.00");
    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(DRAFT, PLACED, PAID);

    private final OrderId id;
    private final CustomerId customerId;
    private final Map<ProductId, OrderLine> lines = new LinkedHashMap<>();
    private final List<OrderEvent> events = new ArrayList<>();
    private OrderStatus status = DRAFT;

    private Order(OrderId id, CustomerId customerId) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
    }

    public static Order startFor(OrderId id, CustomerId customer) {
        return new Order(id, customer);
    }

    public void addProduct(ProductId product, Money unitPrice, int quantity) {
        requireStatus(DRAFT, "add products");
        OrderLine existing = lines.get(product);
        if (existing == null) {
            lines.put(product, new OrderLine(product, unitPrice, quantity));
            return;
        }
        if (!existing.unitPrice().equals(unitPrice)) {
            throw new IllegalArgumentException("%s is already on the order at %s, not %s"
                    .formatted(product.value(), existing.unitPrice(), unitPrice));
        }
        lines.put(product, existing.withQuantity(existing.quantity() + quantity));
    }

    public void changeQuantity(ProductId product, int newQuantity) {
        requireStatus(DRAFT, "change quantities");
        OrderLine line = existingLine(product);
        if (newQuantity == 0) {
            lines.remove(product);
        } else {
            lines.put(product, line.withQuantity(newQuantity));
        }
    }

    public void removeProduct(ProductId product) {
        requireStatus(DRAFT, "remove products");
        existingLine(product);
        lines.remove(product);
    }

    public void place() {
        requireStatus(DRAFT, "place it");
        if (lines.isEmpty()) {
            throw new IllegalStateException("Cannot place an empty order");
        }
        if (total().isLessThan(MINIMUM_ORDER_VALUE)) {
            throw new IllegalStateException(
                    "The minimum order value is %s, this order is %s".formatted(MINIMUM_ORDER_VALUE, total()));
        }
        status = PLACED;
        events.add(new OrderPlaced(id, customerId, total()));
    }

    public void markPaid(Money amount) {
        requireStatus(PLACED, "mark it paid");
        if (!amount.equals(total())) {
            throw new IllegalArgumentException("Order %s costs %s, but %s was paid".formatted(id.value(), total(), amount));
        }
        status = PAID;
        events.add(new OrderPaid(id, amount));
    }

    public void ship(String trackingNumber) {
        requireStatus(PAID, "ship it");
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("A shipment needs a tracking number");
        }
        status = SHIPPED;
        events.add(new OrderShipped(id, trackingNumber));
    }

    public void cancel(String reason) {
        if (status == CANCELLED) {
            return;
        }
        if (!CANCELLABLE.contains(status)) {
            throw new IllegalStateException("Order %s is %s and can no longer be cancelled".formatted(id.value(), status));
        }
        OrderStatus previous = status;
        status = CANCELLED;
        if (previous != DRAFT) {
            events.add(new OrderCancelled(id, reason, previous == PAID));
        }
    }

    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public OrderStatus status() {
        return status;
    }

    public List<OrderLine> lines() {
        return List.copyOf(lines.values());
    }

    public Money total() {
        return lines.values().stream().map(OrderLine::total).reduce(Money.ZERO, Money::plus);
    }

    public List<OrderEvent> pullEvents() {
        List<OrderEvent> recorded = List.copyOf(events);
        events.clear();
        return recorded;
    }

    private OrderLine existingLine(ProductId product) {
        OrderLine line = lines.get(product);
        if (line == null) {
            throw new IllegalArgumentException("%s is not on order %s".formatted(product.value(), id.value()));
        }
        return line;
    }

    private void requireStatus(OrderStatus required, String action) {
        if (status != required) {
            throw new IllegalStateException(
                    "Cannot %s: order %s is %s, must be %s".formatted(action, id.value(), status, required));
        }
    }
}
