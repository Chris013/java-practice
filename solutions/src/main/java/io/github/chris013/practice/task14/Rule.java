package io.github.chris013.practice.task14;

/**
 * One condition that turns a flag on. The set of rule kinds is closed and known at compile time, so this is a sealed
 * interface: a {@code switch} over it can be checked by the compiler, and nobody outside the package can add a kind.
 */
sealed interface Rule permits UserRule, CountryRule, PercentRule {

    /** Whether this rule turns the flag on for the context. {@code flagName} is needed for percentage buckets. */
    boolean matches(EvaluationContext context, String flagName);
}
