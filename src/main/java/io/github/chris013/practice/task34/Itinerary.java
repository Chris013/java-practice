package io.github.chris013.practice.task34;

import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * A traveller's trip: when they leave, when they are back, and the cities they visit.
 *
 * <p>Task 34: read {@code tasks/34-immutable-itinerary.md} first. This is legacy code. It compiles and its public
 * signatures are the ones other teams already use, but it is mutable and it leaks its internals.
 */
public class Itinerary {

    private String traveller;
    private Date departure;
    private Date returnDate;
    private List<String> cities;

    public Itinerary(String traveller, Date departure, Date returnDate, List<String> cities) {
        if (traveller == null || traveller.isBlank()) {
            throw new IllegalArgumentException("traveller must not be blank");
        }
        if (departure.after(returnDate)) {
            throw new IllegalArgumentException("departure must not be after the return date");
        }
        if (cities == null || cities.isEmpty()) {
            throw new IllegalArgumentException("an itinerary needs at least one city");
        }
        this.traveller = traveller;
        this.departure = departure;
        this.returnDate = returnDate;
        this.cities = cities;
    }

    public String traveller() {
        return traveller;
    }

    public Date departure() {
        return departure;
    }

    public Date returnDate() {
        return returnDate;
    }

    public List<String> cities() {
        return cities;
    }

    public long durationInDays() {
        return ChronoUnit.DAYS.between(departure.toInstant(), returnDate.toInstant());
    }

    public Itinerary withExtraCity(String city) {
        cities.add(city);
        return this;
    }

    public Itinerary rescheduled(Date newDeparture, Date newReturn) {
        if (newDeparture.after(newReturn)) {
            throw new IllegalArgumentException("departure must not be after the return date");
        }
        this.departure = newDeparture;
        this.returnDate = newReturn;
        return this;
    }
}
