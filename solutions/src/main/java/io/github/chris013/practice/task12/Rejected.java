package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Objects;

/**
 * A review that ended with a rejection. The four-eyes rule and the reason check are in the constructor, so a
 * rejection that breaks them cannot exist.
 */
public record Rejected(
        DocumentId id, UserId author, Content content, UserId reviewer, String reason, Instant rejectedAt)
        implements Document {

    public Rejected {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(reviewer, "reviewer");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(rejectedAt, "rejectedAt");
        if (reviewer.equals(author)) {
            throw new IllegalArgumentException("The author cannot reject their own document (four-eyes principle)");
        }
        if (reason.isBlank()) {
            throw new IllegalArgumentException("A rejection needs a reason");
        }
    }

    /** Returns a new draft with the revised content. All earlier approvals are gone. */
    public Draft revise(Content newContent) {
        return new Draft(id, author, newContent);
    }
}
