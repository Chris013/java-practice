package io.github.chris013.practice.task13;

import java.math.BigDecimal;

/** Given. One row of the VAT breakdown on the receipt: the amounts for one rate. */
public record VatLine(VatRate rate, BigDecimal gross, BigDecimal net, BigDecimal vat) {
}
