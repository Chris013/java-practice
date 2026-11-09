package io.github.chris013.practice.task10.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Given. What the desk tells the member after a return: which loan ended, when, and the late fee (0.00 if on time). */
public record ReturnReceipt(Loan loan, LocalDate returnedOn, BigDecimal lateFee) {

    public ReturnReceipt {
        Objects.requireNonNull(loan, "loan");
        Objects.requireNonNull(returnedOn, "returnedOn");
        Objects.requireNonNull(lateFee, "lateFee");
    }
}
