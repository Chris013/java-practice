package io.github.chris013.practice.task24.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** The pricing phase is pure, so it needs no fakes: numbers in, numbers out. */
class OrderPricingTest {

    private static Money eur(String amount) {
        return new Money(new BigDecimal(amount));
    }

    @Nested
    class Coupons {

        @ParameterizedTest
        @CsvSource({"SAVE10", "FIVEOFF"})
        void a_known_code_is_recognised(String code) {
            assertThat(Coupon.parse(code)).isPresent();
        }

        @ParameterizedTest
        @CsvSource({"save10", "SAVE20", "FREESHIP", "' SAVE10'"})
        void an_unknown_code_is_not_a_coupon(String code) {
            assertThat(Coupon.parse(code)).isEmpty();
        }

        @Test
        void a_blank_code_means_no_coupon() {
            assertThat(Coupon.parse("   ")).containsInstanceOf(Coupon.NoCoupon.class);
        }

        @Test
        void a_null_code_means_no_coupon() {
            assertThat(Coupon.parse(null)).containsInstanceOf(Coupon.NoCoupon.class);
        }

        @Test
        void fiveoff_applies_from_25_euros_and_not_below() {
            Coupon fiveOff = Coupon.parse("FIVEOFF").orElseThrow();

            assertThat(fiveOff.appliesTo(eur("24.99"))).isFalse();
            assertThat(fiveOff.appliesTo(eur("25.00"))).isTrue();
        }
    }

    @Nested
    class Pricing {

        @Test
        void a_percentage_is_rounded_half_up_to_cents() {
            // 10.05 x 10 % = 1.005, which rounds half up to 1.01
            assertThat(new Coupon.Percentage(10).discountOn(eur("10.05"))).isEqualTo(eur("1.01"));
        }

        @Test
        void shipping_is_free_at_exactly_50_euros_after_the_discount() {
            OrderPricing pricing = OrderPricing.calculate(eur("55.56"), Country.DE,
                    new Coupon.Percentage(10));

            // 55.56 - 5.556 (rounds to 5.56) = 50.00, which is free shipping
            assertThat(pricing.shipping()).isEqualTo(Money.ZERO);
            assertThat(pricing.total()).isEqualTo(eur("50.00"));
        }

        @Test
        void shipping_is_charged_just_below_the_threshold() {
            OrderPricing pricing = OrderPricing.calculate(eur("49.99"), Country.FR, new Coupon.NoCoupon());

            assertThat(pricing.shipping()).isEqualTo(eur("9.90"));
            assertThat(pricing.total()).isEqualTo(eur("59.89"));
        }
    }

    @Nested
    class Guards {

        @Test
        void an_order_line_cannot_have_zero_or_eleven_items() {
            assertThatThrownBy(() -> new OrderLine("TEA-GREEN", 0, eur("10.00")))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new OrderLine("TEA-GREEN", 11, eur("10.00")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void country_codes_are_exact() {
            assertThat(Country.fromCode("de")).isEmpty();
            assertThat(Country.fromCode(null)).isEmpty();
            assertThat(Country.fromCode("DE")).contains(Country.DE);
        }
    }
}
