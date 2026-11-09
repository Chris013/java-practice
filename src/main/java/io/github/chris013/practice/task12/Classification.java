package io.github.chris013.practice.task12;

/** How sensitive a document is. The classification decides how many reviewers must approve it. */
public enum Classification {
    PUBLIC(1),
    CONFIDENTIAL(2);

    private final int requiredApprovals;

    Classification(int requiredApprovals) {
        this.requiredApprovals = requiredApprovals;
    }

    /** The number of distinct reviewers who must approve a document with this classification. */
    public int requiredApprovals() {
        return requiredApprovals;
    }
}
