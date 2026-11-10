package io.github.chris013.practice.task26;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A version number as defined by Semantic Versioning 2.0.0 (semver.org), for example {@code 2.0.0-rc.1+build.7}.
 *
 * <p>Why not keep versions as strings? {@code String.compareTo} puts {@code 1.10.0} before {@code 1.9.0}, and it
 * ranks {@code 2.0.0-rc.1} above {@code 2.0.0}. Both mistakes reached our customers.
 *
 * <p>{@link #compareTo} is consistent with {@link #equals}: it returns zero only for identical versions, because it
 * uses build metadata as the last tie-breaker. {@link #precedence()} ignores build metadata, so it is not consistent
 * with equals. Read its Javadoc before you put it into a sorted set or map.
 */
public record SemanticVersion(int major, int minor, int patch, List<String> preRelease, List<String> build)
        implements Comparable<SemanticVersion> {

    /** SemVer section 11, without the build metadata tie-breaker. */
    private static final Comparator<SemanticVersion> PRECEDENCE = Comparator.comparingInt(SemanticVersion::major)
            .thenComparingInt(SemanticVersion::minor)
            .thenComparingInt(SemanticVersion::patch)
            .thenComparing(SemanticVersion::preRelease, SemanticVersion::comparePreRelease);

    /** The natural order: precedence first, then build metadata. Consistent with equals. */
    private static final Comparator<SemanticVersion> FULL_ORDER =
            PRECEDENCE.thenComparing(SemanticVersion::build, SemanticVersion::compareBuild);

    /**
     * Validates the parts and copies the lists, so the record is immutable.
     *
     * @throws IllegalArgumentException if a number is negative or an identifier breaks the SemVer rules
     * @throws NullPointerException if a component is null
     */
    public SemanticVersion {
        requireNotNegative(major, "major");
        requireNotNegative(minor, "minor");
        requireNotNegative(patch, "patch");
        preRelease = List.copyOf(Objects.requireNonNull(preRelease, "preRelease"));
        build = List.copyOf(Objects.requireNonNull(build, "build"));
        checkIdentifiers(preRelease, "pre-release", true);
        checkIdentifiers(build, "build", false);
    }

    /** A stable version without pre-release or build metadata. */
    public static SemanticVersion of(int major, int minor, int patch) {
        return new SemanticVersion(major, minor, patch, List.of(), List.of());
    }

    /**
     * Parses exactly the SemVer 2.0.0 grammar. Anything else is rejected, including a {@code v} prefix and whitespace.
     *
     * <p>The parser splits the text by its separators ({@code +} first, then the first {@code -}) and lets the
     * constructor validate the identifiers. One regular expression would also work, but it could only say
     * "invalid version", and the message is the part a user actually reads.
     *
     * @throws IllegalArgumentException if the text is not a valid version; the message names the input
     */
    public static SemanticVersion parse(String text) {
        Objects.requireNonNull(text, "text");
        try {
            return parseOrThrow(text);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid version '" + text + "': " + e.getMessage(), e);
        }
    }

    private static SemanticVersion parseOrThrow(String text) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("version is empty");
        }
        String rest = text;
        List<String> build = List.of();
        int plus = rest.indexOf('+');
        if (plus >= 0) {
            build = splitIdentifiers(rest.substring(plus + 1));
            rest = rest.substring(0, plus);
        }
        List<String> preRelease = List.of();
        int dash = rest.indexOf('-');
        if (dash >= 0) {
            preRelease = splitIdentifiers(rest.substring(dash + 1));
            rest = rest.substring(0, dash);
        }
        // The core never contains '-' or '+', so the first separator of each kind is the right one.
        String[] core = rest.split("\\.", -1);
        if (core.length != 3) {
            throw new IllegalArgumentException("expected three numbers MAJOR.MINOR.PATCH, found " + core.length);
        }
        return new SemanticVersion(
                parseNumber(core[0], "major"),
                parseNumber(core[1], "minor"),
                parseNumber(core[2], "patch"),
                preRelease,
                build);
    }

    /** Positive integers only: no sign, no leading zeros, and it must fit an {@code int}. */
    private static int parseNumber(String digits, String part) {
        if (digits.isEmpty()) {
            throw new IllegalArgumentException("empty " + part);
        }
        for (char c : digits.toCharArray()) {
            if (!isAsciiDigit(c)) {
                throw new IllegalArgumentException("invalid character '" + c + "' in " + part);
            }
        }
        if (digits.length() > 1 && digits.charAt(0) == '0') {
            throw new IllegalArgumentException("leading zero in " + part);
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            // Without this, the caller would see a NumberFormatException, which is not the contract.
            throw new IllegalArgumentException(part + " is too large", e);
        }
    }

    private static List<String> splitIdentifiers(String dotted) {
        return List.of(dotted.split("\\.", -1));
    }

    private static void checkIdentifiers(List<String> identifiers, String kind, boolean forbidLeadingZeros) {
        for (String id : identifiers) {
            if (id.isEmpty()) {
                throw new IllegalArgumentException("empty " + kind + " identifier");
            }
            for (char c : id.toCharArray()) {
                if (!isIdentifierChar(c)) {
                    throw new IllegalArgumentException(
                            "invalid character '" + c + "' in " + kind + " identifier '" + id + "'");
                }
            }
            if (forbidLeadingZeros && allDigits(id) && id.length() > 1 && id.charAt(0) == '0') {
                throw new IllegalArgumentException("leading zero in " + kind + " identifier '" + id + "'");
            }
        }
    }

    private static void requireNotNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative, was " + value);
        }
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
        return PRECEDENCE;
    }

    /**
     * Compares precedence first and build metadata second, so that {@code compareTo} returns zero only for equal
     * versions. That makes it safe in sorted collections.
     */
    @Override
    public int compareTo(SemanticVersion other) {
        return FULL_ORDER.compare(this, other);
    }

    /** A version with a pre-release (such as {@code 1.0.0-rc.1}) is lower than the same version without one. */
    public boolean isPreRelease() {
        return !preRelease.isEmpty();
    }

    /** Major version 1 or higher, and no pre-release. Major 0 means "initial development" in SemVer. */
    public boolean isStable() {
        return major >= 1 && !isPreRelease();
    }

    /**
     * The next major release, following the npm rules. A pre-release such as {@code 2.0.0-rc.1} becomes
     * {@code 2.0.0}, because that release is the one the pre-release was heading for.
     *
     * @throws ArithmeticException if the major number would not fit an {@code int}
     */
    public SemanticVersion nextMajor() {
        if (isPreRelease() && minor == 0 && patch == 0) {
            return of(major, 0, 0);
        }
        return of(Math.incrementExact(major), 0, 0);
    }

    /**
     * The next minor release, following the npm rules. {@code 1.3.0-rc.1} becomes {@code 1.3.0}.
     *
     * @throws ArithmeticException if the minor number would not fit an {@code int}
     */
    public SemanticVersion nextMinor() {
        if (isPreRelease() && patch == 0) {
            return of(major, minor, 0);
        }
        return of(major, Math.incrementExact(minor), 0);
    }

    /**
     * The next patch release, following the npm rules. {@code 1.2.3-rc.1} becomes {@code 1.2.3}.
     *
     * @throws ArithmeticException if the patch number would not fit an {@code int}
     */
    public SemanticVersion nextPatch() {
        if (isPreRelease()) {
            return of(major, minor, patch);
        }
        return of(major, minor, Math.incrementExact(patch));
    }

    /** The canonical text, for example {@code 1.0.0-rc.1+build.7}. {@code parse(v.toString())} gives back {@code v}. */
    @Override
    public String toString() {
        StringBuilder text = new StringBuilder()
                .append(major).append('.').append(minor).append('.').append(patch);
        if (isPreRelease()) {
            text.append('-').append(String.join(".", preRelease));
        }
        if (!build.isEmpty()) {
            text.append('+').append(String.join(".", build));
        }
        return text.toString();
    }

    /** SemVer 11.4: an empty pre-release is the stable version, so it sorts higher. Lists are compared left to right. */
    private static int comparePreRelease(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return Boolean.compare(left.isEmpty(), right.isEmpty());
        }
        int common = Math.min(left.size(), right.size());
        for (int i = 0; i < common; i++) {
            int result = compareIdentifier(left.get(i), right.get(i));
            if (result != 0) {
                return result;
            }
        }
        // All shared identifiers are equal, so the longer list has more fields and is higher.
        return Integer.compare(left.size(), right.size());
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = allDigits(left);
        boolean rightNumeric = allDigits(right);
        if (leftNumeric && rightNumeric) {
            // No leading zeros, so a longer number is the bigger one. Comparing lengths first means we never
            // parse the number, and there is no overflow however long the identifier is.
            if (left.length() != right.length()) {
                return Integer.compare(left.length(), right.length());
            }
            return left.compareTo(right);
        }
        if (leftNumeric != rightNumeric) {
            // Numeric identifiers are lower than alphanumeric ones.
            return leftNumeric ? -1 : 1;
        }
        return left.compareTo(right);
    }

    /**
     * Build metadata is only a tie-breaker, so it has no meaning for ordering. We keep it simple: no build sorts
     * lower than any build, then identifiers compare as plain ASCII strings. That means {@code a.10} sorts before
     * {@code a.9}, which is correct by the rules and is documented in the task.
     */
    private static int compareBuild(List<String> left, List<String> right) {
        int common = Math.min(left.size(), right.size());
        for (int i = 0; i < common; i++) {
            int result = left.get(i).compareTo(right.get(i));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(left.size(), right.size());
    }

    private static boolean allDigits(String text) {
        if (text.isEmpty()) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (!isAsciiDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isIdentifierChar(char c) {
        return isAsciiDigit(c) || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '-';
    }
}
