package io.github.chris013.practice.task12;

import java.util.Set;

/**
 * The approval rules that {@link Approved} and {@link Published} share. Package-private: the rules are enforced in
 * the constructors, and callers never need this class.
 */
final class Approvals {

    private Approvals() {}

    /**
     * Checks the approvers of a document that has been approved or published. The author cannot be one of them,
     * and there must be as many approvers as the classification requires.
     *
     * @throws IllegalArgumentException if the author is an approver or there are too few approvers
     */
    static void checkApprovers(UserId author, Content content, Set<UserId> approvers) {
        if (approvers.contains(author)) {
            throw new IllegalArgumentException("The author cannot approve their own document (four-eyes principle)");
        }
        int required = content.classification().requiredApprovals();
        if (approvers.size() < required) {
            throw new IllegalArgumentException(
                    "A %s document needs %d approvals".formatted(content.classification(), required));
        }
    }
}
