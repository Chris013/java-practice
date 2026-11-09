package io.github.chris013.practice.task05;

import java.time.YearMonth;
import java.util.List;

/** What a member did at the gym in one month. */
record MonthlyUsage(int classes, int saunaVisits, int totalVisits) {

    static MonthlyUsage of(YearMonth month, List<Visit> visits) {
        List<Visit> inMonth = visits.stream().filter(visit -> YearMonth.from(visit.date()).equals(month)).toList();
        return new MonthlyUsage(count(inMonth, "CLASS"), count(inMonth, "SAUNA"), inMonth.size());
    }

    private static int count(List<Visit> visits, String kind) {
        return (int) visits.stream().filter(visit -> visit.kind().equals(kind)).count();
    }
}
