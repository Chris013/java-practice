package io.github.chris013.practice.task34;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * A traveller's trip: when they leave, when they are back, and the cities they visit.
 *
 * <p>Immutable. The public constructor and the accessors still speak {@link Date} because other teams use it, but
 * the class never keeps a caller's {@code Date} or {@code List}, and never hands out its own mutable state. Every
 * input is copied before it is checked, and every output is either an unmodifiable list or a fresh {@code Date}.
 */
public final class Itinerary {

    private final String traveller;
    private final Instant departure;
    private final Instant returnDate;
    private final List<String> cities;

    public Itinerary(String traveller, Date departure, Date returnDate, List<String> cities) {
        this(traveller, snapshot(departure, "departure"), snapshot(returnDate, "returnDate"), cities);
    }

    /** Takes {@link Instant}s, which are already immutable, so validation runs on values nobody else can change. */
    private Itinerary(String traveller, Instant departure, Instant returnDate, List<String> cities) {
        Objects.requireNonNull(traveller, "traveller");
        Objects.requireNonNull(cities, "cities");
        // Copy first: from here on we only look at our own list. This also rejects null elements.
        List<String> copied = List.copyOf(cities);

        String name = traveller.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("traveller must not be blank");
        }
        if (departure.isAfter(returnDate)) {
            throw new IllegalArgumentException("departure must not be after the return date: " + departure
                    + " is after " + returnDate);
        }
        if (copied.isEmpty()) {
            throw new IllegalArgumentException("an itinerary needs at least one city");
        }
        List<String> trimmed = new ArrayList<>(copied.size());
        for (String city : copied) {
            trimmed.add(requireCity(city));
        }

        this.traveller = name;
        this.departure = departure;
        this.returnDate = returnDate;
        this.cities = List.copyOf(trimmed);
    }

    public String traveller() {
        return traveller;
    }

    /** A new {@code Date} on every call, so a caller can change it without touching this itinerary. */
    public Date departure() {
        return Date.from(departure);
    }

    /** A new {@code Date} on every call, so a caller can change it without touching this itinerary. */
    public Date returnDate() {
        return Date.from(returnDate);
    }

    public List<String> cities() {
        return cities;
    }

    public long durationInDays() {
        return ChronoUnit.DAYS.between(departure, returnDate);
    }

    /** Returns a new itinerary with one more city. This one does not change. */
    public Itinerary withExtraCity(String city) {
        Objects.requireNonNull(city, "city");
        List<String> extended = new ArrayList<>(cities);
        extended.add(city);
        return new Itinerary(traveller, departure, returnDate, extended);
    }

    /** Returns a new itinerary with new dates, checked by the same rules as the constructor. */
    public Itinerary rescheduled(Date newDeparture, Date newReturn) {
        return new Itinerary(traveller, snapshot(newDeparture, "newDeparture"), snapshot(newReturn, "newReturn"),
                cities);
    }

    /**
     * Reads the millisecond value once and keeps it. Reading it again later could give a different answer if the
     * caller passed a {@code Date} subclass that overrides {@code getTime()}, and then validation and storage would
     * disagree.
     */
    private static Instant snapshot(Date date, String name) {
        Objects.requireNonNull(date, name);
        return Instant.ofEpochMilli(date.getTime());
    }

    private static String requireCity(String city) {
        String trimmed = city.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("a city must not be blank");
        }
        return trimmed;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Itinerary that)) {
            return false;
        }
        return traveller.equals(that.traveller)
                && departure.equals(that.departure)
                && returnDate.equals(that.returnDate)
                && cities.equals(that.cities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traveller, departure, returnDate, cities);
    }

    @Override
    public String toString() {
        return "Itinerary[traveller=" + traveller + ", departure=" + departure + ", returnDate=" + returnDate
                + ", cities=" + cities + "]";
    }
}
