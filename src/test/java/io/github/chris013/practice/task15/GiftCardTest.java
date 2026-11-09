package io.github.chris013.practice.task15;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GiftCardTest {

    private static final String PIN = "4711";
    private static final String WRONG_PIN = "1234";

    private static GiftCard card(String balance) {
        return new GiftCard(new GiftCardCode("ABCD-1234-EFGH"), PIN, new BigDecimal(balance), LocalDate.of(2026, 12, 31));
    }

    @Nested
    class Construction {

        @ParameterizedTest
        @ValueSource(strings = {"", "123", "12345", "12a4", " 4711"})
        void rejects_a_pin_that_is_not_exactly_four_digits(String pin) {
            assertThatThrownBy(() -> new GiftCard(new GiftCardCode("ABCD-1234-EFGH"), pin, BigDecimal.TEN, LocalDate.of(2026, 12, 31)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_negative_balance() {
            assertThatThrownBy(() -> card("-0.01"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_balance_with_more_than_two_decimals() {
            assertThatThrownBy(() -> card("10.005"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void accepts_a_zero_balance_and_keeps_two_decimals() {
            GiftCard card = card("0");

            assertThat(card.balance()).isEqualByComparingTo("0.00");
            assertThat(card.balance().scale()).isEqualTo(2);
        }

        @Test
        void rejects_nulls() {
            assertThatThrownBy(() -> new GiftCard(null, PIN, BigDecimal.TEN, LocalDate.of(2026, 12, 31)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new GiftCard(new GiftCardCode("ABCD-1234-EFGH"), null, BigDecimal.TEN, LocalDate.of(2026, 12, 31)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new GiftCard(new GiftCardCode("ABCD-1234-EFGH"), PIN, null, LocalDate.of(2026, 12, 31)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new GiftCard(new GiftCardCode("ABCD-1234-EFGH"), PIN, BigDecimal.TEN, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class PinAttempts {

        @Test
        void accepts_the_right_pin() {
            GiftCard card = card("10.00");

            assertThat(card.checkPin(PIN)).isTrue();
            assertThat(card.isBlocked()).isFalse();
        }

        @Test
        void rejects_a_wrong_pin() {
            GiftCard card = card("10.00");

            assertThat(card.checkPin(WRONG_PIN)).isFalse();
            assertThat(card.isBlocked()).isFalse();
        }

        @Test
        void blocks_the_card_on_the_third_consecutive_wrong_pin() {
            GiftCard card = card("10.00");

            card.checkPin(WRONG_PIN);
            card.checkPin(WRONG_PIN);
            assertThat(card.isBlocked()).isFalse();

            assertThat(card.checkPin(WRONG_PIN)).isFalse();
            assertThat(card.isBlocked()).isTrue();
        }

        @Test
        void a_right_pin_resets_the_counter_of_wrong_attempts() {
            GiftCard card = card("10.00");

            card.checkPin(WRONG_PIN);
            card.checkPin(WRONG_PIN);
            card.checkPin(PIN);
            card.checkPin(WRONG_PIN);
            card.checkPin(WRONG_PIN);

            assertThat(card.isBlocked()).isFalse();
            card.checkPin(WRONG_PIN);
            assertThat(card.isBlocked()).isTrue();
        }

        @Test
        void a_blocked_card_rejects_even_the_right_pin_and_stays_blocked() {
            GiftCard card = card("10.00");
            card.checkPin(WRONG_PIN);
            card.checkPin(WRONG_PIN);
            card.checkPin(WRONG_PIN);

            assertThat(card.checkPin(PIN)).isFalse();
            assertThat(card.isBlocked()).isTrue();
        }
    }

    @Nested
    class Debit {

        @Test
        void takes_the_amount_off_the_balance() {
            GiftCard card = card("10.00");

            card.debit(new BigDecimal("2.50"));

            assertThat(card.balance()).isEqualByComparingTo("7.50");
        }

        @Test
        void allows_debiting_the_whole_balance() {
            GiftCard card = card("10.00");

            card.debit(new BigDecimal("10.00"));

            assertThat(card.balance()).isEqualByComparingTo("0.00");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0.00", "-1.00"})
        void rejects_zero_and_negative_amounts(String amount) {
            GiftCard card = card("10.00");

            assertThatThrownBy(() -> card.debit(new BigDecimal(amount)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(card.balance()).isEqualByComparingTo("10.00");
        }

        @Test
        void rejects_an_amount_above_the_balance_and_keeps_the_balance() {
            GiftCard card = card("10.00");

            assertThatThrownBy(() -> card.debit(new BigDecimal("10.01")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(card.balance()).isEqualByComparingTo("10.00");
        }

        @Test
        void rejects_an_amount_with_more_than_two_decimals() {
            GiftCard card = card("10.00");

            assertThatThrownBy(() -> card.debit(new BigDecimal("1.005")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(card.balance()).isEqualByComparingTo("10.00");
        }
    }
}
