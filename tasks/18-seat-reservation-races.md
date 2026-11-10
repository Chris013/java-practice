# Task 18 · Fix the race conditions in concert seat reservations

| Difficulty | Topics | Time box |
|---|---|---|
| 🔴 Hard | Concurrency bugs: check-then-act, lost updates, unsafe publication of collections, `ConcurrentModificationException`, calling alien code while holding a lock · `ConcurrentHashMap.compute` as a per-key atomic operation · Stress tests with virtual threads · Making races reproducible | 3–4 h |

> **Background reading:** *Java Concurrency in Practice* (Goetz et al.) ch. 2 ("Thread safety"), ch. 5.1–5.2 ("Synchronized collections", "Concurrent collections"), ch. 10.1 ("Deadlock", the section on "open calls") · *Effective Java* Item 79 ("Avoid excessive synchronization", alien methods) · JEP 444 (Virtual Threads) · The Javadoc of `ConcurrentHashMap.compute`: "the computation should be short and simple, and must not attempt to update any other mappings".

---

## The situation

Our venue sells tickets for one concert, online. A customer picks a seat and we **hold** it for five minutes while they type in their card details. If they pay in time, we **confirm** the seat and it is sold. If they walk away, the hold expires and the seat goes back on sale.

Last Friday, during the presale, two customers got the same seat. Another customer was charged for a seat that was already sold. The support team found the cause in our code: the seat service was written for one user at a time and then put behind a web server with 200 concurrent users. Every single-threaded test passes. Nobody noticed until real people were buying at the same moment.

The [`SeatReservationService`](../src/main/java/io/github/chris013/practice/task18/SeatReservationService.java) in the starter is that code. It is not written carelessly. It is the code a capable mid-level developer writes when they are thinking about *one* thread. Your job is to find every place where two threads can interfere, fix them, and prove each fix with a test that fails before the fix.

The most important part of this task is not the fix. It is learning to **see** the races, and to make them happen on purpose.

## The domain

| Term | Meaning |
|---|---|
| **Seat** | A seat in the venue, identified by a `SeatId` such as `A-12`. The set of seats is fixed when the service is created. |
| **Free** | Nobody holds the seat and nobody bought it. |
| **Hold** | A customer's temporary claim on a seat. It is valid while `now` is before `expiresAt`. |
| **Sold** | The seat was confirmed by the customer who held it. It never becomes free again. |
| **Listener** | Told about every hold and sale. In production it pushes WebSocket updates to every browser that shows the seat map. |

### The seat life cycle

```mermaid
stateDiagram-v2
    [*] --> Free
    Free --> Held : hold succeeds
    Held --> Free : hold expires
    Held --> Free : customer releases
    Held --> Sold : holder confirms before expiry
    Sold --> [*]
```

A seat moves only along these arrows. Two arrows deserve attention: a hold **expires by the clock**, nobody has to call anything, and `Sold` has no way back.

### The listener is alien code

`ReservationListener` is an interface we do not control. Its implementation may be slow (it writes to 500 browsers), and it may call back into the service, for example to ask `availableSeats()` for the new count it shows on the page. Keep this in mind. It is the source of the deadlock you will meet in the next section.

## What I want from you

1. **[`SeatReservationService`](../src/main/java/io/github/chris013/practice/task18/SeatReservationService.java)** is the same class in both versions. Make it correct when many threads call it at once. The public API does not change.
2. `Optional<Hold> hold(SeatId, CustomerId)`:
   * an unknown seat throws `IllegalArgumentException`;
   * a free seat, or a seat whose hold has expired (`now` is at or after `expiresAt`), gives a new `Hold` that expires at `now + holdDuration`, and the listener's `seatHeld` is called;
   * a seat that is held and not expired, or sold, gives `Optional.empty()`, even when the same customer asks again.
3. `boolean confirm(SeatId, CustomerId)`:
   * returns `true` only if the seat is held by this customer and the hold has not expired. The seat then becomes sold, and the listener's `seatSold` is called;
   * otherwise returns `false` and changes nothing. Confirming a sold seat again returns `false`.
   * An unknown seat throws `IllegalArgumentException`, like `hold`.
