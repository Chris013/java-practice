package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * A document with all its required approvals. The constructor enforces the approval rules, so an approved document
 * cannot have its author among the approvers, and it cannot have fewer approvers than its classification requires.
 */
public record Approved(DocumentId id, UserId author, Content content, Set<UserId> approvers, Instant approvedAt)
        implements Document, ReviewOutcome {

    public Approved {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(approvers, "approvers");
        Objects.requireNonNull(approvedAt, "approvedAt");
        approvers = Set.copyOf(approvers);
        Approvals.checkApprovers(author, content, approvers);
    }

    public Published publish(Instant at) {
        Objects.requireNonNull(at, "at");
        return new Published(id, author, content, approvers, at);
    }
}
