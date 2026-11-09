package io.github.chris013.practice.task25.domain;

import java.util.List;

/** Given. What happened when a subscription's period ended. */
public sealed interface RenewalOutcome permits RenewalOutcome.Renewed, RenewalOutcome.Ended {

    /** A new period has started. {@code lines} describe what to bill for it; empty for FREE. */
    record Renewed(List<InvoiceLine> lines) implements RenewalOutcome {

        public Renewed {
            lines = List.copyOf(lines);
        }
    }

    /** The customer had cancelled, so the subscription is now CANCELLED and nothing is billed. */
    record Ended() implements RenewalOutcome {}
}
