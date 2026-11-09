package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A document waiting for approvals. The approvals are copied and unmodifiable, so a caller that keeps a reference
 * to the set it passed in cannot change this review afterwards.
 */
public record InReview(DocumentId id, UserId author, Content content, Instant submittedAt, Set<UserId> approvals)
        implements Document, ReviewOutcome {

    public InReview {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(submittedAt, "submittedAt");
        Objects.requireNonNull(approvals, "approvals");
        approvals = Set.copyOf(approvals);
        if (approvals.contains(author)) {
            throw new IllegalArgumentException("The author cannot approve their own document (four-eyes principle)");
        }
        if (approvals.size() >= content.classification().requiredApprovals()) {
            throw new IllegalArgumentException("A review with all required approvals is an Approved document");
        }
    }

    /**
     * Adds one approval. Returns {@link InReview} while more approvals are needed, and {@link Approved} once
     * the classification's required number is reached. The approval that completes the set is the moment the
     * document changes state, so the type changes too.
     *
     * @throws IllegalArgumentException if the reviewer is the author (four-eyes principle) or has already approved
     */
    public ReviewOutcome approve(UserId reviewer, Instant at) {
        Objects.requireNonNull(reviewer, "reviewer");
        Objects.requireNonNull(at, "at");
        if (reviewer.equals(author)) {
            throw new IllegalArgumentException("The author cannot approve their own document (four-eyes principle)");
        }
        if (approvals.contains(reviewer)) {
            throw new IllegalArgumentException(reviewer.value() + " has already approved this document");
        }
        Set<UserId> updated = new HashSet<>(approvals);
        updated.add(reviewer);
        if (updated.size() < content.classification().requiredApprovals()) {
            return new InReview(id, author, content, submittedAt, updated);
        }
        return new Approved(id, author, content, updated, at);
    }

    /**
     * Rejects the document. The four-eyes rule and the reason check live in {@link Rejected}, so they hold for
     * every way of creating a rejection.
     *
     * @throws IllegalArgumentException if the reviewer is the author or the reason is blank
     */
    public Rejected reject(UserId reviewer, String reason, Instant at) {
        return new Rejected(id, author, content, reviewer, reason, at);
    }
}
