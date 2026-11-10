package io.github.chris013.practice.task29;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task29.domain.Account;
import io.github.chris013.practice.task29.domain.AccountClosed;
import io.github.chris013.practice.task29.domain.AccountClosedException;
import io.github.chris013.practice.task29.domain.AccountEvent;
import io.github.chris013.practice.task29.domain.AccountId;
import io.github.chris013.practice.task29.domain.AccountOpened;
import io.github.chris013.practice.task29.domain.InsufficientFundsException;
import io.github.chris013.practice.task29.domain.Money;
import io.github.chris013.practice.task29.domain.MoneyDeposited;
import io.github.chris013.practice.task29.domain.MoneyWithdrawn;
import io.github.chris013.practice.task29.domain.OverdraftLimitChanged;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccountTest {

    private static final AccountId ID = new AccountId("acc-1");
    private static final AccountId OTHER_ID = new AccountId("acc-2");
    private static final Instant T0 = Instant.parse("2026-03-01T09:00:00Z");
    private static final Instant T1 = Instant.parse("2026-03-02T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-03-03T09:00:00Z");
    private static final Money LIMIT_500 = Money.euros("500.00");

    /** An account that has been opened and whose opening is already committed, like one loaded from a store. */
    private static Account committedAccount(Money limit) {
        Account account = Account.open(ID, "Anna", limit, T0);
        account.markCommitted();
        return account;
    }

    @Nested
    class Opening {

        @Test
        void starts_with_a_zero_balance_and_the_given_limit() {
            Account account = Account.open(ID, "Anna", LIMIT_500, T0);

            assertThat(account.id()).isEqualTo(ID);
            assertThat(account.owner()).isEqualTo("Anna");
            assertThat(account.balance()).isEqualTo(Money.zero());
            assertThat(account.overdraftLimit()).isEqualTo(LIMIT_500);
            assertThat(account.isClosed()).isFalse();
        }

        @Test
        void records_one_opened_event() {
            Account account = Account.open(ID, "Anna", LIMIT_500, T0);

            assertThat(account.uncommittedEvents()).containsExactly(new AccountOpened(ID, T0, "Anna", LIMIT_500));
            assertThat(account.version()).isEqualTo(1);
        }

        @Test
        void rejects_a_blank_owner() {
            assertThatThrownBy(() -> Account.open(ID, " ", LIMIT_500, T0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_negative_overdraft_limit_but_accepts_zero() {
            assertThatThrownBy(() -> Account.open(ID, "Anna", Money.euros("-0.01"), T0))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(Account.open(ID, "Anna", Money.zero(), T0).overdraftLimit()).isEqualTo(Money.zero());
        }
    }

    @Nested
    class Deposits {

        @Test
        void increases_the_balance_and_records_the_event() {
            Account account = committedAccount(LIMIT_500);

            account.deposit(Money.euros("100.00"), T1);

            assertThat(account.balance()).isEqualTo(Money.euros("100.00"));
            assertThat(account.uncommittedEvents())
                    .containsExactly(new MoneyDeposited(ID, T1, Money.euros("100.00")));
            assertThat(account.version()).isEqualTo(2);
        }

        @ParameterizedTest
        @CsvSource({"0.00", "-1.00", "-0.01"})
        void rejects_an_amount_that_is_not_positive_and_records_nothing(String amount) {
            Account account = committedAccount(LIMIT_500);

            assertThatThrownBy(() -> account.deposit(Money.euros(amount), T1))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(account.uncommittedEvents()).isEmpty();
            assertThat(account.version()).isEqualTo(1);
        }
    }

    @Nested
    class Withdrawals {

        @Test
        void reduces_the_balance_within_what_is_there() {
            Account account = committedAccount(LIMIT_500);
            account.deposit(Money.euros("100.00"), T1);

            account.withdraw(Money.euros("40.00"), T2);

            assertThat(account.balance()).isEqualTo(Money.euros("60.00"));
        }

        @Test
        void allows_the_balance_to_reach_exactly_minus_the_limit() {
            Account account = committedAccount(LIMIT_500);

            account.withdraw(Money.euros("500.00"), T1);

            assertThat(account.balance()).isEqualTo(Money.euros("-500.00"));
            assertThat(account.balance().isNegative()).isTrue();
        }

        @Test
        void refuses_one_cent_beyond_the_overdraft_limit() {
            Account account = committedAccount(LIMIT_500);

            assertThatThrownBy(() -> account.withdraw(Money.euros("500.01"), T1))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("500.01 EUR")
                    .hasMessageContaining("500.00 EUR");
            assertThat(account.balance()).isEqualTo(Money.zero());
        }

        @Test
        void counts_the_money_already_in_the_account_as_available() {
            Account account = committedAccount(LIMIT_500);
            account.deposit(Money.euros("100.00"), T1);
            account.markCommitted();

            assertThatThrownBy(() -> account.withdraw(Money.euros("601.00"), T2))
                    .isInstanceOf(InsufficientFundsException.class)
                    .hasMessageContaining("600.00 EUR");
            account.withdraw(Money.euros("600.00"), T2);
            assertThat(account.balance()).isEqualTo(Money.euros("-500.00"));
        }

        @ParameterizedTest
        @CsvSource({"0.00", "-5.00"})
        void rejects_an_amount_that_is_not_positive(String amount) {
            Account account = committedAccount(LIMIT_500);

            assertThatThrownBy(() -> account.withdraw(Money.euros(amount), T1))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(account.uncommittedEvents()).isEmpty();
        }

        @Test
        void a_refused_withdrawal_records_nothing() {
            Account account = committedAccount(Money.zero());

            assertThatThrownBy(() -> account.withdraw(Money.euros("0.01"), T1))
                    .isInstanceOf(InsufficientFundsException.class);
            assertThat(account.uncommittedEvents()).isEmpty();
            assertThat(account.version()).isEqualTo(1);
        }
    }

    @Nested
    class ChangingTheOverdraftLimit {

        @Test
        void sets_the_new_limit_and_records_the_event() {
            Account account = committedAccount(LIMIT_500);

            account.changeOverdraftLimit(Money.euros("800.00"), T1);

            assertThat(account.overdraftLimit()).isEqualTo(Money.euros("800.00"));
            assertThat(account.uncommittedEvents())
                    .containsExactly(new OverdraftLimitChanged(ID, T1, Money.euros("800.00")));
        }

        @Test
        void allows_a_limit_of_exactly_the_current_debt() {
            Account account = committedAccount(LIMIT_500);
            account.withdraw(Money.euros("300.00"), T1);

            account.changeOverdraftLimit(Money.euros("300.00"), T2);

            assertThat(account.overdraftLimit()).isEqualTo(Money.euros("300.00"));
        }

        @Test
        void refuses_a_limit_one_cent_below_the_current_debt() {
            Account account = committedAccount(LIMIT_500);
            account.withdraw(Money.euros("300.00"), T1);
            account.markCommitted();

            assertThatThrownBy(() -> account.changeOverdraftLimit(Money.euros("299.99"), T2))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(account.overdraftLimit()).isEqualTo(LIMIT_500);
            assertThat(account.uncommittedEvents()).isEmpty();
        }

        @Test
        void rejects_a_negative_limit() {
            Account account = committedAccount(LIMIT_500);

            assertThatThrownBy(() -> account.changeOverdraftLimit(Money.euros("-1.00"), T1))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(account.uncommittedEvents()).isEmpty();
        }
    }

    @Nested
    class Closing {

        @Test
        void closes_an_account_with_a_zero_balance() {
            Account account = committedAccount(LIMIT_500);

            account.close(T1);

            assertThat(account.isClosed()).isTrue();
            assertThat(account.uncommittedEvents()).containsExactly(new AccountClosed(ID, T1));
        }

        @Test
        void refuses_to_close_while_money_is_in_the_account() {
            Account account = committedAccount(LIMIT_500);
            account.deposit(Money.euros("0.01"), T1);

            assertThatThrownBy(() -> account.close(T2)).isInstanceOf(IllegalStateException.class);
            assertThat(account.isClosed()).isFalse();
        }

        @Test
        void refuses_to_close_while_the_account_is_overdrawn() {
            Account account = committedAccount(LIMIT_500);
            account.withdraw(Money.euros("50.00"), T1);

            assertThatThrownBy(() -> account.close(T2)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void closing_twice_is_refused_as_a_closed_account() {
            Account account = committedAccount(LIMIT_500);
            account.close(T1);

            assertThatThrownBy(() -> account.close(T2)).isInstanceOf(AccountClosedException.class);
        }
    }

    @Nested
    class ClosedAccounts {

        @Test
        void reject_every_command() {
            Account account = committedAccount(LIMIT_500);
            account.close(T1);
            account.markCommitted();

            assertThatThrownBy(() -> account.deposit(Money.euros("1.00"), T2))
                    .isInstanceOf(AccountClosedException.class);
            assertThatThrownBy(() -> account.withdraw(Money.euros("1.00"), T2))
                    .isInstanceOf(AccountClosedException.class);
            assertThatThrownBy(() -> account.changeOverdraftLimit(Money.euros("10.00"), T2))
                    .isInstanceOf(AccountClosedException.class);
            assertThat(account.uncommittedEvents()).isEmpty();
            assertThat(account.version()).isEqualTo(2);
        }

        @Test
        void checks_the_arguments_before_the_closed_state() {
            Account account = committedAccount(LIMIT_500);
            account.close(T1);

            assertThatThrownBy(() -> account.deposit(Money.zero(), T2)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Replay {

        private final List<AccountEvent> history = List.of(
                new AccountOpened(ID, T0, "Anna", LIMIT_500),
                new MoneyDeposited(ID, T1, Money.euros("200.00")),
                new MoneyWithdrawn(ID, T2, Money.euros("350.00")),
                new OverdraftLimitChanged(ID, T2, Money.euros("800.00")));

        @Test
        void reproduces_the_balance_limit_and_version() {
            Account account = Account.replay(history);

            assertThat(account.id()).isEqualTo(ID);
            assertThat(account.owner()).isEqualTo("Anna");
            assertThat(account.balance()).isEqualTo(Money.euros("-150.00"));
            assertThat(account.overdraftLimit()).isEqualTo(Money.euros("800.00"));
            assertThat(account.version()).isEqualTo(4);
        }

        @Test
        void leaves_no_uncommitted_events() {
            Account account = Account.replay(history);

            assertThat(account.uncommittedEvents()).isEmpty();
        }

        @Test
        void applies_an_old_withdrawal_that_today_would_refuse() {
            // Opened in 2026 with a limit of 500.00. A limit capped at 300.00 from 2027 would refuse this,
            // replay must not.
            List<AccountEvent> oldHistory = List.of(
                    new AccountOpened(ID, T0, "Anna", LIMIT_500),
                    new MoneyWithdrawn(ID, T1, Money.euros("450.00")));

            assertThat(Account.replay(oldHistory).balance()).isEqualTo(Money.euros("-450.00"));
        }

        @Test
        void restores_a_closed_account() {
            List<AccountEvent> closedHistory = List.of(
                    new AccountOpened(ID, T0, "Anna", LIMIT_500),
                    new AccountClosed(ID, T1));

            assertThat(Account.replay(closedHistory).isClosed()).isTrue();
        }

        @Test
        void rejects_an_empty_history() {
            assertThatThrownBy(() -> Account.replay(List.of())).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_history_that_does_not_start_with_the_opening() {
            assertThatThrownBy(() -> Account.replay(List.of(new MoneyDeposited(ID, T1, Money.euros("1.00")))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_events_of_another_account() {
            List<AccountEvent> mixed = List.of(
                    new AccountOpened(ID, T0, "Anna", LIMIT_500),
                    new MoneyDeposited(OTHER_ID, T1, Money.euros("1.00")));

            assertThatThrownBy(() -> Account.replay(mixed)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_second_opening() {
            List<AccountEvent> twice = List.of(
                    new AccountOpened(ID, T0, "Anna", LIMIT_500),
                    new AccountOpened(ID, T1, "Anna", Money.zero()));

            assertThatThrownBy(() -> Account.replay(twice)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Events {

        @Test
        void every_event_carries_the_instant_the_command_was_given() {
            Account account = Account.open(ID, "Anna", LIMIT_500, T0);
            account.deposit(Money.euros("10.00"), T1);

            assertThat(account.uncommittedEvents()).extracting(AccountEvent::occurredAt).containsExactly(T0, T1);
        }

        @Test
        void mark_committed_clears_the_uncommitted_events_but_keeps_the_version() {
            Account account = Account.open(ID, "Anna", LIMIT_500, T0);

            account.markCommitted();

            assertThat(account.uncommittedEvents()).isEmpty();
            assertThat(account.version()).isEqualTo(1);
        }

        @Test
        void uncommitted_events_cannot_be_changed_from_outside() {
            Account account = Account.open(ID, "Anna", LIMIT_500, T0);

            assertThatThrownBy(() -> account.uncommittedEvents().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
