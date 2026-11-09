package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * A document with all its required approvals. An approved document always has at least one approver, which is
 * why the constructor checks it: an empty set would mean nobody reviewed the document.
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
        if (approvers.isEmpty()) {
            throw new IllegalArgumentException("An approved document needs at least one approver");
        }
    }

    public Published publish(Instant at) {
        Objects.requireNonNull(at, "at");
        return new Published(id, author, content, approvers, at);
    }
}
