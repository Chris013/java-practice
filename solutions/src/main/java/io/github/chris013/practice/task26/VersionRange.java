package io.github.chris013.practice.task26;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Which host versions a plugin supports, written in the npm style and simplified.
 *
 * <p>Accepted forms (exactly one of them):
 * <ul>
 *   <li>{@code *}: every version without a pre-release;</li>
 *   <li>{@code 1.2.3}, or {@code =1.2.3}: exactly this version;</li>
 *   <li>{@code ^1.2.3}: compatible with 1.2.3, so {@code >=1.2.3 <2.0.0}. For major 0 the upper bound moves to the
 *       next minor ({@code ^0.2.3} is {@code <0.3.0}), and for {@code 0.0.x} to the next patch;</li>
 *   <li>{@code ~1.2.3}: patch changes only, {@code >=1.2.3 <1.3.0};</li>
 *   <li>one or more space-separated comparators, all of which must hold, for example {@code >=1.2.0 <1.5.0}.</li>
 * </ul>
 *
 * <p>Pre-releases follow a simplified npm rule: a pre-release is included only when it satisfies the comparators and
 * at least one comparator is itself a pre-release with the same major.minor.patch. So {@code ^1.2.3-beta.2} includes
 * {@code 1.2.3-beta.4} but not {@code 1.2.4-beta.1}, and {@code ^1.2.3} and {@code *} never include a pre-release.
 *
 * <p>Comparisons use {@link SemanticVersion#precedence()}, so build metadata is ignored.
 */
public final class VersionRange {

    private final String text;
    private final List<Bound> bounds;

    private VersionRange(String text, List<Bound> bounds) {
        this.text = text;
        this.bounds = bounds;
    }

    /**
     * Parses a range. Surrounding whitespace is ignored.
     *
     * @throws IllegalArgumentException if the range is malformed; the message names the input
     */
    public static VersionRange parse(String text) {
        Objects.requireNonNull(text, "text");
        String trimmed = text.strip();
        try {
            return new VersionRange(trimmed, parseBounds(trimmed));
        } catch (IllegalArgumentException | ArithmeticException e) {
            // ArithmeticException comes from bumping a major such as 2147483647 in a caret range.
            throw new IllegalArgumentException("invalid range '" + trimmed + "': " + e.getMessage(), e);
        }
    }

    /** True when the version satisfies every comparator and the pre-release rule. */
    public boolean includes(SemanticVersion version) {
        Objects.requireNonNull(version, "version");
        if (version.isPreRelease() && !allowsPreReleaseOf(version)) {
            return false;
        }
        for (Bound bound : bounds) {
            if (!bound.holds(version)) {
                return false;
            }
        }
        return true;
    }

    /** The original text, trimmed. */
    @Override
    public String toString() {
        return text;
    }

    private boolean allowsPreReleaseOf(SemanticVersion candidate) {
        return bounds.stream()
                .anyMatch(bound -> bound.version().isPreRelease() && sameCore(bound.version(), candidate));
    }

    private static boolean sameCore(SemanticVersion left, SemanticVersion right) {
        return left.major() == right.major() && left.minor() == right.minor() && left.patch() == right.patch();
    }

    private static List<Bound> parseBounds(String text) {
        if (text.isEmpty()) {
            throw new IllegalArgumentException("range is empty");
        }
        if (text.equals("*")) {
            return List.of();
        }
        if (text.charAt(0) == '^' || text.charAt(0) == '~') {
            String version = text.substring(1);
            if (version.chars().anyMatch(Character::isWhitespace)) {
                throw new IllegalArgumentException("'" + text.charAt(0) + "' takes exactly one version");
            }
            SemanticVersion base = SemanticVersion.parse(version);
            return text.charAt(0) == '^' ? caretBounds(base) : tildeBounds(base);
        }
        String[] tokens = text.split("\\s+");
        if (tokens.length == 1) {
            return List.of(parseComparator(tokens[0], false));
        }
        List<Bound> bounds = new ArrayList<>();
        for (String token : tokens) {
            bounds.add(parseComparator(token, true));
        }
        return List.copyOf(bounds);
    }

    /** {@code ^1.2.3} is {@code >=1.2.3 <2.0.0}. For major 0 the first non-zero part is the one that may not change. */
    private static List<Bound> caretBounds(SemanticVersion base) {
        SemanticVersion upper;
        if (base.major() > 0) {
            upper = SemanticVersion.of(Math.incrementExact(base.major()), 0, 0);
        } else if (base.minor() > 0) {
            upper = SemanticVersion.of(0, Math.incrementExact(base.minor()), 0);
        } else {
            upper = SemanticVersion.of(0, 0, Math.incrementExact(base.patch()));
        }
        return List.of(new Bound(Operator.GREATER_OR_EQUAL, base), new Bound(Operator.LESS, upper));
    }

    /** {@code ~1.2.3} is {@code >=1.2.3 <1.3.0}. */
    private static List<Bound> tildeBounds(SemanticVersion base) {
        SemanticVersion upper = SemanticVersion.of(base.major(), Math.incrementExact(base.minor()), 0);
        return List.of(new Bound(Operator.GREATER_OR_EQUAL, base), new Bound(Operator.LESS, upper));
    }

    /**
     * A comparator is an operator followed by a version, for example {@code >=1.2.0}. The operator may be left out
     * only when the range is a single version, which means exact match.
     */
    private static Bound parseComparator(String token, boolean operatorRequired) {
        for (Operator operator : Operator.values()) {
            // Declared with ">=" and "<=" before ">" and "<", so the longer symbol wins.
            if (token.startsWith(operator.symbol)) {
                return new Bound(operator, SemanticVersion.parse(token.substring(operator.symbol.length())));
            }
        }
        if (operatorRequired) {
            throw new IllegalArgumentException(
                    "comparator '" + token + "' needs an operator (>=, >, <=, <, =)");
        }
        return new Bound(Operator.EQUAL, SemanticVersion.parse(token));
    }

    private enum Operator {
        GREATER_OR_EQUAL(">="),
        LESS_OR_EQUAL("<="),
        GREATER(">"),
        LESS("<"),
        EQUAL("=");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        boolean accepts(int comparison) {
            return switch (this) {
                case GREATER_OR_EQUAL -> comparison >= 0;
                case LESS_OR_EQUAL -> comparison <= 0;
                case GREATER -> comparison > 0;
                case LESS -> comparison < 0;
                case EQUAL -> comparison == 0;
            };
        }
    }

    private record Bound(Operator operator, SemanticVersion version) {

        boolean holds(SemanticVersion candidate) {
            return operator.accepts(SemanticVersion.precedence().compare(candidate, version));
        }
    }
}
