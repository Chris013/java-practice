package io.github.chris013.practice.task24;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Places webshop orders. Legacy code: every rule is inline in {@link #process}.
 *
 * <p>Task 24: read {@code tasks/24-god-class-breakup.md} first. The collaborators are already behind
 * interfaces, but the logic is still one method. Keep {@code OrderProcessorCharacterizationTest} green while
 * you split this class, and fix the stock bug only after the structure makes it obvious.
 *
 * <p>The return value is part of the contract with the checkout page: {@code "OK:<orderId>"} or one of the
 * {@code "ERR:..."} codes. Do not change those strings.
 */
public class OrderProcessor {

    private final StockService stock;
    private final PaymentGateway payments;
    private final MailSender mailSender;
    private final OrderStore orderStore;
    private final Clock clock;
    private int orderCounter;

    public OrderProcessor(StockService stock, PaymentGateway payments, MailSender mailSender,
            OrderStore orderStore, Clock clock) {
        this.stock = stock;
        this.payments = payments;
        this.mailSender = mailSender;
        this.orderStore = orderStore;
        this.clock = clock;
    }

    public String process(OrderRequest req) {
        // validation
        if (req.items() == null || req.items().isEmpty()) {
            return "ERR:EMPTY";
        }
        for (Item item : req.items()) {
            if (item.quantity() < 1 || item.quantity() > 10) {
                return "ERR:INVALID_QUANTITY";
            }
        }

        // country: decides the shipping cost
        String country = req.country();
        BigDecimal shippingCost;
        if ("DE".equals(country)) {
            shippingCost = new BigDecimal("4.90");
        } else if ("AT".equals(country) || "FR".equals(country) || "NL".equals(country)) {
            shippingCost = new BigDecimal("9.90");
        } else {
            return "ERR:UNSUPPORTED_COUNTRY";
        }

        // subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : req.items()) {
            subtotal = subtotal.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        // coupon
        BigDecimal discount = BigDecimal.ZERO;
        boolean hasCoupon = req.couponCode() != null && !req.couponCode().isBlank();
        if (hasCoupon) {
            String code = req.couponCode();
            if (code.equals("SAVE10")) {
                discount = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            } else if (code.equals("FIVEOFF")) {
                if (subtotal.compareTo(new BigDecimal("25.00")) >= 0) {
                    discount = new BigDecimal("5.00");
                } else {
                    return "ERR:INVALID_COUPON";
                }
            } else {
                return "ERR:INVALID_COUPON";
            }
        }

        // shipping: free from 50.00, measured after the discount
        BigDecimal afterDiscount = subtotal.subtract(discount);
        boolean freeShipping = afterDiscount.compareTo(new BigDecimal("50.00")) >= 0;
        BigDecimal shipping = freeShipping ? BigDecimal.ZERO : shippingCost;

        // total, in cents
        BigDecimal total = afterDiscount.add(shipping).setScale(2, RoundingMode.HALF_UP);

        // stock: check every line first, then reserve every line
        for (Item item : req.items()) {
            if (stock.available(item.sku()) < item.quantity()) {
                return "ERR:OUT_OF_STOCK:" + item.sku();
            }
        }
        for (Item item : req.items()) {
            stock.reserve(item.sku(), item.quantity());
        }

        // payment
        try {
            payments.charge(req.customerId(), total);
        } catch (PaymentDeclinedException declined) {
            return "ERR:PAYMENT_DECLINED";
        }

        // order number: one per placed order, counted per processor instance
        orderCounter++;
        String orderId = "ORD-" + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + String.format(Locale.ROOT, "%04d", orderCounter);

        // confirmation mail: the same numbers as the order, formatted for a German customer
        StringBuilder body = new StringBuilder();
        body.append("Thank you for your order ").append(orderId).append(".\n");
        for (Item item : req.items()) {
            BigDecimal lineTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            body.append(item.sku()).append(" x").append(item.quantity()).append(": ")
                    .append(String.format(Locale.GERMANY, "%.2f EUR", lineTotal)).append("\n");
        }
        body.append("Subtotal: ").append(String.format(Locale.GERMANY, "%.2f EUR", subtotal)).append("\n");
        body.append("Discount: ").append(String.format(Locale.GERMANY, "%.2f EUR", discount)).append("\n");
        body.append("Shipping: ").append(String.format(Locale.GERMANY, "%.2f EUR", shipping)).append("\n");
        body.append("Total: ").append(String.format(Locale.GERMANY, "%.2f EUR", total)).append("\n");

        orderStore.save(orderId, req.customerId(), total, "PAID");
        mailSender.send(req.email(), "Order " + orderId + " confirmed", body.toString());
        return "OK:" + orderId;
    }
}
