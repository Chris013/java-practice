package io.github.chris013.practice.task14;

import java.util.List;

/**
 * One parsed flag: a kill switch, a default, and rules in the order they were written.
 *
 * <p>The kill switch is checked first, so it wins over everything else, wherever it appears in the block. Then the
 * rules are tried top to bottom; the first one that matches turns the flag on. Only if none matches does the
 * default apply.
 */
record Flag(String name, boolean killSwitch, boolean defaultOn, List<Rule> rules) {

    Flag {
        rules = List.copyOf(rules);
    }

    boolean isOnFor(EvaluationContext context) {
        if (killSwitch) {
            return false;
        }
        for (Rule rule : rules) {
            if (rule.matches(context, name)) {
                return true;
            }
        }
        return defaultOn;
    }
}
