package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Set;

/**
 * A document waiting for approvals. It is still open: more reviewers may approve it.
 *
 * <p>Rules, checked in the constructor: the author is not among the approvals (four-eyes principle), and there are
 * fewer approvals than the classification requires. A review that has all its approvals is an {@link Approved}.
 * The approvals are copied with {@code Set.copyOf}.
 */
public record InReview(DocumentId id, UserId author, Content content, Instant submittedAt, Set<UserId> approvals)
        implements Document, ReviewOutcome {

    public InReview {
        throw new UnsupportedOperationException("TODO: Task 12");
    }

    /**
     * Adds one approval. Returns {@link InReview} while more approvals are needed, and {@link Approved} once
     * the classification's required number is reached.
     *
     * @throws IllegalArgumentException if the reviewer is the author (four-eyes principle) or has already approved
     */
    public ReviewOutcome approve(UserId reviewer, Instant at) {
        throw new UnsupportedOperationException("TODO: Task 12");
    }

    /**
     * Rejects the document.
     *
     * @throws IllegalArgumentException if the reviewer is the author or the reason is blank
     */
    public Rejected reject(UserId reviewer, String reason, Instant at) {
        throw new UnsupportedOperationException("TODO: Task 12");
    }
}
