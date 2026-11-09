package io.github.chris013.practice.task15;

import java.math.BigDecimal;

/** The card had less than the amount. It is now empty; {@code stillToPay} must be paid another way. */
public record PartiallyRedeemed(BigDecimal charged, BigDecimal stillToPay) implements RedemptionResult {}
