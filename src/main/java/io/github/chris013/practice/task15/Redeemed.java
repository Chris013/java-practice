package io.github.chris013.practice.task15;

import java.math.BigDecimal;

/** The card covered the full amount. {@code remainingBalance} is what is still on the card. */
public record Redeemed(BigDecimal charged, BigDecimal remainingBalance) implements RedemptionResult {}
