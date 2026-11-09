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
            "FRUIT_AND_VEGETABLES, REDUCED, 0, false",
            "BAKERY, REDUCED, 0, false",
            "DAIRY, REDUCED, 0, false",
            "BOOKS, REDUCED, 0, false",
            "SOFT_DRINKS, STANDARD, 0, false",
            "BEER_AND_WINE, STANDARD, 16, true",
            "SPIRITS, STANDARD, 18, true",
            "TOBACCO, STANDARD, 18, true",
            "HOUSEHOLD, STANDARD, 0, false"
        })
        void has_the_vat_rate_minimum_age_and_restriction_from_the_table(
                Category category, VatRate rate, int minimumAge, boolean ageRestricted) {
            assertThat(category.vatRate()).isSameAs(rate);
            assertThat(category.minimumAge()).isEqualTo(minimumAge);
            assertThat(category.isAgeRestricted()).isEqualTo(ageRestricted);
        }
    }
}
