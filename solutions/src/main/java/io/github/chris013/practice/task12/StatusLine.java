package io.github.chris013.practice.task12;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** A one-line summary of a document, for the list page of the policy tool. */
public final class StatusLine {

    private StatusLine() {}

    /**
     * Returns the summary line. One exhaustive switch with record patterns and no {@code default}: when a new
     * state is added, this method stops compiling until it is handled.
     */
    public static String of(Document document) {
        Objects.requireNonNull(document, "document");
        return switch (document) {
            case Draft(_, _, Content content) -> "Draft: " + content.title();
            case InReview(_, _, Content content, _, Set<UserId> approvals) -> "In review: %s (%d of %d approvals)"
                    .formatted(content.title(), approvals.size(), content.classification().requiredApprovals());
            case Approved(_, _, Content content, Set<UserId> approvers, _) ->
                    "Approved: %s by %s".formatted(content.title(), namesSorted(approvers));
            case Rejected(_, _, Content content, UserId reviewer, String reason, _) ->
                    "Rejected: %s by %s: %s".formatted(content.title(), reviewer.value(), reason);
            case Published(_, _, Content content, _, Instant publishedAt) ->
                    "Published: %s on %s".formatted(content.title(), publishedAt.atZone(ZoneOffset.UTC).toLocalDate());
        };
    }

    private static String namesSorted(Set<UserId> users) {
        return users.stream().map(UserId::value).sorted().collect(Collectors.joining(", "));
    }
}
