package io.github.chris013.practice.task10;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.chris013.practice.task10.adapter.inmemory.InMemoryLoanRepository;
import io.github.chris013.practice.task10.domain.CopyId;
import io.github.chris013.practice.task10.domain.Loan;
import io.github.chris013.practice.task10.domain.MemberId;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InMemoryLoanRepositoryTest {

    private static final CopyId C1 = new CopyId("copy-1");
    private static final MemberId ANNA = new MemberId("anna");

    private final InMemoryLoanRepository loans = new InMemoryLoanRepository();

    @Test
    void closing_a_copy_without_a_loan_does_nothing() {
        loans.close(C1);

        assertThat(loans.findActiveLoanOf(C1)).isEmpty();
    }

    @Test
    void closing_a_copy_removes_its_active_loan() {
        Loan loan = Loan.start(C1, ANNA, LocalDate.of(2026, 3, 1));
        loans.save(loan);

        loans.close(C1);

        assertThat(loans.findActiveLoanOf(C1)).isEmpty();
    }
}
