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

        SeatState result = states.compute(seat, (s, current) -> isHeldBy(current, customer, now) ? candidate : current);
        if (result != candidate) {
            return false;
        }
        soldInOrder.add(seat);
        listener.seatSold(seat, customer);
        return true;
    }

    /** Removes the customer's own hold. Does nothing if the seat is free, sold, or held by someone else. */
    public void release(SeatId seat, CustomerId customer) {
        requireKnown(seat);
        Objects.requireNonNull(customer, "customer");
        states.computeIfPresent(seat, (s, current) -> isHeldBy(current, customer, null) ? null : current);
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
        return soldInOrder.size();
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

    /** {@code now == null} means "ignore expiry", which release uses: a customer may always drop their own hold. */
    private static boolean isHeldBy(SeatState state, CustomerId customer, Instant now) {
        return state instanceof HeldSeat held
                && held.customer().equals(customer)
                && (now == null || held.isActiveAt(now));
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
