package io.github.chris013.practice.task24;

import io.github.chris013.practice.task24.application.PlaceOrder;
import io.github.chris013.practice.task24.application.PlaceOrderResult;

import java.time.Clock;

/**
 * Compatibility facade for the checkout page. It keeps the old constructor and the old string results, so the page
 * does not change. All the logic lives in {@link PlaceOrder}. New code should call {@link PlaceOrder} directly
 * and match on {@link PlaceOrderResult}.
 */
public class OrderProcessor {

    private final PlaceOrder placeOrder;

    public OrderProcessor(StockService stock, PaymentGateway payments, MailSender mailSender,
            OrderStore orderStore, Clock clock) {
        this.placeOrder = new PlaceOrder(stock, payments, mailSender, orderStore, clock);
    }

    /** Returns {@code "OK:<orderId>"} or one of the legacy {@code "ERR:..."} codes. */
    public String process(OrderRequest req) {
        return switch (placeOrder.place(req)) {
            case PlaceOrderResult.Placed placed -> "OK:" + placed.orderId();
            case PlaceOrderResult.Rejected rejected -> "ERR:" + legacyCode(rejected);
        };
    }

    private static String legacyCode(PlaceOrderResult.Rejected rejected) {
        return switch (rejected.reason()) {
            case EMPTY -> "EMPTY";
            case INVALID_QUANTITY -> "INVALID_QUANTITY";
            case UNSUPPORTED_COUNTRY -> "UNSUPPORTED_COUNTRY";
            case INVALID_COUPON -> "INVALID_COUPON";
            case OUT_OF_STOCK -> "OUT_OF_STOCK:" + rejected.sku();
            case PAYMENT_DECLINED -> "PAYMENT_DECLINED";
        };
    }
}
