package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Set;

/** A document with all its required approvals. It can be published. */
public record Approved(DocumentId id, UserId author, Content content, Set<UserId> approvers, Instant approvedAt)
        implements Document, ReviewOutcome {

    public Approved {
        throw new UnsupportedOperationException("TODO: Task 12");
    }

    public Published publish(Instant at) {
        throw new UnsupportedOperationException("TODO: Task 12");
    }
}