4. `void release(SeatId, CustomerId)`: removes the hold if this customer holds the seat. Otherwise it does nothing. It never touches a sold seat.
5. `int availableSeats()`: the number of seats that are neither sold nor validly held at this instant. An expired hold counts as available.
6. `int soldCount()`: how many seats are sold.
7. `List<SeatId> soldSeats()`: a snapshot of the sold seats in sale order. A list that was returned earlier must not change when later sales happen.
8. The constructor rejects `null` with `NullPointerException` naming the parameter, and a `holdDuration` that is zero or negative with `IllegalArgumentException`.
9. The listener is called **after** the service has changed its state for that operation, and never while the service holds an internal lock.

The given types [`SeatId`](../src/main/java/io/github/chris013/practice/task18/SeatId.java), [`CustomerId`](../src/main/java/io/github/chris013/practice/task18/CustomerId.java), [`Hold`](../src/main/java/io/github/chris013/practice/task18/Hold.java) and [`ReservationListener`](../src/main/java/io/github/chris013/practice/task18/ReservationListener.java) do not change.

## Constraints & non-goals

* **No `synchronized` on the whole service just to make the tests pass.** You may use one global lock if you can justify it (see the discussion below), but it is not the answer I expect for a reference solution. Think about what the lock protects and what it blocks.
* **Never call the listener while holding a lock or inside a `compute` lambda.** That is the rule.
* No `Thread.sleep` in production code. Time comes from the injected `Clock`.
* No new dependencies. Plain Java 25.
* Holds do not need a background cleaner. Expiry is checked whenever someone asks (stretch goal 1 changes that).
* Holding several seats at once is out of scope (stretch goal 3).

## How I'd approach it

**Start by reproducing, not by fixing.** Run `mvn test -Dtest='**/task18/**'` on the starter. The single-threaded tests pass. The concurrency tests fail, and most of them fail on every run. Read each failure and ask: which two threads, doing which steps, produced this? Then read the bug table below and check that you agree.

### The bugs in the starter

| # | Where | Category | Symptom | Exposed by |
|---|---|---|---|---|
| 1 | `hold`: `isFree` then `holds.put` | Check-then-act | Two customers both get a hold on the same seat | `exactly_one_of_1000_customers_gets_a_seat_they_all_want` |
| 2 | `confirm`: read the hold, then `holds.remove`, `sold.add` and `soldCount++` in separate steps | Check-then-act | A customer confirming from two threads buys the seat twice | `a_customer_who_confirms_the_same_hold_from_50_threads_buys_the_seat_once` |
| 3 | `soldCount++` | Lost update | The counter says 190 when 200 seats were sold | `no_sale_is_lost_when_200_customers_buy_200_different_seats_at_once` |
| 4 | `sold` is a plain `ArrayList` written by many threads | Unsafe publication, lost update | Entries go missing, or the list gets corrupted | `no_sale_is_lost_when_200_customers_buy_200_different_seats_at_once` |
| 5 | `soldSeats()` returns the internal list, not a copy | Leaking internal state | A caller iterating the list while a sale happens gets `ConcurrentModificationException`, and a list returned earlier changes under the caller | `iterating_the_sold_seats_while_another_thread_sells_does_not_throw`, `a_snapshot_taken_earlier_does_not_change_when_later_sales_happen` |
| 6 | `holds` is a plain `HashMap` read and written by many threads | Unsafe publication | Lost or stale entries, undefined behaviour | Several of the tests above |

The starter has no deadlock. Its listener is called outside any lock. The deadlock is the trap in the "fix attempt" below.

### The fix attempt that looks right and is wrong

A common first reaction is to put `synchronized` on the methods:

```mermaid
sequenceDiagram
    participant T1 as Thread 1 confirm
    participant Lock as Monitor of the service
    participant Ws as Listener sends WebSocket
    participant T2 as Thread 2 asks availableSeats
    T1->>Lock: enter synchronized confirm
    T1->>Ws: seatSold
    Ws->>T2: submit availableSeats and wait
    T2->>Lock: availableSeats is synchronized too
    Note over T1,T2: Thread 1 waits for Thread 2, Thread 2 waits for the lock Thread 1 holds
```

