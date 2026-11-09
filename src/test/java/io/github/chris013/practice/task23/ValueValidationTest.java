package io.github.chris013.practice.task23;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ValueValidationTest {

    private static final LocalDate DEPARTURE = LocalDate.of(2027, 3, 1);

    @ParameterizedTest
    @CsvSource({"am,LIS,1", "AMSS,LIS,1", "AM,LIS,1", "AMS,LIS,0", "AMS,LIS,10"})
    void rejects_a_bad_iata_code_or_passenger_count(String from, String to, int passengers) {
        assertThatThrownBy(() -> new TripRequest(from, to, DEPARTURE, passengers))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepts_the_boundaries_of_the_passenger_count() {
        new TripRequest("AMS", "LIS", DEPARTURE, 1);
        new TripRequest("AMS", "LIS", DEPARTURE, 9);
    }

    @ParameterizedTest
    @CsvSource({"0.00", "-1.00", "12", "12.5", "12.000"})
    void rejects_a_price_that_is_not_positive_with_exactly_two_decimals(String price) {
        assertThatThrownBy(() -> new Quote("TAP", new BigDecimal(price), "TP1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accepts_a_price_with_two_decimals() {
        new Quote("TAP", new BigDecimal("0.01"), "TP1");
    }
}
