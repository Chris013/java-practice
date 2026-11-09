package io.github.chris013.practice.task14;

import java.util.List;
import java.util.Objects;

/** {@code countries DE, AT}: on for visitors from these countries. Works for anonymous visitors too. */
record CountryRule(List<String> countries) implements Rule {

    CountryRule {
        Objects.requireNonNull(countries, "countries");
        countries = List.copyOf(countries);
    }

    @Override
    public boolean matches(EvaluationContext context) {
        return context.country().filter(countries::contains).isPresent();
    }
}