Thread 1 holds the lock and calls the listener. The listener hands the question to another thread and waits. That thread needs the same lock to answer. Nobody can move. The test `a_listener_may_call_back_into_the_service_from_another_thread_without_deadlock` guards against this with `assertTimeoutPreemptively`. It passes on the starter because the starter has no lock to deadlock on, and it passes on the solution. Keep it green.

### The check-then-act race

Here is what happens in the starter when two customers hold the same seat at the same time:

```mermaid
sequenceDiagram
    participant A as Customer A thread
    participant B as Customer B thread
    participant M as holds map
    A->>M: isFree A-12 reads nothing, so yes
    B->>M: isFree A-12 reads nothing, so yes
    A->>M: put A-12 to hold for A
    B->>M: put A-12 to hold for B
    Note over A,B: Both calls returned a hold. The second put silently replaces the first.
```

The check and the act are two separate steps. Between them, another thread can do the same thing. Note that the second `put` does not fail. It **overwrites** the first hold, and the listener has been told about both. That is why the race is silent, and why only a test that counts the winners can see it.

### Why the tests make the race likely

A race like this might happen once in a million calls on a laptop. You do not want a test that passes 999,999 times. Two tools make it reproducible:

1. **`YieldingClock`** ([source](../src/test/java/io/github/chris013/practice/task18/YieldingClock.java)). Its `instant()` calls `Thread.yield()` before it returns the time. `hold` reads the clock between the check and the act, so every yield gives another thread the chance to run in that gap. This is a legitimate technique, not cheating. In production the same gap appears whenever a call to a database or to another service sits between a check and an act. A slow dependency widens the window in exactly the same way. We just do it on purpose.
2. **A start gate.** Every virtual thread waits on one `CountDownLatch`. Then the test releases all of them at the same moment. Without the gate, the threads start one after another and rarely overlap.

### Why virtual threads

A test that wants 1,000 contenders needs 1,000 threads. A platform thread is a real operating system thread with a large stack reserved for it, which makes 1,000 of them expensive. A virtual thread (JEP 444) is cheap to create and cheap to park, so `Executors.newVirtualThreadPerTaskExecutor()` lets the test start 1,000 contenders without strain. The tests also run in well under a second, which is what keeps them in the default test run.

### Two ways to make it correct

**Option A: one global lock.** Put every method behind one `ReentrantLock` or one monitor, and call the listener after you release it. This is simple, obviously correct, and easy to review. It also serialises *everything*: a hold on seat 1 waits for a sale on seat 2. For one concert with a few thousand seats and a few hundred requests a second, that is fine. You should be able to say that out loud in a code review.

**Option B: per-key atomicity.** Store the state of each seat in a `ConcurrentHashMap<SeatId, SeatState>`. Make each state change with `compute(seat, ...)`. The map guarantees that the function runs atomically for one key, and other keys are not blocked. The price is a rule: the function passed to `compute` must be short and must touch only that key. Calling the listener inside it is forbidden, both by the Javadoc and by common sense, because the listener may call back from another thread and touch the same key, which then waits for our `compute` to finish.

Either option works. Pick one, write down why, and check that your choice never calls alien code inside the protected region.

### Why `volatile` does not help

Some people mark `holds` or `soldCount` as `volatile`. That gives visibility: a write becomes visible to other threads. It does not make `count++` atomic, and it does not make "check, then put" atomic. Two threads can both read `null` and both write. `volatile` is the right tool for a single flag written by one thread and read by others. It is the wrong tool for a read-modify-write.

### Stop before you write the solution

Write down, in one sentence each, the answer to these: which operations are check-then-act? Which collection is shared without safe publication? Which method returns internal state? Where is the listener called, and which lock (if any) is held at that moment? Only then start on the code.

## Tests

Run: `mvn test -Dtest='**/task18/**'`

Two test classes:

