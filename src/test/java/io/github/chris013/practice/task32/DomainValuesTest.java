package io.github.chris013.practice.task32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DomainValuesTest {

    @Nested
    class CustomerIdRules {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejects_blank_values(String value) {
            assertThatThrownBy(() -> new CustomerId(value)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_null() {
            assertThatThrownBy(() -> new CustomerId(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void prints_as_its_value() {
            assertThat(new CustomerId("anna")).hasToString("anna");
        }
    }

    @Nested
    class AddressRules {

        @ParameterizedTest
        @ValueSource(strings = {"de", "DEU", "D", "D1", ""})
        void rejects_country_codes_that_are_not_two_upper_case_letters(String countryCode) {
            assertThatThrownBy(() -> new Address("Street 1", "10115", "Berlin", countryCode))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void accepts_a_two_letter_upper_case_country_code() {
            assertThat(new Address("Street 1", "10115", "Berlin", "DE").countryCode()).isEqualTo("DE");
        }

        @Test
        void rejects_a_null_part() {
            assertThatThrownBy(() -> new Address(null, "10115", "Berlin", "DE"))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class CustomerOptionals {

        @Test
        void wraps_missing_optional_facts_as_empty() {
            Customer customer = new Customer(new CustomerId("ben"), "Ben", null, null);

            assertThat(customer.homeAddress()).isEmpty();
            assertThat(customer.preferredPickupPointId()).isEmpty();
        }

        @Test
        void exposes_present_optional_facts() {
            Address home = new Address("Street 1", "10115", "Berlin", "DE");
            Customer customer = new Customer(new CustomerId("anna"), "Anna", home, "PP-1");

            assertThat(customer.homeAddress()).contains(home);
            assertThat(customer.preferredPickupPointId()).contains("PP-1");
        }
    }
}
