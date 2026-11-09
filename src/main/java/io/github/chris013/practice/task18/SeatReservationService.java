package io.github.chris013.practice.task18;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Holds and sells seats for one concert. Customers hold a seat while paying, then confirm it.
 *
 * <p>This is the version someone wrote in a hurry. It passes every single-threaded test, but it is not safe when
 * several customers use it at the same time.
 *
 * <p>Task 18: read {@code tasks/18-seat-reservation-races.md} first.
 */
public final class SeatReservationService {

    private final Set<SeatId> seats;
    private final Clock clock;
    private final Duration holdDuration;
    private final ReservationListener listener;

    private final Map<SeatId, Hold> holds = new HashMap<>();
    private final List<SeatId> sold = new ArrayList<>();
    private int soldCount;

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

    public Optional<Hold> hold(SeatId seat, CustomerId customer) {
        requireKnown(seat);
        if (isFree(seat)) {
            Hold hold = new Hold(seat, customer, clock.instant().plus(holdDuration));
            holds.put(seat, hold);
            listener.seatHeld(hold);
            return Optional.of(hold);
        }
        return Optional.empty();
    }

    public boolean confirm(SeatId seat, CustomerId customer) {
        requireKnown(seat);
        Hold hold = holds.get(seat);
        if (hold == null || !hold.customer().equals(customer) || !clock.instant().isBefore(hold.expiresAt())) {
            return false;
        }
        holds.remove(seat);
        sold.add(seat);
        soldCount++;
        listener.seatSold(seat, customer);
        return true;
    }

    public void release(SeatId seat, CustomerId customer) {
        requireKnown(seat);
        Hold hold = holds.get(seat);
        if (hold != null && hold.customer().equals(customer)) {
            holds.remove(seat);
        }
    }

    public int availableSeats() {
        int available = 0;
        for (SeatId seat : seats) {
            if (isFree(seat)) {
                available++;
            }
        }
        return available;
    }

    public int soldCount() {
        return soldCount;
    }

    public List<SeatId> soldSeats() {
        return sold;
    }

    private boolean isFree(SeatId seat) {
        if (sold.contains(seat)) {
            return false;
        }
        Hold current = holds.get(seat);
        return current == null || !clock.instant().isBefore(current.expiresAt());
    }

    private void requireKnown(SeatId seat) {
        Objects.requireNonNull(seat, "seat");
        if (!seats.contains(seat)) {
            throw new IllegalArgumentException("Unknown seat: " + seat.value());
        }
    }
}