* [`SeatReservationServiceTest`](../src/test/java/io/github/chris013/practice/task18/SeatReservationServiceTest.java) checks every rule with one thread. It passes on the starter, so it protects the *behaviour* while you change the *structure*.
* [`SeatReservationConcurrencyTest`](../src/test/java/io/github/chris013/practice/task18/SeatReservationConcurrencyTest.java) checks the races. On the starter five of its six tests fail. The sixth (the listener that calls back) passes on the starter and must keep passing after your change, because it is the guard against the deadlock.

On the starter 20 of 25 tests pass: all 19 in `SeatReservationServiceTest` and the callback test. The other 5 concurrency tests fail. On the solution all 25 pass.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task18/SeatReservationConcurrencyTest.java -->
**[`SeatReservationConcurrencyTest.java`](../src/test/java/io/github/chris013/practice/task18/SeatReservationConcurrencyTest.java)**

```java
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
```
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task18/YieldingClock.java -->
**[`YieldingClock.java`](../src/test/java/io/github/chris013/practice/task18/YieldingClock.java)**

```java
package io.github.chris013.practice.task18;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock that always says the same time, but gives other threads a chance to run first on every reading.
 *
 * <p>This is a legitimate way to widen a race window. In production the same gap appears when a call to a database
 * or to a remote service sits between the "check" and the "act". Here we make the gap wide on purpose, so the bug
 * shows up in a test on every run instead of once in a million.
 */
final class YieldingClock extends Clock {

    private final Instant now;

    YieldingClock(Instant now) {
        this.now = now;
    }

    @Override
    public Instant instant() {
        Thread.yield();
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("Not needed in these tests");
    }
}
```
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task18/SeatReservationServiceTest.java -->
**[`SeatReservationServiceTest.java`](../src/test/java/io/github/chris013/practice/task18/SeatReservationServiceTest.java)**

