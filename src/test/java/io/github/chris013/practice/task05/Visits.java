package io.github.chris013.practice.task05;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Test data helper: creates visits without caring about exact dates. */
final class Visits {

    static final YearMonth MARCH = YearMonth.of(2026, 3);

    private Visits() {}

    static List<Visit> visits(int classes, int sauna, int gym) {
        return Stream.of(inMarch(classes, "CLASS"), inMarch(sauna, "SAUNA"), inMarch(gym, "GYM"))
                .flatMap(List::stream)
                .toList();
    }

    static List<Visit> inMarch(int count, String kind) {
        return IntStream.range(0, count)
                .mapToObj(i -> new Visit(MARCH.atDay(1 + i % MARCH.lengthOfMonth()), kind))
                .toList();
    }

    static List<Visit> concat(List<Visit> first, List<Visit> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }

    static Visit visitOn(String date, String kind) {
        return new Visit(LocalDate.parse(date), kind);
    }
}
