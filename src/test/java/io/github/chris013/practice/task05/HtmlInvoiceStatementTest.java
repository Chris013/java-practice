package io.github.chris013.practice.task05;

import static io.github.chris013.practice.task05.Visits.MARCH;
import static io.github.chris013.practice.task05.Visits.visits;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** The new feature. Red until part 2. */
class HtmlInvoiceStatementTest {

    private final InvoiceGenerator generator = new InvoiceGenerator();

    @Test
    void renders_the_same_invoice_as_html() {
        assertThat(generator.htmlStatement(new Member("Anna Schmidt", "PREMIUM"), MARCH, visits(10, 3, 0)))
                .isEqualTo("""
                        <h1>Invoice for Anna Schmidt (PREMIUM) - 2026-03</h1>
                        <table>
                          <tr><th>Item</th><th>Quantity</th><th>Amount</th></tr>
                          <tr><td>Membership fee</td><td>1</td><td>59.00</td></tr>
                          <tr><td>Classes (8 included)</td><td>10</td><td>8.00</td></tr>
                          <tr><td>Sauna</td><td>3</td><td>0.00</td></tr>
                        </table>
                        <p>Total: <strong>67.00</strong></p>
                        <p>Loyalty points earned: 25</p>
                        """);
    }

    @Test
    void renders_a_student_invoice() {
        assertThat(generator.htmlStatement(new Member("Cem Yilmaz", "STUDENT"), MARCH, visits(4, 1, 6)))
                .contains("<tr><td>Classes</td><td>4</td><td>24.00</td></tr>")
                .contains("<p>Total: <strong>48.00</strong></p>")
                .contains("<p>Loyalty points earned: 9</p>");
    }

    @Test
    void escapes_html_in_member_names() {
        assertThat(generator.htmlStatement(new Member("Tom <Admin> & Jerry", "BASIC"), MARCH, visits(0, 0, 0)))
                .startsWith("<h1>Invoice for Tom &lt;Admin&gt; &amp; Jerry (BASIC) - 2026-03</h1>");
    }
}
