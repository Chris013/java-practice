package io.github.chris013.practice.task40;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Characterization tests. They describe what the code does today and must stay green through the whole refactoring.
 * Every expected number was worked out by hand, not copied from the implementation.
 */
class CouponServiceTest {

    private final CouponService service = new CouponService();

    @Nested
    class Normalize {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "' summer-sale 2O '|SUMMERSALE20",
                "'winter-o-2024'|WINTER02024",
                "'abc def'|ABCDEF",
                "'  o0-1  '|001",
                "'already-OK1'|ALREADY0K1"
        })
        void turns_raw_input_into_the_canonical_form(String raw, String expected) {
            assertThat(service.normalize(raw)).isEqualTo(expected);
        }
    }

    @Nested
    class IsValid {

        @ParameterizedTest
        @CsvSource({
                "ABCDE, false",          // 5 characters: too short
                "ABCDEF, true",          // 6 characters: the shortest allowed
                "ABCDEFGHIJK1, true",    // 12 characters: the longest allowed
                "ABCDEFGHIJKL1, false",  // 13 characters: too long
                "1ABCDE, false",         // starts with a digit
                "AB12CD, true",          // digits are fine after the first character
                "TEST00, false",         // on the blocklist
                "ADMIN0, false",         // on the blocklist
                "ADMIN1, true",          // only exact matches are blocked
                "summer20, false",       // lower case: isValid expects normalized input
                "AB-CDEF, false",        // dashes are removed by normalize, so they are not allowed here
                "ABCDE!, false",         // only upper-case letters and digits
                "'', false"              // empty: the first-character check must not run on it
        })
        void accepts_or_rejects_a_normalized_code(String code, boolean expected) {
            assertThat(service.isValid(code)).isEqualTo(expected);
        }
    }

    @Nested
    class Create {

        @Test
        void normalizes_the_code_and_keeps_the_value() {
            Coupon coupon = service.create(" summer-sale 2O ", DiscountType.PERCENT, new BigDecimal("15"));

            assertThat(coupon).isEqualTo(new Coupon("SUMMERSALE20", DiscountType.PERCENT, new BigDecimal("15")));
        }

        @Test
        void rejects_an_invalid_code_and_names_the_normalized_code_in_the_message() {
            assertThatThrownBy(() -> service.create("admin-0", DiscountType.FIXED, new BigDecimal("5")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ADMIN0");
        }

        @ParameterizedTest
        @CsvSource({
                "PERCENT, 1",
                "PERCENT, 100",
                "FIXED, 0.01",
                "FREE_SHIPPING, 0",
                "FREE_SHIPPING, 0.00"
        })
        void accepts_values_inside_the_rule_for_the_type(DiscountType type, String value) {
            assertThatCode(() -> service.create("CODE12", type, new BigDecimal(value)))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @CsvSource({
                "PERCENT, 0",
                "PERCENT, 101",
                "FIXED, 0",
                "FIXED, -1",
                "FREE_SHIPPING, 5"
        })
        void rejects_values_outside_the_rule_for_the_type(DiscountType type, String value) {
            assertThatThrownBy(() -> service.create("CODE12", type, new BigDecimal(value)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Apply {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                // 15% of 33.33 is 4.9995, rounded to 5.00; 33.33 - 5.00 = 28.33; + 4.90 shipping = 33.23
                "PERCENT|15|33.33|4.90|33.23",
                "PERCENT|100|33.33|4.90|4.90",
                // 10% of 10.25 is 1.025: HALF_UP gives 1.03 (HALF_EVEN would give 1.02); 10.25 - 1.03 = 9.22
                "PERCENT|10|10.25|0.00|9.22",
                // 10% of 20.00 is 2.00; 18.00 + 4.90 = 22.90
                "PERCENT|10|20.00|4.90|22.90",
                // 20.00 - 5 = 15.00; + 4.90 = 19.90
                "FIXED|5|20.00|4.90|19.90",
                // 20.00 - 25 is negative, so the total becomes 0.00; + 4.90 = 4.90
                "FIXED|25|20.00|4.90|4.90",
                // shipping is waived, so the total is unchanged
                "FREE_SHIPPING|0|20.00|4.90|20.00"
        })
        void computes_the_amount_to_pay(DiscountType type, String value, String total, String shipping, String expected) {
            Coupon coupon = new Coupon("CODE12", type, new BigDecimal(value));

            BigDecimal amount = service.apply(coupon, new BigDecimal(total), new BigDecimal(shipping));

            assertThat(amount).isEqualTo(new BigDecimal(expected));
        }

        @Test
        void always_returns_two_decimal_places() {
            Coupon coupon = new Coupon("CODE12", DiscountType.FIXED, new BigDecimal("5"));

            BigDecimal amount = service.apply(coupon, new BigDecimal("20"), new BigDecimal("5"));

            assertThat(amount).isEqualTo(new BigDecimal("20.00"));
        }
    }

    @Nested
    class Listeners {

        @Test
        void notifies_every_listener_in_registration_order_with_the_coupon_and_the_final_amount() {
            List<String> calls = new ArrayList<>();
            service.addListener((coupon, total) -> calls.add("first " + coupon.code() + " " + total));
            service.addListener((coupon, total) -> calls.add("second " + coupon.code() + " " + total));
            Coupon coupon = new Coupon("SUMMERSALE20", DiscountType.PERCENT, new BigDecimal("15"));

            service.apply(coupon, new BigDecimal("33.33"), new BigDecimal("4.90"));

            assertThat(calls).containsExactly("first SUMMERSALE20 33.23", "second SUMMERSALE20 33.23");
        }

        @Test
        void rejects_a_null_listener_without_adding_it() {
            assertThatThrownBy(() -> service.addListener(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("listener");
        }

        @Test
        void returns_the_amount_even_when_nobody_listens() {
            Coupon coupon = new Coupon("FREESHIP1", DiscountType.FREE_SHIPPING, new BigDecimal("0"));

            assertThat(service.apply(coupon, new BigDecimal("20.00"), new BigDecimal("4.90")))
                    .isEqualTo(new BigDecimal("20.00"));
        }
    }

    @Nested
    class SortForDisplay {

        @Test
        void sorts_by_type_then_value_highest_first_then_code() {
            Coupon tenPercentZulu = new Coupon("TENPCT", DiscountType.PERCENT, new BigDecimal("10"));
            Coupon tenPercentAlpha = new Coupon("ALPHA", DiscountType.PERCENT, new BigDecimal("10"));
            Coupon fifteenPercent = new Coupon("FIFTEEN", DiscountType.PERCENT, new BigDecimal("15"));
            Coupon fixedFive = new Coupon("FIVEOFF", DiscountType.FIXED, new BigDecimal("5"));
            Coupon fixedHundred = new Coupon("BIGFIXED", DiscountType.FIXED, new BigDecimal("100"));
            Coupon freeShipping = new Coupon("SHIPPING", DiscountType.FREE_SHIPPING, new BigDecimal("0"));

            List<Coupon> sorted = service.sortForDisplay(
                    List.of(tenPercentZulu, freeShipping, fixedFive, fifteenPercent, fixedHundred, tenPercentAlpha));

            // 100 comes before 5: a plain string comparison of the values would get this wrong
            assertThat(sorted).extracting(Coupon::code)
                    .containsExactly("FIFTEEN", "ALPHA", "TENPCT", "BIGFIXED", "FIVEOFF", "SHIPPING");
        }

        @Test
        void does_not_change_the_list_it_was_given() {
            Coupon fixed = new Coupon("FIVEOFF", DiscountType.FIXED, new BigDecimal("5"));
            Coupon percent = new Coupon("TENPCT", DiscountType.PERCENT, new BigDecimal("10"));
            List<Coupon> input = new ArrayList<>(List.of(fixed, percent));

            service.sortForDisplay(input);

            assertThat(input).containsExactly(fixed, percent);
        }
    }
}
