package io.github.chris013.practice.task26;

import java.util.Comparator;
import java.util.List;

/**
 * A version number as defined by Semantic Versioning 2.0.0 (semver.org), for example {@code 2.0.0-rc.1+build.7}.
 *
 * <p>Why not keep versions as strings? {@code String.compareTo} puts {@code 1.10.0} before {@code 1.9.0}, and it
 * ranks {@code 2.0.0-rc.1} above {@code 2.0.0}.
 *
 * <p>Task 26: read {@code tasks/26-semantic-version.md} first.
 */
public record SemanticVersion(int major, int minor, int patch, List<String> preRelease, List<String> build)
        implements Comparable<SemanticVersion> {

    /**
     * Validates the parts and copies the lists, so the record is immutable.
     *
     * @throws IllegalArgumentException if a number is negative or an identifier breaks the SemVer rules
     * @throws NullPointerException if a component is null
     */
    public SemanticVersion {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /** A stable version without pre-release or build metadata. */
    public static SemanticVersion of(int major, int minor, int patch) {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /**
     * Parses exactly the SemVer 2.0.0 grammar. Anything else is rejected, including a {@code v} prefix and whitespace.
     *
     * @throws IllegalArgumentException if the text is not a valid version; the message names the input
     */
    public static SemanticVersion parse(String text) {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /**
     * SemVer precedence: major, minor and patch numerically, then pre-release, and build metadata is ignored.
     *
     * <p><b>Warning:</b> two versions that differ only in build metadata compare as equal here, but are not
     * {@link #equals}. Do not use this comparator in a {@code TreeSet} or as the key of a {@code TreeMap}: the set
     * would silently drop one of them. It is safe for sorting for display, for {@code max}/{@code min}, and for
     * range checks, where build metadata must not matter.
     */
    public static Comparator<SemanticVersion> precedence() {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /**
     * Precedence first, then build metadata, so that {@code compareTo} returns zero only for equal versions.
     */
    @Override
    public int compareTo(SemanticVersion other) {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /** A version with a pre-release (such as {@code 1.0.0-rc.1}) is lower than the same version without one. */
    public boolean isPreRelease() {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /** Major version 1 or higher, and no pre-release. */
    public boolean isStable() {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /**
     * The next major release, following the npm rules. A pre-release such as {@code 2.0.0-rc.1} becomes
     * {@code 2.0.0}.
     *
     * @throws ArithmeticException if the major number would not fit an {@code int}
     */
    public SemanticVersion nextMajor() {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /**
     * The next minor release, following the npm rules. {@code 1.3.0-rc.1} becomes {@code 1.3.0}.
     *
     * @throws ArithmeticException if the minor number would not fit an {@code int}
     */
    public SemanticVersion nextMinor() {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /**
     * The next patch release, following the npm rules. {@code 1.2.3-rc.1} becomes {@code 1.2.3}.
     *
     * @throws ArithmeticException if the patch number would not fit an {@code int}
     */
    public SemanticVersion nextPatch() {
        throw new UnsupportedOperationException("TODO: Task 26");
    }

    /** The canonical text, for example {@code 1.0.0-rc.1+build.7}. {@code parse(v.toString())} gives back {@code v}. */
    @Override
    public String toString() {
        throw new UnsupportedOperationException("TODO: Task 26");
    }
}
