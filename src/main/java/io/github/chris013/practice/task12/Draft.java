package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Set;

/** A document that its author is still writing. It cannot be published, and it has no approvals. */
public record Draft(DocumentId id, UserId author, Content content) implements Document {

    public Draft {
        throw new UnsupportedOperationException("TODO: Task 12");
    }

    public static Draft create(DocumentId id, UserId author, Content content) {
        throw new UnsupportedOperationException("TODO: Task 12");
    }

    /** Returns a new draft with different content. The original draft is not changed. */
    public Draft edit(Content newContent) {
        throw new UnsupportedOperationException("TODO: Task 12");
    }

    /** Sends the draft to review. The new review starts with no approvals. */
    public InReview submit(Instant at) {
        throw new UnsupportedOperationException("TODO: Task 12");
    }
}