```java
package io.github.chris013.practice.task18;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SeatReservationServiceTest {

    private static final Instant START = Instant.parse("2026-06-01T20:00:00Z");
    private static final Duration HOLD = Duration.ofMinutes(5);
    private static final SeatId A1 = new SeatId("A-1");
    private static final SeatId A2 = new SeatId("A-2");
    private static final SeatId A3 = new SeatId("A-3");
    private static final CustomerId ANNA = new CustomerId("anna");
    private static final CustomerId BEN = new CustomerId("ben");

    private final MutableClock clock = new MutableClock(START);
    private final RecordingReservationListener listener = new RecordingReservationListener();
    private final SeatReservationService service =
            new SeatReservationService(Set.of(A1, A2, A3), clock, HOLD, listener);

    @Nested
    class Holding {

        @Test
        void a_free_seat_can_be_held_and_the_hold_expires_after_the_hold_duration() {
            Hold hold = service.hold(A1, ANNA).orElseThrow();

            assertThat(hold).isEqualTo(new Hold(A1, ANNA, START.plus(HOLD)));
            assertThat(listener.held()).containsExactly(hold);
        }

        @Test
        void a_held_seat_cannot_be_held_again_even_by_the_same_customer() {
            service.hold(A1, ANNA);

            assertThat(service.hold(A1, ANNA)).isEmpty();
            assertThat(service.hold(A1, BEN)).isEmpty();
            assertThat(listener.held()).hasSize(1);
        }

        @Test
        void a_hold_still_counts_one_nanosecond_before_it_expires() {
            service.hold(A1, ANNA);
            clock.advanceBy(HOLD.minusNanos(1));

            assertThat(service.hold(A1, BEN)).isEmpty();
        }

        @Test
        void an_expired_hold_can_be_taken_by_someone_else_at_the_exact_expiry_instant() {
            service.hold(A1, ANNA);
            clock.advanceBy(HOLD);

            Hold hold = service.hold(A1, BEN).orElseThrow();

            assertThat(hold.customer()).isEqualTo(BEN);
            assertThat(hold.expiresAt()).isEqualTo(START.plus(HOLD).plus(HOLD));
        }

        @Test
        void a_sold_seat_cannot_be_held() {
            service.hold(A1, ANNA);
            service.confirm(A1, ANNA);

            assertThat(service.hold(A1, BEN)).isEmpty();
        }
    }

    @Nested
    class Confirming {

        @Test
        void the_holder_can_confirm_and_the_seat_is_sold() {
            service.hold(A1, ANNA);

            assertThat(service.confirm(A1, ANNA)).isTrue();
            assertThat(service.soldCount()).isEqualTo(1);
            assertThat(listener.sold()).containsExactly(new RecordingReservationListener.SoldEvent(A1, ANNA));
        }

        @Test
        void another_customer_cannot_confirm_someone_elses_hold() {
            service.hold(A1, ANNA);

            assertThat(service.confirm(A1, BEN)).isFalse();
            assertThat(service.soldCount()).isZero();
            assertThat(service.confirm(A1, ANNA)).as("the real holder still can").isTrue();
        }

        @Test
        void an_expired_hold_cannot_be_confirmed() {
            service.hold(A1, ANNA);
            clock.advanceBy(HOLD);

            assertThat(service.confirm(A1, ANNA)).isFalse();
            assertThat(service.soldCount()).isZero();
        }

        @Test
        void a_free_seat_cannot_be_confirmed() {
            assertThat(service.confirm(A1, ANNA)).isFalse();
            assertThat(listener.sold()).isEmpty();
        }

        @Test
        void a_seat_can_only_be_sold_once() {
            service.hold(A1, ANNA);
            service.confirm(A1, ANNA);

            assertThat(service.confirm(A1, ANNA)).isFalse();
            assertThat(service.soldCount()).isEqualTo(1);
            assertThat(listener.sold()).hasSize(1);
        }
    }

    @Nested
    class Releasing {

        @Test
        void releasing_your_own_hold_frees_the_seat_for_others() {
            service.hold(A1, ANNA);

            service.release(A1, ANNA);

            assertThat(service.hold(A1, BEN)).isPresent();
        }

        @Test
        void releasing_someone_elses_hold_does_nothing() {
            service.hold(A1, ANNA);

            service.release(A1, BEN);

            assertThat(service.hold(A1, BEN)).isEmpty();
            assertThat(service.confirm(A1, ANNA)).isTrue();
        }

        @Test
        void releasing_a_sold_seat_does_nothing() {
            service.hold(A1, ANNA);
            service.confirm(A1, ANNA);

            service.release(A1, ANNA);

            assertThat(service.soldCount()).isEqualTo(1);
            assertThat(service.hold(A1, BEN)).isEmpty();
        }
    }

    @Nested
    class Counting {

        @Test
        void every_seat_is_available_at_the_start() {
            assertThat(service.availableSeats()).isEqualTo(3);
            assertThat(service.soldCount()).isZero();
            assertThat(service.soldSeats()).isEmpty();
        }

        @Test
        void held_and_sold_seats_are_not_available_but_expired_holds_are() {
            service.hold(A1, ANNA);
            service.hold(A2, BEN);
            service.confirm(A2, BEN);

            assertThat(service.availableSeats()).isEqualTo(1);

            clock.advanceBy(HOLD);

            assertThat(service.availableSeats()).as("A-1's hold has expired, A-2 is still sold").isEqualTo(2);
        }

        @Test
        void sold_seats_are_listed_in_the_order_they_were_sold() {
            service.hold(A3, ANNA);
            service.confirm(A3, ANNA);
            service.hold(A1, BEN);
            service.confirm(A1, BEN);

            assertThat(service.soldSeats()).containsExactly(A3, A1);
            assertThat(service.soldCount()).isEqualTo(2);
        }
    }

    @Nested
    class Validation {

        @Test
        void an_unknown_seat_is_rejected_by_every_operation() {
            SeatId unknown = new SeatId("Z-99");

            assertThatThrownBy(() -> service.hold(unknown, ANNA)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.confirm(unknown, ANNA)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.release(unknown, ANNA)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void the_hold_duration_must_be_positive() {
            assertThatThrownBy(() -> new SeatReservationService(Set.of(A1), clock, Duration.ZERO, listener))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new SeatReservationService(Set.of(A1), clock, Duration.ofSeconds(-1), listener))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void null_arguments_are_rejected() {
            assertThatThrownBy(() -> new SeatReservationService(null, clock, HOLD, listener))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("seats");
            assertThatThrownBy(() -> service.hold(A1, null)).isInstanceOf(NullPointerException.class);
        }
    }
}
```
<!-- /embed -->

