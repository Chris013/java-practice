package io.github.chris013.practice.task18;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A fake listener that remembers every event. Thread-safe, so the concurrency tests can share it. */
final class RecordingReservationListener implements ReservationListener {

    record SoldEvent(SeatId seat, CustomerId customer) {}

    private final List<Hold> held = new CopyOnWriteArrayList<>();
    private final List<SoldEvent> sold = new CopyOnWriteArrayList<>();

    @Override
    public void seatHeld(Hold hold) {
        held.add(hold);
    }

    @Override
    public void seatSold(SeatId seat, CustomerId customer) {
        sold.add(new SoldEvent(seat, customer));
    }

    List<Hold> held() {
        return List.copyOf(held);
    }

    List<SoldEvent> sold() {
        return List.copyOf(sold);
    }
}
