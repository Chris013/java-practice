package io.github.chris013.practice.task35;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Documents the bug in the legacy class. These tests pass on the starter and on the solution. */
class InheritedScanCounterTest {

    @Test
    void add_counts_one_attempt_for_one_scan() {
        InheritedScanCounter counter = new InheritedScanCounter();

        counter.add("4006381333931");

        assertThat(counter.addAttempts()).isEqualTo(1);
        assertThat(counter.size()).isEqualTo(1);
    }

    @Test
    void add_all_of_three_barcodes_counts_six_attempts() {
        InheritedScanCounter counter = new InheritedScanCounter();

        counter.addAll(List.of("4006381333931", "5000159484695", "8712100311009"));

        // Our addAll adds 3. Then HashSet's inherited addAll calls our add() once per element: 3 + 3.
        assertThat(counter.addAttempts()).isEqualTo(6);
        assertThat(counter.size()).isEqualTo(3);
    }
}
