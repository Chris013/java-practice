package io.github.chris013.practice.task12;

import java.time.Instant;

/**
 * A review that ended with a rejection. The author can revise it, which makes it a draft again.
 *
 * <p>Rules, checked in the constructor: the reviewer is not the author (four-eyes principle), and the reason is not
 * blank.
 */
public record Rejected(
        DocumentId id, UserId author, Content content, UserId reviewer, String reason, Instant rejectedAt)
        implements Document {

    public Rejected {
        throw new UnsupportedOperationException("TODO: Task 12");
    }

    /** Returns a new draft with the revised content. All earlier approvals are gone. */
    public Draft revise(Content newContent) {
        throw new UnsupportedOperationException("TODO: Task 12");
    }
}
