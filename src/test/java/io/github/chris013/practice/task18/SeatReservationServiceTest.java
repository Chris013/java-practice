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
