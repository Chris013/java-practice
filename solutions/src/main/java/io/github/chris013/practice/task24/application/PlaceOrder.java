package io.github.chris013.practice.task24.application;

import io.github.chris013.practice.task24.MailSender;
import io.github.chris013.practice.task24.OrderRequest;
import io.github.chris013.practice.task24.OrderStore;
import io.github.chris013.practice.task24.PaymentDeclinedException;
import io.github.chris013.practice.task24.PaymentGateway;
import io.github.chris013.practice.task24.StockService;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * The use case: place one order. It is a Split Phase in two steps.
 *
 * <ol>
 *   <li>{@link OrderQuote#of} decides everything it can without side effects (validation and price).</li>
 *   <li>{@link #execute} touches the world: stock, payment, storage and mail, in that order.</li>
 * </ol>
 *
 * <p>The side effects only ever see a valid, priced order. Ports are the interfaces from the warehouse, payment,
 * mail and database teams. This class knows nothing about how they are implemented.
 */
public final class PlaceOrder {

    private final StockReservation stock;
    private final PaymentGateway payments;
    private final MailSender mailSender;
    private final OrderStore orderStore;
    private final OrderIds orderIds;

    public PlaceOrder(StockService stock, PaymentGateway payments, MailSender mailSender,
            OrderStore orderStore, Clock clock) {
        this.stock = new StockReservation(stock);
        this.payments = Objects.requireNonNull(payments, "payments");
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.orderStore = Objects.requireNonNull(orderStore, "orderStore");
        this.orderIds = new OrderIds(clock);
    }

    public PlaceOrderResult place(OrderRequest request) {
        Objects.requireNonNull(request, "request");
        return switch (OrderQuote.of(request)) {
            case OrderQuote.Refused refused -> PlaceOrderResult.Rejected.because(refused.reason());
            case OrderQuote.Accepted accepted -> execute(accepted);
        };
    }

    private PlaceOrderResult execute(OrderQuote.Accepted order) {
        Optional<String> shortSku = stock.reserveAll(order.lines());
        if (shortSku.isPresent()) {
            return PlaceOrderResult.Rejected.outOfStock(shortSku.get());
        }

        BigDecimal total = order.pricing().total().amount();
        try {
            payments.charge(order.customerId(), total);
        } catch (PaymentDeclinedException declined) {
            // Stock was reserved before the card was charged. Give it back, or the next customer
            // sees a product as sold out that nobody bought.
            stock.releaseAll(order.lines());
            return PlaceOrderResult.Rejected.because(RejectionReason.PAYMENT_DECLINED);
        }

        String orderId = orderIds.next();
        orderStore.save(orderId, order.customerId(), total, "PAID");
        mailSender.send(order.email(), OrderConfirmation.subject(orderId), OrderConfirmation.body(orderId, order));
        return new PlaceOrderResult.Placed(orderId);
    }
}
