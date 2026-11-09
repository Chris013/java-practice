package io.github.chris013.practice.task16;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A composite promotion: "the better of two offers". Every option is evaluated against the same basket and only
 * the best discount is used. A {@code BestOf} can contain another {@code BestOf}, and then the inner winner
 * competes as one option.
 *
 * <p>The winning {@link Discount} is returned unchanged, so the receipt names the promotion that was actually
 * applied, not the wrapper. For nested composites that is the innermost winner.
 */
public final class BestOf implements Promotion {

    private final String name;
    private final List<Promotion> options;

    public BestOf(String name, List<Promotion> options) {
        this.name = Objects.requireNonNull(name, "name");
        this.options = List.copyOf(Objects.requireNonNull(options, "options"));
        if (this.options.isEmpty()) {
            throw new IllegalArgumentException("A BestOf needs at least one option");
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<Discount> evaluate(Basket basket) {
        Discount best = null;
        for (Promotion option : options) {
            Optional<Discount> candidate = option.evaluate(basket);
            // Strictly greater: on a tie the option listed first keeps the win.
            if (candidate.isPresent() && (best == null || candidate.get().amount().compareTo(best.amount()) > 0)) {
                best = candidate.get();
            }
        }
        return Optional.ofNullable(best);
    }
}
