package io.github.chris013.practice.task15;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RedemptionResultTest {

    @Nested
    class NullChecks {

        @Test
        void redeemed_rejects_null_amounts() {
            assertThatThrownBy(() -> new Redeemed(null, BigDecimal.ONE)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Redeemed(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void partially_redeemed_rejects_null_amounts() {
            assertThatThrownBy(() -> new PartiallyRedeemed(null, BigDecimal.ONE)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new PartiallyRedeemed(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void declined_rejects_a_null_reason() {
            assertThatThrownBy(() -> new Declined(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
