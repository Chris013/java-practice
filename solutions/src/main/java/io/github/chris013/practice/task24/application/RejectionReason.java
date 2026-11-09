package io.github.chris013.practice.task24.application;

/**
 * Why an order was refused. A closed set, so the facade can switch over it exhaustively: when a new reason is
 * added, the compiler points at the legacy code string that has to be decided.
 */
public enum RejectionReason {
    EMPTY,
    INVALID_QUANTITY,
    UNSUPPORTED_COUNTRY,
    INVALID_COUPON,
    OUT_OF_STOCK,
    PAYMENT_DECLINED
}
