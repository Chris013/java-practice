package io.github.chris013.practice.task18;

/**
 * Given. Told about every change to the seat map. In production the implementation pushes a WebSocket message to
 * every browser showing the map, so it is <em>alien code</em>: it can be slow, and it can call back into the service.
 */
public interface ReservationListener {

    void seatHeld(Hold hold);

    void seatSold(SeatId seat, CustomerId customer);
}
