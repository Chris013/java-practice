package io.github.chris013.practice.task12;

/**
 * A policy document in one of its states. Each state is its own type, so an operation that is illegal in a
 * state does not exist on that state's type.
 *
 * <p>Task 12: read {@code tasks/12-document-approval-type-state.md} first.
 */
public sealed interface Document permits Draft, InReview, Approved, Rejected, Published {

    DocumentId id();

    UserId author();

    Content content();
}
