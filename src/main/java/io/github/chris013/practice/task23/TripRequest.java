package io.github.chris013.practice.task23;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

/** What the customer searched for. Task 23. */
public record TripRequest(String from, String to, LocalDate departure, int passengers) {

    private static final Pattern IATA_CODE = Pattern.compile("[A-Z]{3}");

    public TripRequest {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(departure, "departure");
        if (!IATA_CODE.matcher(from).matches()) {
            throw new IllegalArgumentException("from must be an IATA code like AMS, got " + from);
        }
        if (!IATA_CODE.matcher(to).matches()) {
            throw new IllegalArgumentException("to must be an IATA code like LIS, got " + to);
        }
        if (passengers < 1 || passengers > 9) {
            throw new IllegalArgumentException("passengers must be between 1 and 9, got " + passengers);
        }
    }
}
