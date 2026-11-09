package io.github.chris013.practice.task15;

/** Why a gift card could not be used. Adding a constant here breaks every exhaustive switch on purpose. */
public enum DeclineReason {
    UNKNOWN_CARD,
    WRONG_PIN,
    CARD_BLOCKED,
    EXPIRED,
    EMPTY
}
