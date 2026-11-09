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
                    () -> bus.publish(ORDER), EventDeliveryException.class);

            assertThat(failure.getSuppressed()).containsExactly(second, third);
        }

        @Test
        void exposes_the_event_that_could_not_be_delivered() {
            bus.subscribe(OrderPlaced.class, failing(new IllegalStateException("boom")));

            EventDeliveryException failure = catchThrowableOfType(
                    () -> bus.publish(ORDER), EventDeliveryException.class);

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
