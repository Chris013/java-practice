package io.github.chris013.practice.task05;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Phase 2a: the plain-text invoice we email. */
final class TextInvoiceRenderer {

    String render(Invoice invoice) {
        StringBuilder text = new StringBuilder(invoice.title()).append('\n');
        for (Invoice.Line line : invoice.lines()) {
            text.append("  %s x%d: %s\n".formatted(line.item(), line.quantity(), amount(line.amount())));
        }
        text.append("Total: ").append(amount(invoice.total())).append('\n');
        text.append("Loyalty points earned: ").append(invoice.loyaltyPoints()).append('\n');
        return text.toString();
    }

    static String amount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
