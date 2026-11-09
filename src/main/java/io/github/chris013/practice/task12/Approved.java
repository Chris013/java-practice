package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Set;

/**
 * A document with all its required approvals. It can be published.
 *
 * <p>Rules, checked in the constructor: the author is not among the approvers (four-eyes principle), and there are
 * at least {@code content.classification().requiredApprovals()} approvers. The approvers are copied with
 * {@code Set.copyOf}.
 */
public record Approved(DocumentId id, UserId author, Content content, Set<UserId> approvers, Instant approvedAt)
        implements Document, ReviewOutcome {

    public Approved {
        throw new UnsupportedOperationException("TODO: Task 12");
    }

    public Published publish(Instant at) {
        throw new UnsupportedOperationException("TODO: Task 12");
    }
}
