package io.github.chris013.practice.task10.adapter.inmemory;

import io.github.chris013.practice.task10.application.LoanRepository;
import io.github.chris013.practice.task10.domain.CopyId;
import io.github.chris013.practice.task10.domain.Loan;
import io.github.chris013.practice.task10.domain.MemberId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps active loans in memory. A real adapter for a demo app and a test double for the tests. Data lives only as
 * long as this object does.
 */
public final class InMemoryLoanRepository implements LoanRepository {

    private final Map<CopyId, Loan> activeByCopy = new ConcurrentHashMap<>();

    @Override
    public Optional<Loan> findActiveLoanOf(CopyId copyId) {
        Objects.requireNonNull(copyId, "copyId");
        return Optional.ofNullable(activeByCopy.get(copyId));
    }

    @Override
    public List<Loan> findActiveLoansOf(MemberId memberId) {
        Objects.requireNonNull(memberId, "memberId");
        return activeByCopy.values().stream()
                .filter(loan -> loan.memberId().equals(memberId))
                .toList();
    }

    @Override
    public void save(Loan loan) {
        Objects.requireNonNull(loan, "loan");
        activeByCopy.put(loan.copyId(), loan);
    }

    @Override
    public void close(CopyId copyId) {
        Objects.requireNonNull(copyId, "copyId");
        activeByCopy.remove(copyId);
    }
}