</details>

## Definition of done

- [ ] All tests green, and the whole task's test run takes well under 5 seconds.
- [ ] The concurrency tests pass on 10 runs in a row, and the starter fails them on 5 runs in a row. Your PR says how you checked.
- [ ] Every shared structure is either a concurrent collection used with `compute`/atomic operations, or is guarded by one lock that never covers a listener call.
- [ ] The listener is never called while a lock is held or inside a `compute` lambda.
- [ ] `soldSeats()` returns a snapshot, not a live view.
- [ ] No `Thread.sleep` was added to any test to "fix" timing.
- [ ] Your PR description names the lock or the per-key strategy you chose and says why you rejected the other one.

## Stretch goals

1. **Expire holds with a scheduled cleanup.** Right now an expired hold is only noticed when someone asks. Add a `ScheduledExecutorService` that removes expired holds every few seconds, and inject its schedule into the service so tests stay deterministic. What does the cleanup race with? (Hint: `confirm` and the cleanup both look at the same hold.)
2. **Measure the two options.** Write a JMH benchmark (in a scratch project, not in this repository) with 16 threads that hold and release random seats. Compare one global lock against `compute`. Which one scales better, and does the answer change if the listener takes 1 ms?
3. **All-or-nothing groups.** A customer wants seats `A-1`, `A-2` and `A-3` together. Add `holdAll(Set<SeatId>, CustomerId)` that holds all of them or none. Why does per-key `compute` no longer give you this? What is the usual fix (lock ordering, or one lock for the group), and what does each cost?
4. **Make `availableSeats()` consistent.** Right now it reads seat by seat, so it can see a moment that never existed (one seat sold, another not yet). Can you return a count that matches one point in time, without a global lock? Write down what you would have to give up.
5. **Read the JDK source.** Open `ConcurrentHashMap.compute` and find where it locks the bin. Why does the Javadoc forbid touching other keys in the function? What could happen if you did?

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. One `ConcurrentHashMap` holds the state of each seat, and every change is one `compute` call.** A missing key means the seat is free. `compute` runs atomically for its key, so "check the state, then write the new state" happens as one step for each seat. Two customers racing for seat A-12 queue up on A-12 only. Seat A-13 is never blocked.

**2. The `compute` lambda decides, and we learn the decision by identity.** `hold` creates a candidate `HeldSeat` before calling `compute`. The lambda returns the candidate if the seat is available, otherwise it returns the current state unchanged. Afterwards `result != candidate` tells us whether we won. This avoids a mutable holder such as an `AtomicReference` or a one-element array. The alternative is perfectly fine too, and some teams find it clearer. The identity check is only correct because the candidate is a fresh object that nothing else can hold.

**3. The listener is called after `compute` has returned.** This is the open call from *JCIP* 10.1. Inside `compute` we hold the lock of that key's bin. A listener that calls `hold`, `confirm` or `release` for the same seat from another thread and waits for it deadlocks. Called on the same thread, `ConcurrentHashMap` may throw `IllegalStateException("Recursive update")`. The test's listener does exactly the first of these. Outside, nobody holds anything. The cost is that a listener can see a state that is already published but whose listener call has not happened yet. That is acceptable: the listener is a notification, not the source of truth.

**4. One clock reading per operation, taken before `compute`.** `hold` reads `clock.instant()` once and uses that value for both the availability check and the new expiry. Reading the clock inside the lambda would also work, but the lambda is supposed to stay short, and the clock is alien code too. `availableSeats` takes one reading for the whole count, so all seats are judged at the same instant.

