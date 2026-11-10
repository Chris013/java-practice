package io.github.chris013.practice.task27;

import java.util.List;

/**
 * What validation made of one row: either a product to save, or the list of problems with it. A sealed type
 * makes the caller handle both cases, and the list of errors is never null or empty.
 */
sealed interface RowOutcome permits RowOutcome.Valid, RowOutcome.Invalid {

    record Valid(Product product) implements RowOutcome {}

    record Invalid(List<RowError> errors) implements RowOutcome {

        public Invalid {
            errors = List.copyOf(errors);
        }
    }
}
