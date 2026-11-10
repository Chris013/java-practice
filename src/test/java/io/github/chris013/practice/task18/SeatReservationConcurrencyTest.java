package io.github.chris013.practice.task18;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * These tests fail on the racy starter and pass on the reference solution. Each one forces a specific interleaving
 * with {@link YieldingClock} and with a start gate, so the failure is reproducible rather than a one-in-a-million event.
 */
class SeatReservationConcurrencyTest {

    private static final Instant START = Instant.parse("2026-06-01T20:00:00Z");
    private static final Duration HOLD = Duration.ofMinutes(5);
    private static final SeatId A1 = new SeatId("A-1");
    private static final CustomerId ANNA = new CustomerId("anna");

    /**
     * Starts {@code tasks} virtual threads, releases them all at the same moment, and waits for every one of them.
     * An exception in any task is rethrown here, so a crash fails the test instead of disappearing.
     */
    private static void runConcurrently(int tasks, IntConsumer task) throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    startGate.await();
                    task.accept(index);
                    return null;
                }));
            }
            startGate.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        }
    }

    private static SeatId seat(int number) {
        return new SeatId("S-%03d".formatted(number));
    }

    private static Set<SeatId> seats(int count) {
        return IntStream.range(0, count).mapToObj(SeatReservationConcurrencyTest::seat).collect(Collectors.toSet());
    }

    @Nested
    class Races {

        @Test
        void exactly_one_of_1000_customers_gets_a_seat_they_all_want() throws Exception {
            // 20 rounds, each with a fresh service, so one lucky round cannot hide a broken hold.
            for (int round = 0; round < 20; round++) {
                var listener = new RecordingReservationListener();
                var service = new SeatReservationService(Set.of(A1), new YieldingClock(START), HOLD, listener);
                var winners = new AtomicInteger();

                runConcurrently(1000, customer -> {
                    if (service.hold(A1, new CustomerId("customer-" + customer)).isPresent()) {
                        winners.incrementAndGet();
                    }
                });

                assertThat(winners).as("round %d", round).hasValue(1);
                assertThat(listener.held()).as("round %d", round).hasSize(1);
            }
        }

        @Test
        void no_sale_is_lost_when_200_customers_buy_200_different_seats_at_once() throws Exception {
            // 20 rounds, each with a fresh service: a lost update is a rare interleaving, so one round is not enough.
            for (int round = 0; round < 20; round++) {
                var service = new SeatReservationService(seats(200), new YieldingClock(START), HOLD,
                        new RecordingReservationListener());
                var sold = new AtomicInteger();

                runConcurrently(200, i -> {
                    CustomerId customer = new CustomerId("customer-" + i);
                    if (service.hold(seat(i), customer).isPresent() && service.confirm(seat(i), customer)) {
                        sold.incrementAndGet();
                    }
                });

                assertThat(sold).as("round %d", round).hasValue(200);
                assertThat(service.soldCount()).as("round %d", round).isEqualTo(200);
                assertThat(service.soldSeats()).as("round %d", round).hasSize(200).doesNotHaveDuplicates();
            }
        }

        @Test
        void a_customer_who_confirms_the_same_hold_from_50_threads_buys_the_seat_once() throws Exception {
            for (int round = 0; round < 20; round++) {
                var service = new SeatReservationService(Set.of(A1), new YieldingClock(START), HOLD,
                        new RecordingReservationListener());
                service.hold(A1, ANNA);
                var confirmed = new AtomicInteger();

                runConcurrently(50, i -> {
                    if (service.confirm(A1, ANNA)) {
                        confirmed.incrementAndGet();
                    }
                });

                assertThat(confirmed).as("round %d", round).hasValue(1);
                assertThat(service.soldSeats()).as("round %d", round).containsExactly(A1);
            }
        }
    }

    @Nested
    class Iteration {

        @Test
        void iterating_the_sold_seats_while_another_thread_sells_does_not_throw() throws Exception {
            var service = new SeatReservationService(seats(200), new YieldingClock(START), HOLD,
                    new RecordingReservationListener());
            for (int i = 0; i < 10; i++) {
                service.hold(seat(i), new CustomerId("early-" + i));
                service.confirm(seat(i), new CustomerId("early-" + i));
            }

            int visited = 0;
            for (SeatId ignored : service.soldSeats()) {
                if (visited == 0) {
                    // Another thread sells the other 190 seats while this loop is in progress.
                    Thread writer = Thread.ofVirtual().start(() -> {
                        for (int i = 10; i < 200; i++) {
                            CustomerId customer = new CustomerId("late-" + i);
                            service.hold(seat(i), customer);
                            service.confirm(seat(i), customer);
                        }
                    });
                    writer.join();
                }
                visited++;
            }

            assertThat(visited).as("the loop sees the snapshot taken before the writer started").isEqualTo(10);
            assertThat(service.soldCount()).isEqualTo(200);
        }

        @Test
        void a_snapshot_taken_earlier_does_not_change_when_later_sales_happen() throws Exception {
            var service = new SeatReservationService(seats(3), new YieldingClock(START), HOLD,
                    new RecordingReservationListener());
            service.hold(seat(0), ANNA);
            service.confirm(seat(0), ANNA);
            List<SeatId> snapshot = service.soldSeats();

            runConcurrently(2, i -> {
                CustomerId customer = new CustomerId("late-" + i);
                service.hold(seat(i + 1), customer);
                service.confirm(seat(i + 1), customer);
            });

            assertThat(snapshot).containsExactly(seat(0));
            assertThat(service.soldSeats()).hasSize(3);
        }
    }

    @Nested
    class Callbacks {

        /**
         * Alien code: from another thread it releases the sold seat (a {@code compute} on the same key) and asks for
         * the available count, then waits for the answer. If {@code seatSold} ran inside {@code compute} for that key,
         * the other thread would block on the key and this listener would wait forever.
         */
        private static final class AskingListener implements ReservationListener {

            private final ExecutorService executor;
            private final AtomicInteger seenAvailable = new AtomicInteger(-1);
            private SeatReservationService service;

            AskingListener(ExecutorService executor) {
                this.executor = executor;
            }

            void bindTo(SeatReservationService service) {
                this.service = service;
            }

            @Override
            public void seatHeld(Hold hold) {
                // Not needed for this test.
            }

            @Override
            public void seatSold(SeatId seat, CustomerId customer) {
                try {
                    seenAvailable.set(executor.submit(() -> {
                        // compute on the same key: blocks if the caller is inside compute for that key
                        service.release(seat, new CustomerId("someone-else"));
                        return service.availableSeats();
                    }).get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted while asking the service", e);
                } catch (ExecutionException e) {
                    throw new IllegalStateException("the service failed when asked from another thread", e);
                }
            }
        }

        @Test
        void a_listener_may_call_back_into_the_service_from_another_thread_without_deadlock() {
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            try {
                var listener = new AskingListener(executor);
                var service = new SeatReservationService(Set.of(A1), new YieldingClock(START), HOLD, listener);
                listener.bindTo(service);

                assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
                    service.hold(A1, ANNA);
                    assertThat(service.confirm(A1, ANNA)).isTrue();
                });

                assertThat(listener.seenAvailable).hasValue(0);
            } finally {
                executor.shutdownNow();
            }
        }
    }
}