**5. Sale order in a `ConcurrentLinkedQueue`, and snapshots by copy.** The queue is append-only, and its iterator is weakly consistent: it never throws `ConcurrentModificationException`. `soldSeats()` returns `List.copyOf(...)`, a frozen copy. `soldCount()` reads an `AtomicInteger` that is incremented right after the queue add. Both are updated together after the winning `compute` and can briefly lag the seat map by one sale, which is acceptable for a display count. We do not use `ConcurrentLinkedQueue.size()` for it, because that walks the whole queue and its Javadoc says it is typically not very useful in concurrent code. A `synchronizedList` would not help: iterating it still needs the caller to hold its lock, which the caller does not know about.

**6. `release` and `confirm` never touch another customer's state.** `release` uses `isHeldBy(state, customer)`, which ignores expiry: a customer may always drop their own hold, even if it expired a second ago. `confirm` uses `isValidHoldOf(state, customer, now)`, which also requires the hold to be active at `now`. Two small predicates, one per rule, so neither needs a magic "ignore expiry" value.

**7. Why not one global lock?** It would have been correct, and it is a good answer in a code review. We chose per-key atomicity because the same design extends to many concerts and many venues without a bottleneck, and because the rule "never call alien code inside compute" is small enough to hold in one reviewed file. Stretch goal 2 asks you to measure this choice instead of trusting it.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task18/SeatReservationService.java -->
**[`SeatReservationService.java`](../solutions/src/main/java/io/github/chris013/practice/task18/SeatReservationService.java)**

