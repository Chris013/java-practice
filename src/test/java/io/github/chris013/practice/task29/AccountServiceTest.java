package io.github.chris013.practice.task29;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task29.adapter.inmemory.InMemoryEventStore;
import io.github.chris013.practice.task29.application.AccountService;
import io.github.chris013.practice.task29.application.ConcurrencyException;
import io.github.chris013.practice.task29.application.EventStore;
import io.github.chris013.practice.task29.domain.AccountClosedException;
import io.github.chris013.practice.task29.domain.AccountEvent;
import io.github.chris013.practice.task29.domain.AccountId;
import io.github.chris013.practice.task29.domain.AccountOpened;
import io.github.chris013.practice.task29.domain.InsufficientFundsException;
import io.github.chris013.practice.task29.domain.Money;
import io.github.chris013.practice.task29.domain.MoneyDeposited;
import io.github.chris013.practice.task29.domain.MoneyWithdrawn;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private static final AccountId ID = new AccountId("acc-1");
    private static final Instant T0 = Instant.parse("2026-03-01T09:00:00Z");

    private final MutableClock clock = new MutableClock(T0);
    private final InMemoryEventStore store = new InMemoryEventStore();
    private final AccountService service = new AccountService(store, clock, () -> ID);

    /** Adds an event to the stream behind the back of the service, as a second writer would. */
    private static void writeConcurrently(EventStore store, AccountEvent event) {
        store.append(event.accountId(), store.load(event.accountId()).size(), List.of(event));
    }

    /**
     * Lets another writer sneak in before the first {@code races} appends, then forwards the append. This is the
     * lost-update scenario in code: the service decided on a state that another writer has already changed.
     */
    private static final class RacingEventStore implements EventStore {

        private final EventStore delegate;
        private final Consumer<EventStore> otherWriter;
        private int racesLeft;
        private int appendCalls;
        private int loadCalls;

        RacingEventStore(EventStore delegate, int races, Consumer<EventStore> otherWriter) {
            this.delegate = delegate;
            this.racesLeft = races;
            this.otherWriter = otherWriter;
        }

        @Override
        public List<AccountEvent> load(AccountId id) {
            loadCalls++;
            return delegate.load(id);
        }

        @Override
        public void append(AccountId id, long expectedVersion, List<AccountEvent> events) {
            appendCalls++;
            if (racesLeft > 0) {
                racesLeft--;
                otherWriter.accept(delegate);
            }
            delegate.append(id, expectedVersion, events);
        }

        int appendCalls() {
            return appendCalls;
        }

        int loadCalls() {
            return loadCalls;
        }
    }

    @Nested
    class HappyPath {

        @Test
        void open_stores_the_opening_event_and_returns_the_id() {
            AccountId opened = service.open("Anna", Money.euros("500.00"));

            assertThat(opened).isEqualTo(ID);
            assertThat(store.load(ID))
                    .containsExactly(new AccountOpened(ID, T0, "Anna", Money.euros("500.00")));
        }

        @Test
        void each_command_is_replayed_from_the_store_and_persisted() {
            service.open("Anna", Money.euros("500.00"));
            service.deposit(ID, Money.euros("100.00"));
            service.withdraw(ID, Money.euros("40.00"));

            assertThat(service.balanceOf(ID)).isEqualTo(Money.euros("60.00"));
            assertThat(store.load(ID)).hasSize(3);
        }

        @Test
        void events_carry_the_time_of_the_command() {
            service.open("Anna", Money.euros("500.00"));
            clock.advanceBy(Duration.ofHours(1));

            service.deposit(ID, Money.euros("10.00"));

            assertThat(store.load(ID).getLast())
                    .isEqualTo(new MoneyDeposited(ID, T0.plus(Duration.ofHours(1)), Money.euros("10.00")));
        }

        @Test
        void limit_changes_and_closing_go_through_the_store() {
            service.open("Anna", Money.euros("500.00"));

            service.changeOverdraftLimit(ID, Money.euros("800.00"));
            service.close(ID);

            assertThat(service.balanceOf(ID)).isEqualTo(Money.zero());
            assertThat(store.load(ID)).hasSize(3);
            assertThatThrownBy(() -> service.deposit(ID, Money.euros("1.00")))
                    .isInstanceOf(AccountClosedException.class);
        }
    }

    @Nested
    class UnknownAccount {

        @Test
        void a_command_on_an_unknown_account_is_rejected() {
            assertThatThrownBy(() -> service.deposit(ID, Money.euros("1.00")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("unknown account acc-1");
        }

        @Test
        void asking_the_balance_of_an_unknown_account_is_rejected() {
            assertThatThrownBy(() -> service.balanceOf(ID)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class OptimisticConcurrency {

        @Test
        void retries_after_a_concurrent_deposit_and_keeps_both_writes() {
            service.open("Anna", Money.euros("500.00"));
            service.deposit(ID, Money.euros("100.00"));
            RacingEventStore racing = new RacingEventStore(store, 1,
                    writer -> writeConcurrently(writer, new MoneyDeposited(ID, T0, Money.euros("50.00"))));
            AccountService racingService = new AccountService(racing, clock, () -> ID);

            racingService.withdraw(ID, Money.euros("80.00"));

            assertThat(service.balanceOf(ID)).isEqualTo(Money.euros("70.00"));
            assertThat(racing.appendCalls()).isEqualTo(2);
            assertThat(store.load(ID)).hasSize(4);
        }

        @Test
        void revalidates_a_withdrawal_that_was_fine_on_the_stale_balance() {
            // Balance 100.00, limit 500.00. Both writers see 100.00 and withdraw 500.00 (-400.00, fine).
            // The other one lands first, so the retry sees -400.00, and -400.00 - 500.00 = -900.00 is past
            // -500.00: refused.
            service.open("Anna", Money.euros("500.00"));
            service.deposit(ID, Money.euros("100.00"));
            RacingEventStore racing = new RacingEventStore(store, 1,
                    writer -> writeConcurrently(writer, new MoneyWithdrawn(ID, T0, Money.euros("500.00"))));
            AccountService racingService = new AccountService(racing, clock, () -> ID);

            assertThatThrownBy(() -> racingService.withdraw(ID, Money.euros("500.00")))
                    .isInstanceOf(InsufficientFundsException.class);

            assertThat(service.balanceOf(ID)).isEqualTo(Money.euros("-400.00"));
            assertThat(racing.appendCalls()).isEqualTo(1);
            assertThat(racing.loadCalls()).isEqualTo(2);
            assertThat(store.load(ID)).hasSize(3);
        }

        @Test
        void gives_up_after_three_attempts_and_rethrows_the_conflict() {
            service.open("Anna", Money.euros("500.00"));
            RacingEventStore racing = new RacingEventStore(store, Integer.MAX_VALUE,
                    writer -> writeConcurrently(writer, new MoneyDeposited(ID, T0, Money.euros("1.00"))));
            AccountService racingService = new AccountService(racing, clock, () -> ID);

            assertThatThrownBy(() -> racingService.deposit(ID, Money.euros("10.00")))
                    .isInstanceOf(ConcurrencyException.class);

            assertThat(racing.appendCalls()).isEqualTo(3);
            assertThat(store.load(ID)).hasSize(4);
        }

        @Test
        void a_business_failure_is_never_retried() {
            service.open("Anna", Money.zero());
            RacingEventStore racing = new RacingEventStore(store, Integer.MAX_VALUE,
                    writer -> writeConcurrently(writer, new MoneyDeposited(ID, T0, Money.euros("1.00"))));
            AccountService racingService = new AccountService(racing, clock, () -> ID);

            assertThatThrownBy(() -> racingService.withdraw(ID, Money.euros("5.00")))
                    .isInstanceOf(InsufficientFundsException.class);

            assertThat(racing.loadCalls()).isEqualTo(1);
            assertThat(racing.appendCalls()).isZero();
        }
    }
}
