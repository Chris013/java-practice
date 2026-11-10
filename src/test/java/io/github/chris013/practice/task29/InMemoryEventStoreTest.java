package io.github.chris013.practice.task29;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task29.adapter.inmemory.InMemoryEventStore;
import io.github.chris013.practice.task29.application.ConcurrencyException;
import io.github.chris013.practice.task29.domain.AccountEvent;
import io.github.chris013.practice.task29.domain.AccountId;
import io.github.chris013.practice.task29.domain.AccountOpened;
import io.github.chris013.practice.task29.domain.Money;
import io.github.chris013.practice.task29.domain.MoneyDeposited;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InMemoryEventStoreTest {

    private static final AccountId ID = new AccountId("acc-1");
    private static final Instant T0 = Instant.parse("2026-03-01T09:00:00Z");
    private static final AccountOpened OPENED = new AccountOpened(ID, T0, "Anna", Money.zero());
    private static final MoneyDeposited DEPOSIT = new MoneyDeposited(ID, T0, Money.euros("10.00"));

    @Nested
    class AppendAndLoad {

        private final InMemoryEventStore store = new InMemoryEventStore();

        @Test
        void loads_what_was_appended_in_order() {
            store.append(ID, 0, List.of(OPENED));
            store.append(ID, 1, List.of(DEPOSIT));

            assertThat(store.load(ID)).containsExactly(OPENED, DEPOSIT);
        }

        @Test
        void an_unknown_stream_loads_as_empty() {
            assertThat(store.load(new AccountId("nobody"))).isEmpty();
        }

        @Test
        void a_new_stream_must_be_appended_at_version_zero() {
            assertThatThrownBy(() -> store.append(ID, 1, List.of(OPENED)))
                    .isInstanceOf(ConcurrencyException.class);
            assertThat(store.load(ID)).isEmpty();
        }

        @Test
        void a_wrong_expected_version_is_refused_and_writes_nothing() {
            store.append(ID, 0, List.of(OPENED));

            assertThatThrownBy(() -> store.append(ID, 0, List.of(DEPOSIT)))
                    .isInstanceOf(ConcurrencyException.class)
                    .hasMessageContaining("expected version 0")
                    .hasMessageContaining("at 1");
            assertThat(store.load(ID)).containsExactly(OPENED);
        }

        @Test
        void the_returned_history_cannot_be_changed() {
            store.append(ID, 0, List.of(OPENED));

            assertThatThrownBy(() -> store.load(ID).add(DEPOSIT)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void streams_are_independent() {
            AccountId other = new AccountId("acc-2");
            store.append(ID, 0, List.of(OPENED));
            store.append(other, 0, List.of(new AccountOpened(other, T0, "Ben", Money.zero())));

            assertThat(store.load(ID)).hasSize(1);
            assertThat(store.load(other)).hasSize(1);
        }
    }

    @Nested
    class Concurrency {

        @Test
        void of_many_writers_with_the_same_expected_version_exactly_one_succeeds() throws InterruptedException {
            InMemoryEventStore store = new InMemoryEventStore();
            store.append(ID, 0, List.of(OPENED));
            int writers = 100;
            AtomicInteger succeeded = new AtomicInteger();
            AtomicInteger refused = new AtomicInteger();
            CountDownLatch startTogether = new CountDownLatch(1);

            try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < writers; i++) {
                    pool.submit(() -> {
                        startTogether.await();
                        try {
                            store.append(ID, 1, List.of(DEPOSIT));
                            succeeded.incrementAndGet();
                        } catch (ConcurrencyException expected) {
                            refused.incrementAndGet();
                        }
                        return null;
                    });
                }
                startTogether.countDown();
            }

            assertThat(succeeded.get()).isEqualTo(1);
            assertThat(refused.get()).isEqualTo(writers - 1);
            List<AccountEvent> history = store.load(ID);
            assertThat(history).hasSize(2);
        }
    }
}
