# Task 17 · A type-safe event bus

| Difficulty | Topics | Time box |
|---|---|---|
| 🟡 Medium | Generics: bounded wildcards and PECS, `Class<T>` as a type token, typesafe heterogeneous containers · Observer pattern · No unchecked warnings · Exception aggregation with suppressed exceptions · Copy-on-write for safe iteration | 2–3 h |

> **Background reading:** *Effective Java* Items 26–33 (especially Item 31 "Use bounded wildcards to increase API flexibility" and Item 33 "Consider typesafe heterogeneous containers") · GoF, "Observer" · Maurice Naftalin and Philip Wadler, *Java Generics and Collections*, ch. 2 (subtyping and wildcards) · Guava's `EventBus` documentation (for contrast)

---

## The situation

Our webshop is a modular monolith. The modules react to each other's events: billing listens to `OrderPlaced`, the mail module listens to `CustomerRegistered`. We have an `EventBus` already, and it has three problems.

* It stores handlers in a `Map<String, List<Object>>` keyed by the class name. Every listener casts with `instanceof`, so a typo in a name or a missing check fails at run time.
* The generics are raw in most places. The compiler reports **214 unchecked or raw-type warnings** for that package, and everybody has learned to ignore them.
* **One bad listener stopped all the following ones.** An exception in the billing listener meant that the mail listener never ran, and customers did not get their confirmation mail for a week.

I want a bus where the compiler checks the event type for every subscription, where a failing handler cannot silence the others, and where the code has no warnings at all. The generics part is the hard part, and that is where most of your learning will be.

## The domain

| Term | Meaning |
|---|---|
| **Domain event** | A fact that happened in the past: `CustomerRegistered`, `OrderPlaced`. Modelled as the sealed interface `DomainEvent` with records. |
| **Subscription** | The handle returned by `subscribe`. Closing it stops the delivery to that handler. |
| **Handler** | A `Consumer` for one event type, or for a supertype of it. |
| **Dead event** | An event that no handler matched. The bus publishes a `DeadEvent` so somebody can log it or alert on it. |
| **Delivery failure** | A handler threw a `RuntimeException`. The bus reports it after all handlers ran. |

```mermaid
classDiagram
    class EventBus {
        +subscribe(Class type, Consumer handler) Subscription
        +publish(Object event) void
        +publishAll(Iterable events) void
    }
    class Registration {
        <<record>>
        Class type
        Consumer handler
        +matches(Object event) boolean
        +deliver(Object event) void
    }
    class Subscription {
        <<interface>>
        +close() void
    }
    class DeadEvent {
        <<record>>
        Object event
    }
    class EventDeliveryException {
        +event() Object
    }
    class DomainEvent {
        <<sealed interface>>
    }
    EventBus "1" o--> "*" Registration : holds
    EventBus ..> Subscription : returns
    EventBus ..> DeadEvent : publishes
    EventBus ..> EventDeliveryException : throws
    DomainEvent <|.. CustomerRegistered
    DomainEvent <|.. OrderPlaced
```

### What happens during one publish

A failing middle handler must not change what the other handlers see. This diagram shows an `OrderPlaced` with three matching handlers, where the mail handler throws:

```mermaid
sequenceDiagram
    participant Shop as Checkout
    participant Bus as EventBus
    participant Billing as Billing handler
    participant Mail as Mail handler
    participant Audit as Audit handler
    Shop->>Bus: publish OrderPlaced
    Bus->>Billing: accept OrderPlaced
    Billing-->>Bus: done
    Bus->>Mail: accept OrderPlaced
    Mail--xBus: IllegalStateException
    Bus->>Audit: accept OrderPlaced
    Audit-->>Bus: done
    Bus-->>Shop: EventDeliveryException with the mail failure as cause
```

The audit handler still runs. The caller learns about the failure, but only after every handler had its chance.

## What I want from you

Implement the bus in [`EventBus`](../src/main/java/io/github/chris013/practice/task17/EventBus.java). The other main classes are given, and you should read them before you start: [`DomainEvent`](../src/main/java/io/github/chris013/practice/task17/DomainEvent.java), [`DeadEvent`](../src/main/java/io/github/chris013/practice/task17/DeadEvent.java), [`Subscription`](../src/main/java/io/github/chris013/practice/task17/Subscription.java) and [`EventDeliveryException`](../src/main/java/io/github/chris013/practice/task17/EventDeliveryException.java).

