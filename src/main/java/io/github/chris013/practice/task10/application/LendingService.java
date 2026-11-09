package io.github.chris013.practice.task10.application;

import io.github.chris013.practice.task10.domain.CopyId;
import io.github.chris013.practice.task10.domain.Loan;
import io.github.chris013.practice.task10.domain.MemberId;
import io.github.chris013.practice.task10.domain.ReturnReceipt;
import java.time.Clock;

/**
 * The use cases of the lending desk. It orchestrates: it loads, checks the rules, asks the domain to change, and
 * saves. It holds no SQL and no HTTP, only the ports it was given.
 *
 * <p>Task 10: read {@code tasks/10-library-lending-hexagonal.md} first.
 */
public final class LendingService {

    public LendingService(LoanRepository loans, MemberDirectory members, Catalog catalog, Clock clock) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    /**
     * Lends a copy to a member. Checks in this order and throws the first refusal that applies: unknown member,
     * blocked member, overdue loan, loan limit (5), unknown copy, copy already on loan.
     */
    public Loan borrow(MemberId memberId, CopyId copyId) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    /** Ends the loan of a copy and returns the receipt with the late fee. Refuses if the copy is not on loan. */
    public ReturnReceipt giveBack(CopyId copyId) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    /** Renews the loan of a copy. Refuses if the copy is not on loan or the renewal is not allowed. */
    public Loan renew(CopyId copyId) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }
}
