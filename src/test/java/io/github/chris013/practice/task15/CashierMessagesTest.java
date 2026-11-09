package io.github.chris013.practice.task15;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CashierMessagesTest {

    @Nested
    class Redemptions {

        @Test
        void full_redemption_shows_the_charge_and_the_remaining_balance() {
            RedemptionResult result = new Redeemed(new BigDecimal("12.50"), new BigDecimal("7.50"));

            assertThat(CashierMessages.forResult(result))
                    .isEqualTo("Paid 12.50 EUR with gift card. Remaining balance: 7.50 EUR.");
        }

        @Test
        void partial_redemption_tells_the_cashier_what_to_collect() {
            RedemptionResult result = new PartiallyRedeemed(new BigDecimal("5.00"), new BigDecimal("7.50"));

            assertThat(CashierMessages.forResult(result))
                    .isEqualTo("Gift card covered 5.00 EUR. Please collect 7.50 EUR.");
        }

        @Test
        void amounts_always_show_two_decimals() {
            RedemptionResult result = new Redeemed(new BigDecimal("12.5"), new BigDecimal("7"));

            assertThat(CashierMessages.forResult(result))
                    .isEqualTo("Paid 12.50 EUR with gift card. Remaining balance: 7.00 EUR.");
        }
    }

    @Nested
    class Declines {

        @Test
        void unknown_card() {
            assertThat(message(DeclineReason.UNKNOWN_CARD)).isEqualTo("Gift card not found. Please check the code.");
        }

        @Test
        void wrong_pin() {
            assertThat(message(DeclineReason.WRONG_PIN)).isEqualTo("Wrong PIN. Please try again.");
        }

        @Test
        void blocked_card() {
            assertThat(message(DeclineReason.CARD_BLOCKED))
                    .isEqualTo("Gift card blocked after too many wrong PINs. Refer the customer to the service desk.");
        }

        @Test
        void expired_card() {
            assertThat(message(DeclineReason.EXPIRED)).isEqualTo("Gift card expired.");
        }

        @Test
        void empty_card() {
            assertThat(message(DeclineReason.EMPTY)).isEqualTo("Gift card has no balance left.");
        }

        @ParameterizedTest
        @EnumSource(DeclineReason.class)
        void every_decline_reason_has_a_message_for_the_cashier(DeclineReason reason) {
            assertThat(message(reason)).isNotBlank();
        }

        private String message(DeclineReason reason) {
            return CashierMessages.forResult(new Declined(reason));
        }
    }
}