1. **`<E> Subscription subscribe(Class<E> eventType, Consumer<? super E> handler)`**
   * A handler subscribed to a type receives every event that is an instance of that type. That includes subtypes and interfaces the type implements.
   * Subscribing to `DomainEvent.class` receives every domain event. Subscribing to `Object.class` receives everything.
   * `null` for either argument throws `NullPointerException`.
   * The same handler may be subscribed more than once. Each subscription is independent.
2. **Delivery order.** `publish` delivers to every matching handler in **subscription order**. That order is global, across event types: a handler for `Object` subscribed before a handler for `OrderPlaced` runs first.
3. **`void publish(Object event)`**
   * `null` throws `NullPointerException`.
   * Every matching handler runs, even when an earlier one throws.
   * If at least one handler threw a `RuntimeException`, `publish` throws an [`EventDeliveryException`](../src/main/java/io/github/chris013/practice/task17/EventDeliveryException.java) **after** all handlers ran. Its cause is the **first** failure, and every later failure is added with `addSuppressed`.
   * `Error`s are not caught. They propagate at once. (That is a JVM-level problem; we don't want to hide it.)
4. **Dead events.** If **no** handler matched the event, `publish` publishes `new DeadEvent(event)` instead. A `DeadEvent` that nobody listens to is dropped silently. It must never recurse.
5. **`void publishAll(Iterable<?> events)`** publishes each event in iteration order, with the rules above. It stops at the first event whose delivery throws an `EventDeliveryException`, and the remaining events are not published. `null` throws `NullPointerException`.
6. **Subscribing and closing during a publish.** A publish works on a snapshot of the subscriptions:
   * A handler that subscribes during a publish does **not** receive the event being published.
   * A handler that is closed during a publish **still** receives the event being published. Later publishes do not deliver to it.
7. **`close()` on a `Subscription`** stops the delivery to its handler. It is **idempotent**: closing twice does nothing, and it never throws. Closing one of two subscriptions for the same handler leaves the other one working.
8. **No unchecked code.** The main code has no `@SuppressWarnings` and no unchecked casts, and it compiles without warnings under `-Xlint:all`. Matching a `Class<E>` to an event goes through `Class.isInstance` and `Class.cast`.

## Generics you need for this task

The bus has the signature `<E> Subscription subscribe(Class<E>, Consumer<? super E>)`. Most of the questions in a design review about it come down to three ideas.

**PECS: Producer Extends, Consumer Super.** A generic type parameter with `? extends T` lets you *read* a `T` out of a structure. A parameter with `? super T` lets you *write* a `T` into it. A `Consumer<? super OrderPlaced>` is a thing that can accept an `OrderPlaced`, and possibly anything above it. Here is the type hierarchy our handlers rely on:

```mermaid
flowchart TD
    Object --> DomainEvent
    DomainEvent --> OrderPlaced
    DomainEvent --> CustomerRegistered
```

Read the arrows as "is a supertype of". A `Consumer<Object>` can handle every event, so it is a valid `Consumer<? super OrderPlaced>`. That is why the test `a_consumer_of_object_can_subscribe_to_a_specific_event` compiles. If the parameter were `Consumer<E>`, a logger written as `Consumer<Object>` could not subscribe to `OrderPlaced` at all, and the API would be less useful for no safety gain. Item 31 of *Effective Java* has the full argument, and Naftalin and Wadler explain why `Consumer<Object>` is not a subtype of `Consumer<OrderPlaced>` in the first place (generics are invariant).

```mermaid
flowchart LR
    Events[Iterable of OrderPlaced] -->|producer, read as Object, extends| Bus[EventBus]
    Bus -->|consumer, write OrderPlaced, super| Logger[Consumer of Object]
```

**Why `Iterable<?>` in `publishAll`?** The bus only *reads* the elements, and it reads them as `Object`. So the widest type is correct: `Iterable<?>` accepts a `List<OrderPlaced>`, a `Set<DomainEvent>` or a `List<Object>`. Writing `Iterable<Object>` would reject the first two, which is the mistake the PECS rule warns about.

**Why `Class<E>` and not `E.class`?** Type erasure removes `E` at compile time. Inside `subscribe`, the bus has no way to ask "is this event an `E`?" unless it gets a runtime object that knows `E`. That object is the `Class<E>` token. Java has no `E.class` syntax, which is why the caller always passes `OrderPlaced.class` explicitly.

**What is a typesafe heterogeneous container?** It is a container whose keys carry a type, so that `get(Key<T>)` returns a `T` with no cast. Effective Java Item 33 describes it, and `Map<Class<?>, Object>` with `type.cast(value)` on the way out is the standard implementation. Our bus is a close cousin: the key is the event type, the value is the handler, and the type is checked at run time by `Class.isInstance` and `Class.cast`.

**Thread safety.** The bus is safe for concurrent `subscribe`, `close` and `publish`. The reason is the `CopyOnWriteArrayList`: writes copy the list, reads see a consistent snapshot without any lock. Publishing is much more common than subscribing, so this is the right trade-off. Note that a *single* handler can still be called from two threads at the same time, if two publishes run concurrently. That is the handler's problem, and it is documented in the class Javadoc.

## Constraints & non-goals

* No external libraries (no Guava, no Spring). Only the JDK.
* Keep `EventBus` a single `final` class. The `Registration` record may be private and nested.
* No annotation scanning (`@Subscribe`), no priorities and no asynchronous delivery in this task. Those are stretch goals.
* Keep the public API exactly as above. You may add private or package-private helpers.
* Synchronization: the bus must be safe for `subscribe`, `close` and `publish` on different threads. You don't need to make it fast for writers.
* A handler failure is a bug in that handler. The bus reports it; it does not retry, log, or decide what the business does. The caller decides.
* A dead event is information for operations, not an error. Publishing one never throws on its own.
* Nothing in the bus is persistent. If the JVM dies, undelivered events are lost. That is an acceptable trade-off for in-process modules, and it is the reason we don't use this bus for payments.

## How I'd approach it

* **Start with the types, not the bus.** Write `Registration<E>` first and make `matches` and `deliver` work for one registration. The rest is a loop.
* **Use `Class<E>` as a type token.** You cannot write `E.class` or `event instanceof E` in Java, because of **erasure**: at run time, `E` does not exist. A `Class<E>` object carries the type at run time. `isInstance` answers "is this an event for me?", and `cast` returns the event as `E`.
* **Keep the cast in one place.** Do not write `(E) event` anywhere. You don't need to: if `type` and `handler` share the same `E`, `type.cast(event)` already has the type the handler needs. Explain this in your PR description, because the trick is the whole point of the exercise.
* **Keep one list for everything.** A single `List<Registration<?>>` gives you the global subscription order for free: the list keeps insertion order. Don't keep one list per type. Then you would have to merge them to get the order right.
* **Snapshot iteration.** Look at what a `CopyOnWriteArrayList` gives you for free. Iterating over it does not throw `ConcurrentModificationException`, and the iterator sees the list as it was when the loop started. That gives you the reentrancy rules in item 6 without any extra code.
* **Close by identity.** Two registrations with the same type and the same handler are *equal* if `Registration` is a record. If `close` removes "the first equal element", it can remove the wrong subscription. Remove by identity (`==`).
* **Collect failures in one pass.** Keep a `failure` variable, which is `null` at the start. The first failure creates the `EventDeliveryException`. Every later failure goes into `addSuppressed`. Throw it after the loop, never inside it.
* **Dead events need one `boolean`.** Track whether any handler matched. The dead-event rule is then two lines. Be careful with the case where a matching handler failed: it did match, so there is no dead event. Think about which code path that is.
* **Don't catch `Exception` or `Throwable`.** `RuntimeException` is the right type: handlers in this design are not allowed to throw checked exceptions. Catching `Throwable` would also catch `OutOfMemoryError`.
* Look at [`RecordingHandler`](../src/test/java/io/github/chris013/practice/task17/RecordingHandler.java): a generic test double like this is how you test a `Consumer<? super E>` without any mock setup.

## Tests

Run: `mvn test -Dtest='**/task17/**'`

[`EventBusTest`](../src/test/java/io/github/chris013/practice/task17/EventBusTest.java) groups the rules by theme: **Delivery**, **DeadEvents**, **Failures**, **Reentrancy**, **PECS** (the variance rules from "Generics you need for this task" above) and **PublishAll**. Read the **PECS** and **Reentrancy** groups first: they pin down the two subtle rules, and the names of the tests say what they do.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task17/EventBusTest.java -->
**[`EventBusTest.java`](../src/test/java/io/github/chris013/practice/task17/EventBusTest.java)**

```java
package io.github.chris013.practice.task17;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EventBusTest {

    private static final OrderPlaced ORDER = new OrderPlaced("o-1", "c-1", new BigDecimal("42.00"));
    private static final CustomerRegistered CUSTOMER = new CustomerRegistered("c-1", "ada@example.com");

    private final EventBus bus = new EventBus();

    @Nested
    class Delivery {

        @Test
        void delivers_an_event_to_a_handler_for_its_exact_type() {
            RecordingHandler<OrderPlaced> orders = new RecordingHandler<>();
            bus.subscribe(OrderPlaced.class, orders);

            bus.publish(ORDER);

            assertThat(orders.received()).containsExactly(ORDER);
        }

        @Test
        void delivers_to_a_handler_for_a_sealed_interface_so_it_gets_every_subtype() {
            RecordingHandler<DomainEvent> domain = new RecordingHandler<>();
            bus.subscribe(DomainEvent.class, domain);

            bus.publish(ORDER);
            bus.publish(CUSTOMER);

            assertThat(domain.received()).containsExactly(ORDER, CUSTOMER);
        }

        @Test
        void delivers_to_a_handler_for_a_superclass_of_the_record_types() {
            RecordingHandler<Record> anyRecord = new RecordingHandler<>();
            bus.subscribe(Record.class, anyRecord);

            bus.publish(ORDER);
            bus.publish(CUSTOMER);

            assertThat(anyRecord.received()).containsExactly(ORDER, CUSTOMER);
        }

        @Test
        void delivers_everything_to_a_handler_for_object() {
            RecordingHandler<Object> everything = new RecordingHandler<>();
            bus.subscribe(Object.class, everything);

            bus.publish(ORDER);
            bus.publish("a plain string");

            assertThat(everything.received()).containsExactly(ORDER, "a plain string");
        }

        @ParameterizedTest
        @CsvSource({"order, 1", "customer, 1", "text, 0"})
        void matches_only_domain_events_for_a_domain_handler(String kind, int expected) {
            RecordingHandler<DomainEvent> domain = new RecordingHandler<>();
            bus.subscribe(DomainEvent.class, domain);

            bus.publish(eventOf(kind));

            assertThat(domain.received()).hasSize(expected);
        }

        @Test
        void delivers_in_subscription_order_across_event_types() {
            List<String> calls = new ArrayList<>();
            bus.subscribe(DomainEvent.class, event -> calls.add("domain"));
            bus.subscribe(OrderPlaced.class, event -> calls.add("order"));
            bus.subscribe(CustomerRegistered.class, event -> calls.add("customer"));
            bus.subscribe(Object.class, event -> calls.add("everything"));

            bus.publish(ORDER);

            assertThat(calls).containsExactly("domain", "order", "everything");
        }

        @Test
        void stops_delivering_to_a_handler_after_it_is_closed() {
            RecordingHandler<OrderPlaced> orders = new RecordingHandler<>();
            Subscription subscription = bus.subscribe(OrderPlaced.class, orders);
            bus.publish(ORDER);

            subscription.close();
            bus.publish(ORDER);

            assertThat(orders.received()).containsExactly(ORDER);
        }

        @Test
        void closing_twice_is_harmless_and_removes_only_its_own_registration() {
            RecordingHandler<OrderPlaced> orders = new RecordingHandler<>();
            Subscription first = bus.subscribe(OrderPlaced.class, orders);
            bus.subscribe(OrderPlaced.class, orders); // the same handler, subscribed a second time

            first.close();
            first.close();
            bus.publish(ORDER);

            assertThat(orders.received()).containsExactly(ORDER);
        }

        @Test
        void rejects_null_arguments() {
            assertThatNullPointerException().isThrownBy(() -> bus.subscribe(OrderPlaced.class, null));
            assertThatNullPointerException().isThrownBy(() -> bus.subscribe(null, new RecordingHandler<>()));
            assertThatNullPointerException().isThrownBy(() -> bus.publish(null));
            assertThatNullPointerException().isThrownBy(() -> bus.publishAll(null));
        }

        private static Object eventOf(String kind) {
            return switch (kind) {
                case "order" -> ORDER;
                case "customer" -> CUSTOMER;
                default -> "not a domain event";
            };
        }
    }

    @Nested
    class DeadEvents {

        @Test
        void publishes_a_dead_event_when_no_handler_matches() {
            RecordingHandler<DeadEvent> dead = new RecordingHandler<>();
            bus.subscribe(DeadEvent.class, dead);

            bus.publish(CUSTOMER);

            assertThat(dead.received()).containsExactly(new DeadEvent(CUSTOMER));
        }

        @Test
        void does_not_publish_a_dead_event_when_a_handler_matches() {
            RecordingHandler<DeadEvent> dead = new RecordingHandler<>();
            bus.subscribe(DeadEvent.class, dead);
            bus.subscribe(OrderPlaced.class, new RecordingHandler<OrderPlaced>());

            bus.publish(ORDER);

            assertThat(dead.received()).isEmpty();
        }

        @Test
        void does_not_publish_a_dead_event_when_the_only_matching_handler_failed() {
            RecordingHandler<DeadEvent> dead = new RecordingHandler<>();
            bus.subscribe(DeadEvent.class, dead);
            bus.subscribe(OrderPlaced.class, failing(new IllegalStateException("boom")));

            assertThatThrownBy(() -> bus.publish(ORDER)).isInstanceOf(EventDeliveryException.class);

            assertThat(dead.received()).isEmpty();
        }

        @Test
        void silently_drops_an_event_that_nobody_listens_to() {
            assertThatCode(() -> bus.publish(CUSTOMER)).doesNotThrowAnyException();
        }

        @Test
        void silently_drops_a_dead_event_that_nobody_listens_to() {
            assertThatCode(() -> bus.publish(new DeadEvent(CUSTOMER))).doesNotThrowAnyException();
        }

        @Test
        void delivers_a_dead_event_itself_once_when_a_handler_for_it_exists() {
            RecordingHandler<DeadEvent> dead = new RecordingHandler<>();
            bus.subscribe(DeadEvent.class, dead);

            bus.publish(new DeadEvent(CUSTOMER));

            assertThat(dead.received()).containsExactly(new DeadEvent(CUSTOMER));
        }
    }

    @Nested
    class Failures {

        @Test
        void runs_the_other_handlers_even_when_one_throws() {
            RecordingHandler<OrderPlaced> after = new RecordingHandler<>();
            bus.subscribe(OrderPlaced.class, failing(new IllegalStateException("billing is down")));
            bus.subscribe(OrderPlaced.class, after);

            assertThatThrownBy(() -> bus.publish(ORDER)).isInstanceOf(EventDeliveryException.class);

            assertThat(after.received()).containsExactly(ORDER);
        }

        @Test
        void throws_the_first_failure_as_the_cause() {
            IllegalStateException first = new IllegalStateException("first");
            bus.subscribe(OrderPlaced.class, failing(first));
            bus.subscribe(OrderPlaced.class, failing(new IllegalArgumentException("second")));

            assertThatThrownBy(() -> bus.publish(ORDER))
                    .isInstanceOf(EventDeliveryException.class)
                    .hasCause(first);
        }

        @Test
        void attaches_the_later_failures_as_suppressed_exceptions() {
            IllegalStateException second = new IllegalStateException("second");
            IllegalArgumentException third = new IllegalArgumentException("third");
            bus.subscribe(OrderPlaced.class, failing(new IllegalStateException("first")));
            bus.subscribe(OrderPlaced.class, failing(second));
            bus.subscribe(OrderPlaced.class, failing(third));

            EventDeliveryException failure = catchThrowableOfType(
                    EventDeliveryException.class, () -> bus.publish(ORDER));

            assertThat(failure.getSuppressed()).containsExactly(second, third);
        }

        @Test
        void exposes_the_event_that_could_not_be_delivered() {
            bus.subscribe(OrderPlaced.class, failing(new IllegalStateException("boom")));

            EventDeliveryException failure = catchThrowableOfType(
                    EventDeliveryException.class, () -> bus.publish(ORDER));

            assertThat(failure.event()).isSameAs(ORDER);
        }
    }

    @Nested
    class Reentrancy {

        @Test
        void a_handler_subscribed_during_a_publish_misses_the_current_event() {
            RecordingHandler<OrderPlaced> late = new RecordingHandler<>();
            AtomicBoolean subscribed = new AtomicBoolean();
            bus.subscribe(OrderPlaced.class, order -> {
                if (subscribed.compareAndSet(false, true)) {
                    bus.subscribe(OrderPlaced.class, late);
                }
            });

            bus.publish(ORDER);
            assertThat(late.received()).isEmpty();

            bus.publish(ORDER);
            assertThat(late.received()).containsExactly(ORDER);
        }

        @Test
        void a_handler_closed_during_a_publish_still_gets_the_current_event() {
            RecordingHandler<OrderPlaced> victim = new RecordingHandler<>();
            AtomicReference<Subscription> victimSubscription = new AtomicReference<>();
            bus.subscribe(OrderPlaced.class, order -> victimSubscription.get().close());
            victimSubscription.set(bus.subscribe(OrderPlaced.class, victim));

            bus.publish(ORDER);
            assertThat(victim.received()).containsExactly(ORDER);

            bus.publish(ORDER);
            assertThat(victim.received()).containsExactly(ORDER);
        }
    }

    @Nested
    class PECS {

        @Test
        void a_consumer_of_object_can_subscribe_to_a_specific_event() {
            List<Object> logged = new ArrayList<>();
            Consumer<Object> logger = logged::add;
            bus.subscribe(OrderPlaced.class, logger);

            bus.publish(ORDER);

            assertThat(logged).containsExactly(ORDER);
        }

        @Test
        void a_list_of_consumers_for_a_supertype_can_be_subscribed_in_one_go() {
            RecordingHandler<Object> anything = new RecordingHandler<>();
            RecordingHandler<OrderPlaced> orders = new RecordingHandler<>();
            List<Consumer<? super OrderPlaced>> handlers = List.of(anything, orders);
            subscribeAll(OrderPlaced.class, handlers);

            bus.publish(ORDER);

            assertThat(anything.received()).containsExactly(ORDER);
            assertThat(orders.received()).containsExactly(ORDER);
        }

        private void subscribeAll(Class<OrderPlaced> type, List<Consumer<? super OrderPlaced>> handlers) {
            handlers.forEach(handler -> bus.subscribe(type, handler));
        }
    }

    @Nested
    class PublishAll {

        @Test
        void publishes_each_event_in_iteration_order() {
            RecordingHandler<Object> all = new RecordingHandler<>();
            bus.subscribe(Object.class, all);

            bus.publishAll(List.of(ORDER, CUSTOMER));

            assertThat(all.received()).containsExactly(ORDER, CUSTOMER);
        }

        @Test
        void accepts_any_iterable_not_only_lists() {
            RecordingHandler<OrderPlaced> orders = new RecordingHandler<>();
            bus.subscribe(OrderPlaced.class, orders);

            bus.publishAll(Set.of(ORDER));

            assertThat(orders.received()).containsExactly(ORDER);
        }

        @Test
        void stops_at_the_first_event_whose_delivery_fails() {
            RecordingHandler<CustomerRegistered> customers = new RecordingHandler<>();
            bus.subscribe(OrderPlaced.class, failing(new IllegalStateException("boom")));
            bus.subscribe(CustomerRegistered.class, customers);

            assertThatThrownBy(() -> bus.publishAll(List.of(ORDER, CUSTOMER)))
                    .isInstanceOf(EventDeliveryException.class);

            assertThat(customers.received()).isEmpty();
        }
    }

    private static <E> Consumer<E> failing(RuntimeException failure) {
        return event -> {
            throw failure;
        };
    }
}
```
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task17/RecordingHandler.java -->
**[`RecordingHandler.java`](../src/test/java/io/github/chris013/practice/task17/RecordingHandler.java)**

```java
package io.github.chris013.practice.task17;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A test handler that remembers every event it received, in order. */
final class RecordingHandler<E> implements Consumer<E> {

    private final List<E> received = new ArrayList<>();

    @Override
    public void accept(E event) {
        received.add(event);
    }

    List<E> received() {
        return List.copyOf(received);
    }
}
```
<!-- /embed -->

</details>

## Definition of done

- [ ] All 28 tests in `EventBusTest` are green.
- [ ] The main code has no `@SuppressWarnings`, no raw types and no unchecked casts. `mvn -B -q test-compile` prints no warnings.
- [ ] Subscription order is global across event types, and that is pinned by a test.
- [ ] A failing handler does not stop the others, and the exception has the first failure as its cause and the others as suppressed.
- [ ] A snapshot is used for the delivery, and both reentrancy rules hold.
- [ ] Your PR description explains how `Registration.deliver` type-checks without an unchecked cast.

## Stretch goals

1. **Asynchronous delivery on virtual threads.** Add a bus variant that delivers each handler on its own virtual thread (JEP 444, `Executors.newVirtualThreadPerTaskExecutor()`). What happens to the exception aggregation? You can no longer throw one exception from `publish` as the handlers finish later. Decide what the caller gets, and write it down.
2. **Annotation scanning.** Add a `@Subscribe` method annotation and register all annotated methods of an object by reflection. Explain why we *didn't* do this in the starter: which errors move from compile time to start-up time, and how do you find them?
3. **Ordering by priority.** Let a subscription carry an `int priority` and deliver in priority order, with the subscription order as the tie-breaker. How does the copy-on-write list help or hurt you here?
4. **Generic event types.** Consider `record EntityChanged<T>(T entity)`. The class token `EntityChanged.class` cannot tell `EntityChanged<Customer>` from `EntityChanged<Order>`. Explain what a *super type token* is (the trick that Guava's `TypeToken` uses, and that Neal Gafter popularised), what it lets you do, and what it still cannot do.
5. **Unsubscribe by handler.** Add `void unsubscribe(Consumer<?> handler)`. How do you decide which subscriptions it removes, and why is this API a smell if the same handler can be registered twice?

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. One registration list, and the global order comes from it.** All subscriptions go into a single `CopyOnWriteArrayList<Registration<?>>`. Insertion order is subscription order, so "delivery in subscription order across types" is a property of the data structure, not an algorithm. The rejected alternative was a `Map<Class, List>` with a merge step at publish time, which is more code and gets the order wrong as soon as two types are involved.

**2. The type check happens in one private record.** `Registration<E>(Class<E> type, Consumer<? super E> handler)` holds the matching pair. The list holds `Registration<?>`, so the bus cannot name `E`. It doesn't need to: the body of `deliver` is type-checked once, for an arbitrary `E` that `type` and `handler` share, so `type.cast(event)` is an `E` and `Consumer<? super E>` accepts it. Try the same thing from outside, `registration.handler().accept(registration.type().cast(event))`, and it does not compile. Each call on a `Registration<?>` gets its own fresh capture (`CAP#1`, `CAP#2`), and the compiler cannot prove they are the same type. Moving the code *into* the generic type gives both fields one name for the type, and that is the whole trick. No cast is written by us, so there is nothing to suppress. The rejected alternative was storing `Consumer<Object>` and casting the event to `E` through an unchecked cast with `@SuppressWarnings`. That compiles, but it lies to the compiler, and the warning it hides is the one the task is meant to remove.

**3. Snapshot semantics for free.** Publishing iterates over the `CopyOnWriteArrayList`, which iterates over an array that is never modified in place. A handler subscribed during the publish is not in that array, and a handler closed during the publish is still in it. This is exactly the behaviour the tests pin down. The rejected alternative was an `ArrayList` with a `synchronized` block around publish. That serializes all publishes behind one lock, so a slow handler blocks every other thread, and a handler that waits for another thread to subscribe deadlocks. A plain `ArrayList` also throws `ConcurrentModificationException` on subscribe-during-publish.

**4. Close by identity.** `Subscription.close()` removes its own `Registration` with `removeIf(r -> r == registration)`. Because `Registration` is a record, `equals` compares type and handler, and two registrations of the same handler would be equal. Removing "an equal one" would close the wrong subscription. `removeIf` on a copy-on-write list is atomic, and calling it twice is harmless: the second call finds nothing.

**5. Failures are collected, and thrown once.** The loop catches `RuntimeException` only, keeps the first failure as the cause of an `EventDeliveryException`, and attaches every later one as suppressed. The exception is thrown after the loop, so every handler ran. An `Error` is not caught. The rejected alternative was to stop at the first failure, which is exactly the bug from the situation section, or to catch `Throwable`, which would also swallow `OutOfMemoryError`.

**6. Dead events come from a single flag.** `deliverToMatchingHandlers` returns whether any handler matched. `publish` publishes a `DeadEvent` only when nothing matched and the event is not already a `DeadEvent`. The second condition is what stops the recursion. A matching handler that failed still counts as matched, so the exception is thrown and no dead event follows.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task17/EventBus.java -->
**[`EventBus.java`](../solutions/src/main/java/io/github/chris013/practice/task17/EventBus.java)**

```java
package io.github.chris013.practice.task17;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * An in-process publish/subscribe bus for domain events. Modules subscribe to the event types they care about
 * and never import each other's classes.
 *
 * <p>Thread safety: {@code subscribe}, {@code close} and {@code publish} may run on different threads. The
 * registrations live in a {@link CopyOnWriteArrayList}, so reads (publishing) are lock-free and every publish
 * delivers to a snapshot. That is why a handler that subscribes or closes during a publish does not change the
 * delivery in progress, and why nothing ever throws {@code ConcurrentModificationException}.
 * Writes copy the whole list, which is fine because subscriptions change rarely compared to publishing.
 * Two concurrent publishes may call the same handler at the same time; handlers must be thread-safe themselves.
 */
public final class EventBus {

    // One list for all types: its insertion order is the subscription order, which gives us the global ordering.
    private final List<Registration<?>> registrations = new CopyOnWriteArrayList<>();

    /**
     * Registers {@code handler} for {@code eventType} and for every subtype of it. Subscribing to {@code Object.class}
     * receives every event.
     *
     * <p>Handlers run in the order in which they were subscribed, across all event types.
     *
     * @return a subscription that stops the delivery when closed
     */
    public <E> Subscription subscribe(Class<E> eventType, Consumer<? super E> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        Registration<E> registration = new Registration<>(eventType, handler);
        registrations.add(registration);
        // Remove by identity. Two registrations of the same type and handler are equal records, and closing one of
        // them must not remove the other.
        return () -> registrations.removeIf(existing -> existing == registration);
    }

    /**
     * Delivers {@code event} to every matching handler, in subscription order.
     *
     * <p>A failing handler does not stop the others. After all of them ran, if any threw a
     * {@link RuntimeException}, this method throws an {@link EventDeliveryException}. If no handler matched at all,
     * the bus publishes a {@link DeadEvent} instead.
     *
     * @throws NullPointerException if {@code event} is null
     * @throws EventDeliveryException if at least one handler failed
     */
    public void publish(Object event) {
        Objects.requireNonNull(event, "event");
        boolean delivered = deliverToMatchingHandlers(event);
        if (!delivered && !(event instanceof DeadEvent)) {
            // Only a DeadEvent that nobody listens to is dropped. Wrapping it again would recurse forever.
            publish(new DeadEvent(event));
        }
    }

    /**
     * Publishes each event in iteration order, one {@link #publish} after the other. Stops at the first event whose
     * delivery fails.
     *
     * @throws EventDeliveryException if a handler failed for one of the events
     */
    public void publishAll(Iterable<?> events) {
        Objects.requireNonNull(events, "events");
        for (Object event : events) {
            publish(event);
        }
    }

    /**
     * Runs every matching handler, even after a failure. Returns whether at least one handler matched.
     * Only {@link RuntimeException}s are collected: an {@link Error} means the JVM is in trouble, and we let it
     * propagate at once.
     */
    private boolean deliverToMatchingHandlers(Object event) {
        boolean matched = false;
        EventDeliveryException failure = null;
        for (Registration<?> registration : registrations) {
            if (!registration.matches(event)) {
                continue;
            }
            // Set before delivering: a matching handler that fails still counts as matched, so no DeadEvent follows.
            matched = true;
            try {
                registration.deliver(event);
            } catch (RuntimeException handlerFailure) {
                if (failure == null) {
                    failure = new EventDeliveryException(event, handlerFailure);
                } else {
                    failure.addSuppressed(handlerFailure);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        return matched;
    }

    /**
     * One subscription: an event type and the handler for it.
     *
     * <p>How the types line up without unchecked casts: the bus stores {@code Registration<?>} because it cannot name
     * the event type of each subscription. Inside {@code deliver} the compiler sees one {@code E} that both
     * {@code type} and {@code handler} share, so {@code type.cast(event)} returns an {@code E} that the
     * {@code Consumer<? super E>} accepts. The cast is checked at run time by {@code Class.cast}. {@code matches}
     * runs first, so that a non-matching event is skipped instead of throwing.
     */
    private record Registration<E>(Class<E> type, Consumer<? super E> handler) {

        boolean matches(Object event) {
            return type.isInstance(event);
        }

        void deliver(Object event) {
            handler.accept(type.cast(event));
        }
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task17/EventDeliveryException.java -->
**[`EventDeliveryException.java`](../solutions/src/main/java/io/github/chris013/practice/task17/EventDeliveryException.java)**

```java
package io.github.chris013.practice.task17;

/**
 * Given. Thrown by {@link EventBus#publish} after every matching handler has run, when at least one of them failed.
 * The first failure is the {@linkplain #getCause() cause}; the later ones are {@linkplain #getSuppressed() suppressed}.
 */
public final class EventDeliveryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // Transient: the event may not be serializable, and the exception is only used in-process.
    private final transient Object event;

    EventDeliveryException(Object event, RuntimeException firstFailure) {
        super("A handler failed while delivering " + event.getClass().getName(), firstFailure);
        this.event = event;
    }

    /** The event that could not be delivered to every handler. */
    public Object event() {
        return event;
    }
}
```
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| Raw `Consumer` or `Class` in fields and parameters | The compiler can no longer check the event type, and the 214-warning problem is back. |
| `@SuppressWarnings("unchecked")` on the whole class, with `(E) event` inside | It hides the warning and removes the check. `Class.cast` gives the same result with a real check. |
| `Consumer<E>` instead of `Consumer<? super E>` | A logger typed as `Consumer<Object>` cannot subscribe to `OrderPlaced`. The API is needlessly rigid. |
| One failing handler stops the others | This is the bug from the situation section. Collect the failures and run every handler. |
| Swallowing exceptions, or logging and continuing without telling the caller | The publisher then believes the event was delivered. Throw an `EventDeliveryException` after the loop. |
| Looping over an `ArrayList` and subscribing inside a handler | `ConcurrentModificationException` at run time. A copy-on-write list or a snapshot avoids it. |
| Keying the subscriptions by class name strings | Two classes with the same simple name collide, and the `instanceof` checks come back. Key by `Class<?>`. |
| Exact-class matching only (`event.getClass() == type`) | A handler for `DomainEvent` silently gets nothing. `isInstance` matches subtypes and interfaces. |

</details>
