package io.github.chris013.practice.task14;

import java.util.Objects;
import java.util.Set;

/** {@code countries DE, AT}: on for visitors from these countries. Works for anonymous visitors too. */
record CountryRule(Set<String> countries) implements Rule {

    CountryRule {
        Objects.requireNonNull(countries, "countries");
        countries = Set.copyOf(countries);
    }

    @Override
    public boolean matches(EvaluationContext context, String flagName) {
        return context.country().filter(countries::contains).isPresent();
    }
}
