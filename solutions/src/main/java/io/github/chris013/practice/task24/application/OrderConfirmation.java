package io.github.chris013.practice.task24.application;

import io.github.chris013.practice.task24.domain.Money;
import io.github.chris013.practice.task24.domain.OrderLine;
import io.github.chris013.practice.task24.domain.OrderPricing;

import java.util.Locale;

/**
 * The customer's confirmation mail. It only formats numbers that were already computed by the pricing phase, so
 * the mail can never disagree with the amount that was charged.
 */
final class OrderConfirmation {

    private OrderConfirmation() {}

    static String subject(String orderId) {
        return "Order " + orderId + " confirmed";
    }

    static String body(String orderId, OrderQuote.Accepted order) {
        StringBuilder body = new StringBuilder();
        body.append("Thank you for your order ").append(orderId).append(".\n");
        for (OrderLine line : order.lines()) {
            body.append(line.sku()).append(" x").append(line.quantity()).append(": ")
                    .append(eur(line.lineTotal())).append("\n");
        }
        OrderPricing pricing = order.pricing();
        body.append("Subtotal: ").append(eur(pricing.subtotal())).append("\n");
        body.append("Discount: ").append(eur(pricing.discount())).append("\n");
        body.append("Shipping: ").append(eur(pricing.shipping())).append("\n");
        body.append("Total: ").append(eur(pricing.total())).append("\n");
        return body.toString();
    }

    /** German formatting for the customer, two decimals, straight from {@link java.math.BigDecimal}. */
    private static String eur(Money amount) {
        return String.format(Locale.GERMANY, "%.2f EUR", amount.amount());
    }
}
