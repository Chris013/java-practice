package io.github.chris013.practice.task08;

/** Given. Things that happened to an order that other parts of the system care about. */
public sealed interface OrderEvent {

    OrderId orderId();

    record OrderPlaced(OrderId orderId, CustomerId customerId, Money total) implements OrderEvent {}

    record OrderPaid(OrderId orderId, Money amount) implements OrderEvent {}

    record OrderShipped(OrderId orderId, String trackingNumber) implements OrderEvent {}

    record OrderCancelled(OrderId orderId, String reason, boolean refundDue) implements OrderEvent {}
}
