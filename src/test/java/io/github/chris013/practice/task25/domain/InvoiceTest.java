package io.github.chris013.practice.task25.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InvoiceTest {

    private static final InvoiceId INVOICE = new InvoiceId("INV-0001");
    private static final SubscriptionId SUBSCRIPTION = new SubscriptionId("SUB-0001");
    private static final CustomerId ANNA = new CustomerId("anna");
    private static final LocalDate ISSUED = LocalDate.of(2026, 4, 14);

    private static Invoice invoiceWith(InvoiceLine... lines) {
        return new Invoice(INVOICE, SUBSCRIPTION, ANNA, ISSUED, List.of(lines));
    }

    @Nested
    class Totals {

        @Test
        void sums_the_lines() {
            Invoice invoice = invoiceWith(
                    new InvoiceLine("Unused time on TEAM", new BigDecimal("-16.43")),
                    new InvoiceLine("Remaining time on BUSINESS", new BigDecimal("44.77")));

            assertThat(invoice.total()).isEqualTo(new BigDecimal("28.34"));
        }

        @Test
        void never_goes_below_zero_when_credits_exceed_charges() {
            Invoice invoice = invoiceWith(new InvoiceLine("Unused time on BUSINESS", new BigDecimal("-79.00")));

            assertThat(invoice.total()).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        void an_invoice_without_lines_totals_zero() {
            assertThat(invoiceWith().total()).isEqualTo(new BigDecimal("0.00"));
        }
    }

    @Nested
    class Lines {

        @Test
        void amounts_are_normalised_to_two_decimals() {
            assertThat(new InvoiceLine("Credit", new BigDecimal("5")).amount()).isEqualTo(new BigDecimal("5.00"));
        }
    }
}
