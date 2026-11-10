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

    private static final System.Logger LOG = System.getLogger(OrderProcessor.class.getName());

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

    /**
     * The whole checkout in one place. The order of the checks is the contract: empty items, quantity, country,
     * coupon, stock, payment, and only then the number, the row and the mail.
     */
    public String process(OrderRequest req) {
        LOG.log(System.Logger.Level.DEBUG, "Processing order for customer {0}", req.customerId());
        LOG.log(System.Logger.Level.DEBUG, "Request has {0} items, coupon {1}",
                req.items() == null ? 0 : req.items().size(), req.couponCode());

        // validation: an order needs at least one item, and every item needs a quantity from 1 to 10
        if (req.items() == null || req.items().isEmpty()) {
            LOG.log(System.Logger.Level.DEBUG, "Rejected: no items");
            return "ERR:EMPTY";
        }
        for (Item item : req.items()) {
            if (item.quantity() < 1 || item.quantity() > 10) {
                LOG.log(System.Logger.Level.DEBUG, "Rejected: quantity {0} for {1}", item.quantity(), item.sku());
                return "ERR:INVALID_QUANTITY";
            }
            LOG.log(System.Logger.Level.TRACE, "Item {0} accepted with quantity {1}", item.sku(), item.quantity());
        }

        // country: decides the shipping cost. Checked before the coupon, so a bad country is reported first.
        // Codes are matched exactly: "de" is not "DE".
        String country = req.country();
        boolean isDomestic = "DE".equals(country);
        boolean isNeighbour = "AT".equals(country) || "FR".equals(country) || "NL".equals(country);
        if (!isDomestic && !isNeighbour) {
            LOG.log(System.Logger.Level.DEBUG, "Rejected: unsupported country {0}", country);
            return "ERR:UNSUPPORTED_COUNTRY";
        }

        // subtotal. The lines of the confirmation mail are built in the same loop, so each line is formatted once.
        BigDecimal subtotal = BigDecimal.ZERO;
        StringBuilder lines = new StringBuilder();
        for (Item item : req.items()) {
            BigDecimal lineTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            subtotal = subtotal.add(lineTotal);
            LOG.log(System.Logger.Level.TRACE, "Line {0} totals {1}", item.sku(), lineTotal);
            lines.append(item.sku()).append(" x").append(item.quantity()).append(": ")
                    .append(String.format(Locale.GERMANY, "%.2f EUR", lineTotal)).append("\n");
        }

        // coupon: every code has its own rule, and each rule computes its own discount. An unknown or
        // too-early coupon is refused here, before any stock is touched.
        BigDecimal discount;
        BigDecimal afterDiscount;
        String code = req.couponCode();
        if ("SAVE10".equals(code)) {
            // 10 % off the subtotal, rounded to cents
            discount = subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
            afterDiscount = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        } else if ("FIVEOFF".equals(code)) {
            // 5.00 off, but only from a subtotal of 25.00
            if (subtotal.compareTo(new BigDecimal("25.00")) < 0) {
                LOG.log(System.Logger.Level.DEBUG, "Rejected: FIVEOFF below 25.00, subtotal {0}", subtotal);
                return "ERR:INVALID_COUPON";
            }
            discount = new BigDecimal("5.00").setScale(2, RoundingMode.HALF_UP);
            afterDiscount = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        } else if (code == null || code.isBlank()) {
            // no coupon
            discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            afterDiscount = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        } else {
            LOG.log(System.Logger.Level.DEBUG, "Rejected: unknown coupon {0}", code);
            return "ERR:INVALID_COUPON";
        }

        // shipping: free from 50.00, measured after the discount. Each country has its own cost.
        BigDecimal shipping;
        if (isDomestic) {
            if (afterDiscount.compareTo(new BigDecimal("50.00")) >= 0) {
                shipping = BigDecimal.ZERO;
            } else {
                shipping = new BigDecimal("4.90");
            }
        } else {
            if (afterDiscount.compareTo(new BigDecimal("50.00")) >= 0) {
                shipping = BigDecimal.ZERO;
            } else {
                shipping = new BigDecimal("9.90");
            }
        }

        // total, in cents. Rounded once here; the mail and the payment use this exact value.
        BigDecimal total = afterDiscount.add(shipping).setScale(2, RoundingMode.HALF_UP);
        LOG.log(System.Logger.Level.DEBUG, "Priced order: subtotal {0}, shipping {1}, total {2}",
                subtotal, shipping, total);

        // stock: check every line first, then reserve every line. A short line stops the order before any
        // reservation is made, so the warehouse never holds half an order.
        for (Item item : req.items()) {
            int available = stock.available(item.sku());
            if (available < item.quantity()) {
                LOG.log(System.Logger.Level.DEBUG, "Rejected: {0} is out of stock, {1} available",
                        item.sku(), available);
                return "ERR:OUT_OF_STOCK:" + item.sku();
            }
        }
        for (Item item : req.items()) {
            stock.reserve(item.sku(), item.quantity());
            LOG.log(System.Logger.Level.TRACE, "Reserved {0} x{1}", item.sku(), item.quantity());
        }

        // payment. A declined card is a normal outcome, not an exception for the caller.
        try {
            payments.charge(req.customerId(), total);
        } catch (PaymentDeclinedException declined) {
            LOG.log(System.Logger.Level.INFO, "Payment declined for customer {0}", req.customerId());
            return "ERR:PAYMENT_DECLINED";
        }

        // order number: one per placed order, counted per processor instance. It is only used up here, after
        // the payment went through, so a declined or rejected request never takes a number.
        orderCounter++;
        String orderId = "ORD-" + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + String.format(Locale.ROOT, "%04d", orderCounter);
        LOG.log(System.Logger.Level.DEBUG, "Placed order {0}", orderId);
        LOG.log(System.Logger.Level.DEBUG, "Charged {0} for customer {1}", total, req.customerId());

        // confirmation mail: the same numbers as the order, formatted for a German customer. The lines were
        // collected while pricing, so only the header and the totals are added here.
        StringBuilder body = new StringBuilder();
        body.append("Thank you for your order ").append(orderId).append(".\n");
        body.append(lines);
        body.append("Subtotal: ").append(String.format(Locale.GERMANY, "%.2f EUR", subtotal)).append("\n");
        body.append("Discount: ").append(String.format(Locale.GERMANY, "%.2f EUR", discount)).append("\n");
        body.append("Shipping: ").append(String.format(Locale.GERMANY, "%.2f EUR", shipping)).append("\n");
        body.append("Total: ").append(String.format(Locale.GERMANY, "%.2f EUR", total)).append("\n");

        // persist, then tell the customer. The mail is the last step, after the row is saved.
        orderStore.save(orderId, req.customerId(), total, "PAID");
        LOG.log(System.Logger.Level.DEBUG, "Saved order {0} as PAID", orderId);
        mailSender.send(req.email(), "Order " + orderId + " confirmed", body.toString());
        LOG.log(System.Logger.Level.DEBUG, "Confirmation mail sent for {0}", orderId);
        return "OK:" + orderId;
    }
}
