package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * A document that its author is still writing. It has no approvals and no publish method, so it cannot be
 * published without going through review first.
 *
 * <p>Task 12: read {@code tasks/12-document-approval-type-state.md} first.
 */
public record Draft(DocumentId id, UserId author, Content content) implements Document {

    public Draft {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(content, "content");
    }

    public static Draft create(DocumentId id, UserId author, Content content) {
        return new Draft(id, author, content);
    }

    /** Returns a new draft with different content. The original draft is not changed. */
    public Draft edit(Content newContent) {
        return new Draft(id, author, newContent);
    }

    /** Sends the draft to review. The new review starts with no approvals. */
    public InReview submit(Instant at) {
        Objects.requireNonNull(at, "at");
        return new InReview(id, author, content, at, Set.of());
    }
}
