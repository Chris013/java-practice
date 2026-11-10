package io.github.chris013.practice.task24;

import static io.github.chris013.practice.task24.Shop.item;
import static io.github.chris013.practice.task24.Shop.order;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Pins down what {@link OrderProcessor} does today. Green before you start, green after every refactoring step.
 *
 * <p>Deliberately silent about stock after a declined payment: that is the bug, and the new-rules test covers it.
 */
class OrderProcessorCharacterizationTest {

    @Nested
    class Result_codes {

        @Test
        void an_order_without_items_is_rejected() {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("DE", null))).isEqualTo("ERR:EMPTY");
        }

        @Test
        void a_quantity_of_zero_is_rejected() {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 0))))
                    .isEqualTo("ERR:INVALID_QUANTITY");
        }

        @Test
        void a_quantity_of_eleven_is_rejected_but_ten_is_allowed() {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 11))))
                    .isEqualTo("ERR:INVALID_QUANTITY");
            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 10))))
                    .startsWith("OK:");
        }

        @Test
        void a_country_outside_the_four_supported_ones_is_rejected() {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("CH", null, item("TEA-GREEN", 1))))
                    .isEqualTo("ERR:UNSUPPORTED_COUNTRY");
        }

        @ParameterizedTest
        @CsvSource({"SAVE20", "save10", "FREESHIP"})
        void an_unknown_coupon_is_rejected(String coupon) {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("DE", coupon, item("TEA-GREEN", 1))))
                    .isEqualTo("ERR:INVALID_COUPON");
        }

        @Test
        void fiveoff_is_rejected_below_a_subtotal_of_25_euros() {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("DE", "FIVEOFF", item("CANDLE-24", 1))))
                    .isEqualTo("ERR:INVALID_COUPON");
        }

        @Test
        void an_item_that_is_out_of_stock_is_reported_by_sku() {
            Shop shop = new Shop();
            shop.stock.with("TEA-GREEN", 1);

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 2))))
                    .isEqualTo("ERR:OUT_OF_STOCK:TEA-GREEN");
        }

        @Test
        void the_first_item_without_stock_in_request_order_is_reported() {
            Shop shop = new Shop();
            shop.stock.with("TEA-GREEN", 0).with("BOOK-KAFKA", 0);

            assertThat(shop.processor.process(order("DE", null,
                    item("MUG-BLUE", 1), item("TEA-GREEN", 1), item("BOOK-KAFKA", 1))))
                    .isEqualTo("ERR:OUT_OF_STOCK:TEA-GREEN");
        }

        @Test
        void a_coupon_error_is_reported_before_a_stock_error() {
            Shop shop = new Shop();
            shop.stock.with("TEA-GREEN", 0);

            assertThat(shop.processor.process(order("DE", "SAVE20", item("TEA-GREEN", 1))))
                    .isEqualTo("ERR:INVALID_COUPON");
        }

        @Test
        void stock_is_checked_for_every_line_before_anything_is_reserved() {
            Shop shop = new Shop();
            shop.stock.with("TEA-GREEN", 0);

            shop.processor.process(order("DE", null, item("MUG-BLUE", 2), item("TEA-GREEN", 1)));

            assertThat(shop.stock.availableOf("MUG-BLUE")).isEqualTo(100);
            assertThat(shop.payments.charges()).isEmpty();
            assertThat(shop.store.saved()).isEmpty();
        }

        @Test
        void a_declined_payment_is_reported_as_such() {
            Shop shop = new Shop();
            shop.payments.declineNextCharges(1);

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 1))))
                    .isEqualTo("ERR:PAYMENT_DECLINED");
        }

        @Test
        void an_empty_order_is_reported_before_a_bad_country() {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("CH", null))).isEqualTo("ERR:EMPTY");
        }

        @Test
        void a_bad_quantity_is_reported_before_a_bad_country() {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("CH", null, item("TEA-GREEN", 0))))
                    .isEqualTo("ERR:INVALID_QUANTITY");
        }

        @Test
        void a_bad_country_is_reported_before_a_bad_coupon() {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("CH", "SAVE20", item("TEA-GREEN", 1))))
                    .isEqualTo("ERR:UNSUPPORTED_COUNTRY");
        }
    }

    @Nested
    class Totals {

        /** Subtotal, coupon and shipping decide the total. The last column is what the customer pays. */
        @ParameterizedTest(name = "{0} {1} {2} x{3} is {4}")
        @CsvSource({
            "DE,,TEA-GREEN,2,24.90",
            "AT,,TEA-GREEN,2,29.90",
            "FR,,MUG-BLUE,2,34.90",
            "NL,SAVE10,BOOK-KAFKA,1,36.90",
            "DE,,TEA-GREEN,4,44.90",
            "DE,,TEA-GREEN,5,50.00",
            "DE,SAVE10,BOOK-KAFKA,1,31.90",
            "DE,SAVE10,BOOK-KAFKA,2,54.00",
            "DE,SAVE10,SOAP-10,1,13.94",
            "DE,SAVE10,SOAP-10,5,50.12",
            "DE,FIVEOFF,MUG-BLUE,2,24.90",
            "NL,FIVEOFF,BOOK-KAFKA,1,34.90",
            "DE,FIVEOFF,BOOK-KAFKA,2,55.00",
            "NL,,BOOK-KAFKA,2,60.00",
            "DE,FIVEOFF,TEA-GREEN,5,49.90"
        })
        void the_total_is_subtotal_minus_coupon_plus_shipping(
                String country, String coupon, String sku, int quantity, String total) {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order(country, coupon, item(sku, quantity))))
                    .startsWith("OK:");
            assertThat(shop.store.saved()).singleElement()
                    .satisfies(saved -> assertThat(saved.total()).isEqualByComparingTo(total));
            assertThat(shop.payments.charges()).singleElement()
                    .satisfies(charge -> assertThat(charge.amount()).isEqualByComparingTo(total));
        }

        @Test
        void a_blank_coupon_is_the_same_as_no_coupon() {
            Shop shop = new Shop();

            shop.processor.process(order("DE", "   ", item("TEA-GREEN", 2)));

            assertThat(shop.store.saved().get(0).total()).isEqualByComparingTo("24.90");
        }

        @Test
        void fiveoff_takes_5_euros_off_at_a_subtotal_of_exactly_25_euros() {
            Shop shop = new Shop();

            shop.processor.process(order("DE", "FIVEOFF", item("MUG-BLUE", 2)));

            assertThat(shop.mailer.sent().get(0).body()).contains("Discount: 5,00 EUR");
        }
    }

    @Nested
    class Order_numbers {

        @Test
        void the_first_order_of_the_day_is_numbered_0001() {
            Shop shop = new Shop();

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 1))))
                    .isEqualTo("OK:ORD-20260301-0001");
        }

        @Test
        void numbers_count_up_for_each_placed_order() {
            Shop shop = new Shop();

            shop.processor.process(order("DE", null, item("TEA-GREEN", 1)));

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 1))))
                    .isEqualTo("OK:ORD-20260301-0002");
        }

        @Test
        void a_rejected_request_does_not_use_up_a_number() {
            Shop shop = new Shop();
            shop.processor.process(order("CH", null, item("TEA-GREEN", 1)));

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 1))))
                    .isEqualTo("OK:ORD-20260301-0001");
        }

        @Test
        void a_declined_payment_does_not_use_up_a_number() {
            Shop shop = new Shop();
            shop.payments.declineNextCharges(1);
            shop.processor.process(order("DE", null, item("TEA-GREEN", 1)));

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 1))))
                    .isEqualTo("OK:ORD-20260301-0001");
        }

        @Test
        void each_processor_counts_on_its_own() {
            assertThat(new Shop().processor.process(order("DE", null, item("TEA-GREEN", 1))))
                    .isEqualTo("OK:ORD-20260301-0001");
            assertThat(new Shop().processor.process(order("DE", null, item("TEA-GREEN", 1))))
                    .isEqualTo("OK:ORD-20260301-0001");
        }
    }

    @Nested
    class Side_effects {

        @Test
        void a_placed_order_reserves_its_stock() {
            Shop shop = new Shop();

            shop.processor.process(order("DE", null, item("TEA-GREEN", 2), item("MUG-BLUE", 1)));

            assertThat(shop.stock.availableOf("TEA-GREEN")).isEqualTo(98);
            assertThat(shop.stock.availableOf("MUG-BLUE")).isEqualTo(99);
        }

        @Test
        void a_placed_order_is_saved_as_paid_for_the_customer() {
            Shop shop = new Shop();

            shop.processor.process(order("DE", "SAVE10", item("TEA-GREEN", 2), item("MUG-BLUE", 1)));

            assertThat(shop.store.saved()).containsExactly(new InMemoryOrderStore.Saved(
                    "ORD-20260301-0001", "C-42", new BigDecimal("34.15"), "PAID"));
        }

        @Test
        void the_customer_gets_one_confirmation_mail_with_the_exact_text() {
            Shop shop = new Shop();

            shop.processor.process(order("DE", "SAVE10", item("TEA-GREEN", 2), item("MUG-BLUE", 1)));

            assertThat(shop.mailer.sent()).containsExactly(new RecordingMailer.Sent(
                    "anna@example.com",
                    "Order ORD-20260301-0001 confirmed",
                    """
                    Thank you for your order ORD-20260301-0001.
                    TEA-GREEN x2: 20,00 EUR
                    MUG-BLUE x1: 12,50 EUR
                    Subtotal: 32,50 EUR
                    Discount: 3,25 EUR
                    Shipping: 4,90 EUR
                    Total: 34,15 EUR
                    """));
        }

        @Test
        void the_customer_is_charged_the_total_once() {
            Shop shop = new Shop();

            shop.processor.process(order("DE", "SAVE10", item("TEA-GREEN", 2), item("MUG-BLUE", 1)));

            assertThat(shop.payments.charges())
                    .containsExactly(new FakePayments.Charge("C-42", new BigDecimal("34.15")));
        }

        @Test
        void a_subtotal_of_exactly_50_euros_ships_free_and_the_mail_says_so() {
            Shop shop = new Shop();

            shop.processor.process(order("DE", null, item("TEA-GREEN", 5)));

            assertThat(shop.mailer.sent().get(0).body()).isEqualTo("""
                    Thank you for your order ORD-20260301-0001.
                    TEA-GREEN x5: 50,00 EUR
                    Subtotal: 50,00 EUR
                    Discount: 0,00 EUR
                    Shipping: 0,00 EUR
                    Total: 50,00 EUR
                    """);
        }

        @Test
        void without_a_coupon_the_mail_shows_a_discount_of_zero() {
            Shop shop = new Shop();

            shop.processor.process(order("AT", null, item("TEA-GREEN", 1)));

            assertThat(shop.mailer.sent().get(0).body()).isEqualTo("""
                    Thank you for your order ORD-20260301-0001.
                    TEA-GREEN x1: 10,00 EUR
                    Subtotal: 10,00 EUR
                    Discount: 0,00 EUR
                    Shipping: 9,90 EUR
                    Total: 19,90 EUR
                    """);
        }
    }
}
