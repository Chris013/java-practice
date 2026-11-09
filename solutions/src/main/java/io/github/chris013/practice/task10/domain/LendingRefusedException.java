package io.github.chris013.practice.task10.domain;

import java.io.Serial;
import java.util.Objects;

/** Given. A business rule said no. The caller can show the reason to the member. */
public final class LendingRefusedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final RefusalReason reason;

    public LendingRefusedException(RefusalReason reason) {
        super(Objects.requireNonNull(reason, "reason").name());
        this.reason = reason;
    }

    /** A refusal that was caused by a lower-level failure. The cause is kept, so the stack trace shows both. */
    public LendingRefusedException(RefusalReason reason, Throwable cause) {
        super(Objects.requireNonNull(reason, "reason").name(), cause);
        this.reason = reason;
    }

    public RefusalReason reason() {
        return reason;
    }
}
