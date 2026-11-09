package io.github.chris013.practice.task05;

import static io.github.chris013.practice.task05.TextInvoiceRenderer.amount;

/** Phase 2b: the invoice in the member portal. */
final class HtmlInvoiceRenderer {

    String render(Invoice invoice) {
        StringBuilder html = new StringBuilder();
        html.append("<h1>").append(escape(invoice.title())).append("</h1>\n");
        html.append("<table>\n");
        html.append("  <tr><th>Item</th><th>Quantity</th><th>Amount</th></tr>\n");
        for (Invoice.Line line : invoice.lines()) {
            html.append("  <tr><td>%s</td><td>%d</td><td>%s</td></tr>\n"
                    .formatted(escape(line.item()), line.quantity(), amount(line.amount())));
        }
        html.append("</table>\n");
        html.append("<p>Total: <strong>").append(amount(invoice.total())).append("</strong></p>\n");
        html.append("<p>Loyalty points earned: ").append(invoice.loyaltyPoints()).append("</p>\n");
        return html.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
