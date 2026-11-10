package io.github.chris013.practice.task26;

/**
 * Which host versions a plugin supports, written in the npm style and simplified.
 *
 * <p>Accepted forms (exactly one of them): {@code *}, an exact version such as {@code 1.2.3} (or {@code =1.2.3}),
 * a caret range such as {@code ^1.2.3}, a tilde range such as {@code ~1.2.3}, or one or more space-separated
 * comparators such as {@code >=1.2.0 <1.5.0} that must all hold.
 *
 * <p>Pre-releases follow a simplified npm rule: a pre-release is included only when it satisfies the comparators and
 * at least one comparator is itself a pre-release with the same major.minor.patch.
 *
 * <p>Task 26: read {@code tasks/26-semantic-version.md} first.
 */
public final class VersionRange {

    private VersionRange() {
    }

    /**
     * Parses a range. Surrounding whitespace is ignored.
     *
     * @throws IllegalArgumentException if the range is malformed; the message names the input
     */
    public static VersionRange parse(String text) {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /** True when the version satisfies every comparator and the pre-release rule. */
    public boolean includes(SemanticVersion version) {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /** The original text, trimmed. */
    @Override
    public String toString() {
        throw new UnsupportedOperationException("TODO: Task 26");
    }
}
