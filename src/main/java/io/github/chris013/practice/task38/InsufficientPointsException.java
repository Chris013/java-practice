package io.github.chris013.practice.task38;

import java.io.Serial;

/** Thrown when a member tries to redeem more points than they have. */
public final class InsufficientPointsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String member;
    private final int balance;
    private final int requested;

    public InsufficientPointsException(String member, int balance, int requested) {
        super("%s has %d points, cannot redeem %d".formatted(member, balance, requested));
        this.member = member;
        this.balance = balance;
        this.requested = requested;
    }

    public String member() {
        return member;
    }

    public int balance() {
        return balance;
    }

    public int requested() {
        return requested;
    }
}
