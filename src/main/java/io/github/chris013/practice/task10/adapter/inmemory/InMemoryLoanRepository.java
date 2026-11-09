package io.github.chris013.practice.task10.adapter.inmemory;

import io.github.chris013.practice.task10.application.LoanRepository;
import io.github.chris013.practice.task10.domain.CopyId;
import io.github.chris013.practice.task10.domain.Loan;
import io.github.chris013.practice.task10.domain.MemberId;
import java.util.List;
import java.util.Optional;

/**
 * Keeps active loans in memory. A real adapter for a demo app and a test double for the tests. Task 10: read
 * {@code tasks/10-library-lending-hexagonal.md} first.
 */
public final class InMemoryLoanRepository implements LoanRepository {

    @Override
    public Optional<Loan> findActiveLoanOf(CopyId copyId) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    @Override
    public List<Loan> findActiveLoansOf(MemberId memberId) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    @Override
    public void save(Loan loan) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    @Override
    public void close(CopyId copyId) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }
}
