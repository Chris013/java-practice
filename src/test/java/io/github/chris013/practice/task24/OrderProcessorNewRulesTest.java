package io.github.chris013.practice.task24;

import static io.github.chris013.practice.task24.Shop.item;
import static io.github.chris013.practice.task24.Shop.order;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The new requirement from the business: a declined card must not leave stock reserved.
 *
 * <p>The first test fails on the starter. That is the point: it is the bug, written down as a rule.
 */
class OrderProcessorNewRulesTest {

    @Nested
    class When_payment_is_declined {

        @Test
        void all_reserved_stock_is_released() {
            Shop shop = new Shop();
            shop.payments.declineNextCharges(1);

            shop.processor.process(order("DE", null, item("TEA-GREEN", 2), item("MUG-BLUE", 1)));

            assertThat(shop.stock.availableOf("TEA-GREEN")).isEqualTo(100);
            assertThat(shop.stock.availableOf("MUG-BLUE")).isEqualTo(100);
        }

        @Test
        void the_stock_is_free_again_for_the_next_customer() {
            Shop shop = new Shop();
            shop.stock.with("TEA-GREEN", 2);
            shop.payments.declineNextCharges(1);
            shop.processor.process(order("DE", null, item("TEA-GREEN", 2)));

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 2))))
                    .isEqualTo("OK:ORD-20260301-0001");
        }

        @Test
        void nothing_is_saved_and_no_mail_is_sent() {
            Shop shop = new Shop();
            shop.payments.declineNextCharges(1);

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 1))))
                    .isEqualTo("ERR:PAYMENT_DECLINED");

            assertThat(shop.store.saved()).isEmpty();
            assertThat(shop.mailer.sent()).isEmpty();
        }

        @Test
        void the_declined_attempt_does_not_use_up_an_order_number() {
            Shop shop = new Shop();
            shop.payments.declineNextCharges(1);
            shop.processor.process(order("DE", null, item("TEA-GREEN", 1)));

            assertThat(shop.processor.process(order("DE", null, item("TEA-GREEN", 1))))
                    .isEqualTo("OK:ORD-20260301-0001");
        }
    }
}
