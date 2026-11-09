package io.github.chris013.practice.task10.domain;

/**
 * Given. Why a lending request was refused. One constant per rule, so a kiosk can show a helpful message and a
 * test can assert the exact rule that fired.
 */
public enum RefusalReason {
    UNKNOWN_MEMBER,
    MEMBER_BLOCKED,
    HAS_OVERDUE_LOANS,
    LOAN_LIMIT_REACHED,
    UNKNOWN_COPY,
    COPY_ALREADY_ON_LOAN,
    COPY_NOT_ON_LOAN,
    RENEWAL_NOT_ALLOWED
}
