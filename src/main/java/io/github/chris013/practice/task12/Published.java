package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Set;

/**
 * A document that is live for everyone it applies to. Nothing can be done to it any more.
 *
 * <p>Rules, checked in the constructor: the same as for {@link Approved}. The author is not among the approvers,
 * and there are enough approvers for the classification.
 */
public record Published(DocumentId id, UserId author, Content content, Set<UserId> approvers, Instant publishedAt)
        implements Document {

    public Published {
        throw new UnsupportedOperationException("TODO: Task 12");
    }
}