```java
package io.github.chris013.practice.task18;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds and sells seats for one concert. Customers hold a seat while paying, then confirm it.
 *
 * <p>Thread-safe. Every seat has one entry in {@link #states}, and every state change for a seat happens inside
 * {@link Map#compute}, which is atomic per key. Two customers racing for the same seat are serialised on that seat
 * only; other seats are not blocked.
 *
 * <p>The {@link ReservationListener} is never called while a map operation is running. It is alien code and may call
 * back into this service, so calling it under a lock or inside {@code compute} risks a deadlock.
 *
 * <p>Task 18: read {@code tasks/18-seat-reservation-races.md} first.
 */
public final class SeatReservationService {

    private final Set<SeatId> seats;
    private final Clock clock;
    private final Duration holdDuration;
    private final ReservationListener listener;

    /** Absent key means the seat is free. */
    private final Map<SeatId, SeatState> states = new ConcurrentHashMap<>();

    /** Sale order. Append-only, so iterating it while other threads sell is safe. */
    private final Queue<SeatId> soldInOrder = new ConcurrentLinkedQueue<>();

    /** Kept next to the queue so {@link #soldCount()} does not walk the queue. Updated right after each sale. */
    private final AtomicInteger soldCount = new AtomicInteger();

    public SeatReservationService(
            Set<SeatId> seats, Clock clock, Duration holdDuration, ReservationListener listener) {
        this.seats = Set.copyOf(Objects.requireNonNull(seats, "seats"));
        this.clock = Objects.requireNonNull(clock, "clock");
        this.holdDuration = Objects.requireNonNull(holdDuration, "holdDuration");
        this.listener = Objects.requireNonNull(listener, "listener");
        if (holdDuration.isNegative() || holdDuration.isZero()) {
            throw new IllegalArgumentException("holdDuration must be positive: " + holdDuration);
        }
    }

    /**
     * Returns the new hold, or empty when the seat is held by someone else (and the hold is still valid) or sold.
     */
    public Optional<Hold> hold(SeatId seat, CustomerId customer) {
        requireKnown(seat);
        Objects.requireNonNull(customer, "customer");
        Instant now = clock.instant();
        HeldSeat candidate = new HeldSeat(customer, now.plus(holdDuration));

        // The lambda decides atomically. We compare identity afterwards to learn whether it chose our candidate.
        SeatState result = states.compute(seat, (s, current) -> isAvailable(current, now) ? candidate : current);
        if (result != candidate) {
            return Optional.empty();
        }
        Hold hold = new Hold(seat, customer, candidate.expiresAt());
        listener.seatHeld(hold);
        return Optional.of(hold);
    }

    /** True when the customer holds the seat and the hold has not expired. The seat is then sold. */
    public boolean confirm(SeatId seat, CustomerId customer) {
        requireKnown(seat);
        Objects.requireNonNull(customer, "customer");
        Instant now = clock.instant();
        SoldSeat candidate = new SoldSeat(customer);

        SeatState result =
                states.compute(seat, (s, current) -> isValidHoldOf(current, customer, now) ? candidate : current);
        if (result != candidate) {
            return false;
        }
        soldInOrder.add(seat);
        soldCount.incrementAndGet();
        listener.seatSold(seat, customer);
        return true;
    }

    /** Removes the customer's own hold. Does nothing if the seat is free, sold, or held by someone else. */
    public void release(SeatId seat, CustomerId customer) {
        requireKnown(seat);
        Objects.requireNonNull(customer, "customer");
        states.computeIfPresent(seat, (s, current) -> isHeldBy(current, customer) ? null : current);
    }

    public int availableSeats() {
        // One reading of the clock for the whole count, so every seat is judged at the same instant.
        Instant now = clock.instant();
        int available = 0;
        for (SeatId seat : seats) {
            if (isAvailable(states.get(seat), now)) {
                available++;
            }
        }
        return available;
    }

    public int soldCount() {
        return soldCount.get();
    }

    /** A snapshot in sale order. Later sales do not change the returned list. */
    public List<SeatId> soldSeats() {
        return List.copyOf(soldInOrder);
    }

    private static boolean isAvailable(SeatState state, Instant now) {
        return switch (state) {
            case null -> true;
            case HeldSeat held -> !held.isActiveAt(now);
            case SoldSeat _ -> false;
        };
    }

    /** Any hold of this customer, expired or not. Used by release: a customer may always drop their own hold. */
    private static boolean isHeldBy(SeatState state, CustomerId customer) {
        return state instanceof HeldSeat held && held.customer().equals(customer);
    }

    /** A hold of this customer that is still active at {@code now}. Used by confirm. */
    private static boolean isValidHoldOf(SeatState state, CustomerId customer, Instant now) {
        return state instanceof HeldSeat held && held.customer().equals(customer) && held.isActiveAt(now);
    }

    private void requireKnown(SeatId seat) {
        Objects.requireNonNull(seat, "seat");
        if (!seats.contains(seat)) {
            throw new IllegalArgumentException("Unknown seat: " + seat.value());
        }
    }

    /** The states a seat can be in. Immutable, so a value read from the map can be shared between threads. */
    private sealed interface SeatState permits HeldSeat, SoldSeat {}

    private record HeldSeat(CustomerId customer, Instant expiresAt) implements SeatState {

        /** Held while {@code now < expiresAt}. At exactly {@code expiresAt} the hold is over. */
        boolean isActiveAt(Instant now) {
            return now.isBefore(expiresAt);
        }
    }

    private record SoldSeat(CustomerId customer) implements SeatState {}
}
```
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| `synchronized` on some methods only | `hold` is unlocked while `confirm` is locked, so the two still race. A lock only protects the code that takes it. |
| Calling the listener inside a lock or a `compute` lambda | The listener may call back from another thread and wait for it. That is a deadlock, and it only happens in production. |
| Returning the internal list or map | The caller iterates while another thread writes, and gets `ConcurrentModificationException` or a changing view. Return a copy. |
| `Collections.synchronizedList` iterated without holding its lock | The wrapper makes single calls safe, not iteration. Use a concurrent collection, or copy under the lock. |
| `ConcurrentHashMap` with `containsKey` then `put` | Each call is atomic, the pair is not. Use `compute`, `putIfAbsent`, or `computeIfAbsent`. |
| `Thread.sleep` in a test to "make the race happen" | It makes the test slow and still flaky. Use a start gate and a yielding clock to widen the window instead. |
| Ignoring hold expiry in `availableSeats` | The count shows seats as taken after their hold has ended, and customers are told the venue is full. |

</details>
