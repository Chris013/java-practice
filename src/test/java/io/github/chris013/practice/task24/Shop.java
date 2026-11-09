package io.github.chris013.practice.task24;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * A test shop: all fakes wired into one {@link OrderProcessor}, with a small catalogue.
 * Tests change a fake (for example {@code shop.stock.with("TEA-GREEN", 1)}) when they need another state.
 */
final class Shop {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-01T10:15:00Z"), ZoneOffset.UTC);

    private static final Map<String, String> PRICES = Map.of(
            "TEA-GREEN", "10.00",
            "MUG-BLUE", "12.50",
            "BOOK-KAFKA", "30.00",
            "CANDLE-24", "24.99",
            "SOAP-10", "10.05");

    final FakeStock stock = catalogueInStock();
    final FakePayments payments = new FakePayments();
    final RecordingMailer mailer = new RecordingMailer();
    final InMemoryOrderStore store = new InMemoryOrderStore();
    final OrderProcessor processor = new OrderProcessor(stock, payments, mailer, store, CLOCK);

    /** A request for customer C-42 and e-mail anna@example.com. Pass {@code null} as coupon for none. */
    static OrderRequest order(String country, String couponCode, Item... items) {
        return new OrderRequest("C-42", "anna@example.com", country, List.of(items), couponCode);
    }

    /** An item at the catalogue price. */
    static Item item(String sku, int quantity) {
        return new Item(sku, quantity, new BigDecimal(PRICES.get(sku)));
    }

    private static FakeStock catalogueInStock() {
        FakeStock stock = new FakeStock();
        PRICES.keySet().forEach(sku -> stock.with(sku, 100));
        return stock;
    }
}
