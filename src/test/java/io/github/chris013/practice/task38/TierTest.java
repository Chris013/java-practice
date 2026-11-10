package io.github.chris013.practice.task38;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TierTest {

    @ParameterizedTest
    @CsvSource({"0, BRONZE", "999, BRONZE", "1000, SILVER", "4999, SILVER", "5000, GOLD", "120000, GOLD"})
    void maps_a_balance_to_its_tier_at_each_boundary(int points, Tier expected) {
        assertThat(Tier.forBalance(points)).isEqualTo(expected);
    }

    @Test
    void rejects_a_negative_balance() {
        assertThatThrownBy(() -> Tier.forBalance(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
