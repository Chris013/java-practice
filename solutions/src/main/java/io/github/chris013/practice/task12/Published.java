package io.github.chris013.practice.task12;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/** A document that is live. It has no transition methods: a published document is final. */
public record Published(DocumentId id, UserId author, Content content, Set<UserId> approvers, Instant publishedAt)
        implements Document {

    public Published {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(approvers, "approvers");
        Objects.requireNonNull(publishedAt, "publishedAt");
        approvers = Set.copyOf(approvers);
        Approvals.checkApprovers(author, content, approvers);
    }
}
