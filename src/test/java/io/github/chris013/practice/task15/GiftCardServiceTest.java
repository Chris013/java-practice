package io.github.chris013.practice.task15;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GiftCardServiceTest {

    private static final GiftCardCode CODE = new GiftCardCode("ABCD-1234-EFGH");
    private static final String PIN = "4711";
    private static final String WRONG_PIN = "1234";
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);
    private static final Instant NOON_TODAY = Instant.parse("2026-03-01T12:00:00Z");

    private final CountingGiftCardRepository repository = new CountingGiftCardRepository();

    private GiftCardService serviceAt(Instant now) {
        return new GiftCardService(repository, Clock.fixed(now, ZoneOffset.UTC));
    }

    private GiftCardService service() {
        return serviceAt(NOON_TODAY);
    }

    private GiftCard giveCard(String balance, LocalDate validUntil) {
        GiftCard card = new GiftCard(CODE, PIN, new BigDecimal(balance), validUntil);
        repository.seed(card);
        return card;
    }

    private GiftCard giveValidCard(String balance) {
        return giveCard(balance, TODAY.plusMonths(6));
    }

    private BigDecimal balanceOnCard() {
        return repository.findByCode(CODE).orElseThrow().balance();
    }

    @Nested
    class Outcomes {

        @Test
        void redeems_the_full_amount_when_the_card_covers_it() {
            giveValidCard("20.00");

            RedemptionResult result = service().redeem(CODE, PIN, new BigDecimal("12.50"));

            assertThat(result).isEqualTo(new Redeemed(new BigDecimal("12.50"), new BigDecimal("7.50")));
            assertThat(balanceOnCard()).isEqualByComparingTo("7.50");
        }

        @Test
        void redeems_an_exact_balance_and_leaves_zero() {
            giveValidCard("20.00");

            RedemptionResult result = service().redeem(CODE, PIN, new BigDecimal("20.00"));

            assertThat(result).isEqualTo(new Redeemed(new BigDecimal("20.00"), new BigDecimal("0.00")));
        }

        @Test
        void partially_redeems_and_empties_the_card_when_the_balance_is_too_small() {
            giveValidCard("5.00");

            RedemptionResult result = service().redeem(CODE, PIN, new BigDecimal("12.50"));

            assertThat(result).isEqualTo(new PartiallyRedeemed(new BigDecimal("5.00"), new BigDecimal("7.50")));
            assertThat(balanceOnCard()).isEqualByComparingTo("0.00");
        }

        @Test
        void declines_an_unknown_card() {
            RedemptionResult result = service().redeem(CODE, PIN, BigDecimal.ONE);

            assertThat(result).isEqualTo(new Declined(DeclineReason.UNKNOWN_CARD));
        }

        @Test
        void declines_a_wrong_pin_and_leaves_the_balance_alone() {
            giveValidCard("20.00");

            RedemptionResult result = service().redeem(CODE, WRONG_PIN, BigDecimal.ONE);

            assertThat(result).isEqualTo(new Declined(DeclineReason.WRONG_PIN));
            assertThat(balanceOnCard()).isEqualByComparingTo("20.00");
        }

        @Test
        void reports_the_third_wrong_pin_as_a_blocked_card() {
            giveValidCard("20.00");
            GiftCardService service = service();

            assertThat(service.redeem(CODE, WRONG_PIN, BigDecimal.ONE)).isEqualTo(new Declined(DeclineReason.WRONG_PIN));
            assertThat(service.redeem(CODE, WRONG_PIN, BigDecimal.ONE)).isEqualTo(new Declined(DeclineReason.WRONG_PIN));
            assertThat(service.redeem(CODE, WRONG_PIN, BigDecimal.ONE)).isEqualTo(new Declined(DeclineReason.CARD_BLOCKED));
        }

        @Test
        void a_blocked_card_stays_blocked_even_with_the_right_pin() {
            giveValidCard("20.00");
            GiftCardService service = service();
            service.redeem(CODE, WRONG_PIN, BigDecimal.ONE);
            service.redeem(CODE, WRONG_PIN, BigDecimal.ONE);
            service.redeem(CODE, WRONG_PIN, BigDecimal.ONE);

            RedemptionResult result = service.redeem(CODE, PIN, BigDecimal.ONE);

            assertThat(result).isEqualTo(new Declined(DeclineReason.CARD_BLOCKED));
            assertThat(balanceOnCard()).isEqualByComparingTo("20.00");
        }

        @Test
        void declines_an_expired_card_without_charging_it() {
            giveCard("20.00", TODAY.minusDays(1));

            RedemptionResult result = service().redeem(CODE, PIN, BigDecimal.ONE);

            assertThat(result).isEqualTo(new Declined(DeclineReason.EXPIRED));
            assertThat(balanceOnCard()).isEqualByComparingTo("20.00");
        }

        @Test
        void declines_an_empty_card() {
            giveValidCard("0.00");

            RedemptionResult result = service().redeem(CODE, PIN, BigDecimal.ONE);

            assertThat(result).isEqualTo(new Declined(DeclineReason.EMPTY));
        }
    }

    @Nested
    class ExpiryBoundary {

        @Test
        void a_card_is_valid_on_its_last_day_until_midnight() {
            giveCard("20.00", TODAY);

            RedemptionResult result = serviceAt(Instant.parse("2026-03-01T23:59:59Z")).redeem(CODE, PIN, BigDecimal.ONE);

            assertThat(result).isEqualTo(new Redeemed(new BigDecimal("1.00"), new BigDecimal("19.00")));
        }

        @Test
        void a_card_has_expired_the_day_after_its_last_day() {
            giveCard("20.00", TODAY);

            RedemptionResult result = serviceAt(Instant.parse("2026-03-02T00:00:00Z")).redeem(CODE, PIN, BigDecimal.ONE);

            assertThat(result).isEqualTo(new Declined(DeclineReason.EXPIRED));
        }
    }

    @Nested
    class CheckOrder {

        @Test
        void a_wrong_pin_is_reported_before_expiry_and_still_counts() {
            giveCard("20.00", TODAY.minusDays(1));
            GiftCardService service = service();

            assertThat(service.redeem(CODE, WRONG_PIN, BigDecimal.ONE)).isEqualTo(new Declined(DeclineReason.WRONG_PIN));
            assertThat(repository.saveCount()).isEqualTo(1);

            service.redeem(CODE, WRONG_PIN, BigDecimal.ONE);
            assertThat(service.redeem(CODE, WRONG_PIN, BigDecimal.ONE)).isEqualTo(new Declined(DeclineReason.CARD_BLOCKED));
        }

        @Test
        void a_right_pin_on_an_expired_card_resets_the_counter_and_is_saved() {
            giveCard("20.00", TODAY.minusDays(1));
            GiftCardService service = service();
            service.redeem(CODE, WRONG_PIN, BigDecimal.ONE);
            service.redeem(CODE, WRONG_PIN, BigDecimal.ONE);

            assertThat(service.redeem(CODE, PIN, BigDecimal.ONE)).isEqualTo(new Declined(DeclineReason.EXPIRED));
            assertThat(repository.saveCount()).isEqualTo(3);

            assertThat(service.redeem(CODE, WRONG_PIN, BigDecimal.ONE)).isEqualTo(new Declined(DeclineReason.WRONG_PIN));
            assertThat(service.redeem(CODE, WRONG_PIN, BigDecimal.ONE)).isEqualTo(new Declined(DeclineReason.WRONG_PIN));
        }

        @Test
        void expiry_is_reported_before_an_empty_balance() {
            giveCard("0.00", TODAY.minusDays(1));

            RedemptionResult result = service().redeem(CODE, PIN, BigDecimal.ONE);

            assertThat(result).isEqualTo(new Declined(DeclineReason.EXPIRED));
        }

        @Test
        void an_unknown_card_is_reported_before_anything_else() {
            RedemptionResult result = service().redeem(CODE, WRONG_PIN, BigDecimal.ONE);

            assertThat(result).isEqualTo(new Declined(DeclineReason.UNKNOWN_CARD));
        }
    }

    @Nested
    class Persistence {

        @Test
        void saves_the_card_after_a_wrong_pin() {
            giveValidCard("20.00");

            service().redeem(CODE, WRONG_PIN, BigDecimal.ONE);

            assertThat(repository.saveCount()).isEqualTo(1);
        }

        @Test
        void saves_the_card_after_a_debit() {
            giveValidCard("20.00");

            service().redeem(CODE, PIN, BigDecimal.ONE);

            assertThat(repository.saveCount()).isPositive();
            assertThat(repository.lastSavedBalance()).isEqualByComparingTo("19.00");
            assertThat(balanceOnCard()).isEqualByComparingTo("19.00");
        }

        @Test
        void does_not_save_anything_for_an_unknown_card() {
            service().redeem(CODE, PIN, BigDecimal.ONE);

            assertThat(repository.saveCount()).isZero();
        }
    }

    @Nested
    class Amounts {

        @Test
        void accepts_trailing_zeros_in_the_amount() {
            giveValidCard("20.00");

            RedemptionResult result = service().redeem(CODE, PIN, new BigDecimal("12.500"));

            assertThat(result).isEqualTo(new Redeemed(new BigDecimal("12.50"), new BigDecimal("7.50")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0.00", "-1.00"})
        void rejects_zero_and_negative_amounts(String amount) {
            giveValidCard("20.00");

            assertThatThrownBy(() -> service().redeem(CODE, PIN, new BigDecimal(amount)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(balanceOnCard()).isEqualByComparingTo("20.00");
        }

        @Test
        void rejects_an_amount_with_more_than_two_decimals() {
            giveValidCard("20.00");

            assertThatThrownBy(() -> service().redeem(CODE, PIN, new BigDecimal("12.505")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(balanceOnCard()).isEqualByComparingTo("20.00");
        }

        @Test
        void rejects_null_arguments_with_a_null_pointer_exception() {
            giveValidCard("20.00");
            GiftCardService service = service();

            assertThatThrownBy(() -> service.redeem(null, PIN, BigDecimal.ONE)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> service.redeem(CODE, null, BigDecimal.ONE)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> service.redeem(CODE, PIN, null)).isInstanceOf(NullPointerException.class);
        }
    }
}
