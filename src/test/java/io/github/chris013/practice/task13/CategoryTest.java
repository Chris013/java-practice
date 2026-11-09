package io.github.chris013.practice.task13;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CategoryTest {

    @Nested
    class VatAndAge {

        @ParameterizedTest
        @CsvSource({
            "FRUIT_AND_VEGETABLES, REDUCED, 0",
            "BAKERY, REDUCED, 0",
            "DAIRY, REDUCED, 0",
            "BOOKS, REDUCED, 0",
            "SOFT_DRINKS, STANDARD, 0",
            "BEER_AND_WINE, STANDARD, 16",
            "SPIRITS, STANDARD, 18",
            "TOBACCO, STANDARD, 18",
            "HOUSEHOLD, STANDARD, 0"
        })
        void has_the_vat_rate_and_minimum_age_from_the_table(Category category, VatRate rate, int minimumAge) {
            assertThat(category.vatRate()).isSameAs(rate);
            assertThat(category.minimumAge()).isEqualTo(minimumAge);
        }

        @ParameterizedTest
        @CsvSource({
            "FRUIT_AND_VEGETABLES, false",
            "SOFT_DRINKS, false",
            "BEER_AND_WINE, true",
            "SPIRITS, true",
            "TOBACCO, true"
        })
        void is_age_restricted_only_when_a_minimum_age_applies(Category category, boolean restricted) {
            assertThat(category.isAgeRestricted()).isEqualTo(restricted);
        }
    }
}
