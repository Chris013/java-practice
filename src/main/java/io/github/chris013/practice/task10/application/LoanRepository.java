package io.github.chris013.practice.task10.application;

import io.github.chris013.practice.task10.domain.CopyId;
import io.github.chris013.practice.task10.domain.Loan;
import io.github.chris013.practice.task10.domain.MemberId;
import java.util.List;
import java.util.Optional;

/**
 * Given. Outbound port: where active loans are kept. It is shaped by what the lending rules need, not by any
 * database. A JDBC adapter, a file adapter and an in-memory adapter can all implement it.
 */
public interface LoanRepository {

    /** The loan that currently holds this copy, if any. */
    Optional<Loan> findActiveLoanOf(CopyId copyId);

    /** All loans this member currently holds. */
    List<Loan> findActiveLoansOf(MemberId memberId);

    /** Stores the loan as the active loan of its copy, replacing the previous one for that copy. */
    void save(Loan loan);

    /** Ends the active loan of this copy (the copy is back on the shelf). */
    void close(CopyId copyId);
}
