package io.github.chris013.practice.task05;

import static io.github.chris013.practice.task05.Visits.MARCH;
import static io.github.chris013.practice.task05.Visits.concat;
import static io.github.chris013.practice.task05.Visits.visitOn;
import static io.github.chris013.practice.task05.Visits.visits;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Pins down the current plain-text statement. Green before you start, green after every refactoring step. */
class InvoiceStatementCharacterizationTest {

    private final InvoiceGenerator generator = new InvoiceGenerator();

    @Test
    void basic_member_pays_per_class_and_sauna_visit() {
        List<Visit> visits = concat(
                visits(3, 2, 1),
                List.of(visitOn("2026-02-28", "CLASS"), visitOn("2026-04-01", "SAUNA")));

        assertThat(generator.statement(new Member("Ben Weber", "BASIC"), MARCH, visits)).isEqualTo("""
                Invoice for Ben Weber (BASIC) - 2026-03
                  Membership fee x1: 29.00
                  Classes x3: 24.00
                  Sauna x2: 10.00
                Total: 63.00
                Loyalty points earned: 3
                """);
    }

    @Test
    void premium_member_gets_eight_classes_and_the_sauna_included_and_double_points() {
        assertThat(generator.statement(new Member("Anna Schmidt", "PREMIUM"), MARCH, visits(10, 3, 0))).isEqualTo("""
                Invoice for Anna Schmidt (PREMIUM) - 2026-03
                  Membership fee x1: 59.00
                  Classes (8 included) x10: 8.00
                  Sauna x3: 0.00
                Total: 67.00
                Loyalty points earned: 25
                """);
    }

    @Test
    void premium_member_pays_nothing_extra_within_the_included_classes() {
        assertThat(generator.statement(new Member("Anna Schmidt", "PREMIUM"), MARCH, visits(5, 0, 0))).isEqualTo("""
                Invoice for Anna Schmidt (PREMIUM) - 2026-03
                  Membership fee x1: 59.00
                  Classes (8 included) x5: 0.00
                  Sauna x0: 0.00
                Total: 59.00
                Loyalty points earned: 10
                """);
    }

    @Test
    void student_pays_a_lower_fee_and_class_price() {
        assertThat(generator.statement(new Member("Cem Yilmaz", "STUDENT"), MARCH, visits(4, 1, 6))).isEqualTo("""
                Invoice for Cem Yilmaz (STUDENT) - 2026-03
                  Membership fee x1: 19.00
                  Classes x4: 24.00
                  Sauna x1: 5.00
                Total: 48.00
                Loyalty points earned: 9
                """);
    }

    @Test
    void member_without_visits_pays_only_the_fee() {
        assertThat(generator.statement(new Member("Ben Weber", "BASIC"), MARCH, List.of())).isEqualTo("""
                Invoice for Ben Weber (BASIC) - 2026-03
                  Membership fee x1: 29.00
                  Classes x0: 0.00
                  Sauna x0: 0.00
                Total: 29.00
                Loyalty points earned: 0
                """);
    }

    @Test
    void regulars_bonus_needs_more_than_ten_visits() {
        Member ben = new Member("Ben Weber", "BASIC");

        assertThat(generator.statement(ben, MARCH, visits(0, 0, 10))).endsWith("Loyalty points earned: 0\n");
        assertThat(generator.statement(ben, MARCH, visits(0, 0, 11))).endsWith("Loyalty points earned: 5\n");
    }
}
