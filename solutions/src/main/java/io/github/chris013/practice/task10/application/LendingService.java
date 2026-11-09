package io.github.chris013.practice.task10.application;

import io.github.chris013.practice.task10.domain.CopyId;
import io.github.chris013.practice.task10.domain.LendingRefusedException;
import io.github.chris013.practice.task10.domain.Loan;
import io.github.chris013.practice.task10.domain.Member;
import io.github.chris013.practice.task10.domain.MemberId;
import io.github.chris013.practice.task10.domain.RefusalReason;
import io.github.chris013.practice.task10.domain.ReturnReceipt;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The use cases of the lending desk. It orchestrates: it loads, checks the rules, asks the domain to change, and
 * saves. It holds no SQL and no HTTP, only the ports it was given.
 *
 * <p>Rules that concern one loan belong to {@link Loan}. Rules that need several loans, a member or the catalog
 * belong here.
 */
public final class LendingService {

    /** Maximum number of loans one member may hold at the same time. */
    static final int LOAN_LIMIT = 5;

    private final LoanRepository loans;
    private final MemberDirectory members;
    private final Catalog catalog;
    private final Clock clock;

    public LendingService(LoanRepository loans, MemberDirectory members, Catalog catalog, Clock clock) {
        this.loans = Objects.requireNonNull(loans, "loans");
        this.members = Objects.requireNonNull(members, "members");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * The order of the checks is part of the contract. The member is checked first, so a blocked member with
     * overdue loans is told "blocked", the more fundamental reason.
     */
    public Loan borrow(MemberId memberId, CopyId copyId) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(copyId, "copyId");
        LocalDate today = LocalDate.now(clock);

        Member member = members.findById(memberId).orElseThrow(() -> refuse(RefusalReason.UNKNOWN_MEMBER));
        if (member.blocked()) {
            throw refuse(RefusalReason.MEMBER_BLOCKED);
        }

        List<Loan> active = loans.findActiveLoansOf(memberId);
        if (active.stream().anyMatch(loan -> loan.isOverdueOn(today))) {
            throw refuse(RefusalReason.HAS_OVERDUE_LOANS);
        }
        if (active.size() >= LOAN_LIMIT) {
            throw refuse(RefusalReason.LOAN_LIMIT_REACHED);
        }

        if (!catalog.exists(copyId)) {
            throw refuse(RefusalReason.UNKNOWN_COPY);
        }
        if (loans.findActiveLoanOf(copyId).isPresent()) {
            throw refuse(RefusalReason.COPY_ALREADY_ON_LOAN);
        }

        Loan loan = Loan.start(copyId, memberId, today);
        loans.save(loan);
        return loan;
    }

    public ReturnReceipt giveBack(CopyId copyId) {
        Objects.requireNonNull(copyId, "copyId");
        Loan loan = loans.findActiveLoanOf(copyId).orElseThrow(() -> refuse(RefusalReason.COPY_NOT_ON_LOAN));
        LocalDate today = LocalDate.now(clock);

        BigDecimal lateFee = loan.lateFeeOn(today);
        loans.close(copyId);
        return new ReturnReceipt(loan, today, lateFee);
    }

    /**
     * The domain refuses renewals with an IllegalStateException. The service translates that into the refusal
     * type the desk, kiosk and app understand. The original exception is kept as suppressed, so nothing is lost
     * when debugging.
     */
    public Loan renew(CopyId copyId) {
        Objects.requireNonNull(copyId, "copyId");
        Loan loan = loans.findActiveLoanOf(copyId).orElseThrow(() -> refuse(RefusalReason.COPY_NOT_ON_LOAN));

        Loan renewed;
        try {
            renewed = loan.renewedOn(LocalDate.now(clock));
        } catch (IllegalStateException rejected) {
            LendingRefusedException refusal = refuse(RefusalReason.RENEWAL_NOT_ALLOWED);
            refusal.addSuppressed(rejected);
            throw refusal;
        }
        loans.save(renewed);
        return renewed;
    }

    private static LendingRefusedException refuse(RefusalReason reason) {
        return new LendingRefusedException(reason);
    }
}
