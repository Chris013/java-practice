package io.github.chris013.practice.task05;

import java.time.YearMonth;
import java.util.List;

/** Creates the monthly invoice for a gym member: calculate once, render in the format the caller needs. */
public class InvoiceGenerator {

    private final InvoiceCalculator calculator = new InvoiceCalculator();
    private final TextInvoiceRenderer text = new TextInvoiceRenderer();
    private final HtmlInvoiceRenderer html = new HtmlInvoiceRenderer();

    public String statement(Member member, YearMonth month, List<Visit> visits) {
        return text.render(calculator.calculate(member, month, visits));
    }

    public String htmlStatement(Member member, YearMonth month, List<Visit> visits) {
        return html.render(calculator.calculate(member, month, visits));
    }
}
