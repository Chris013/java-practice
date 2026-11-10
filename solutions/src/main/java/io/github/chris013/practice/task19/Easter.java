package io.github.chris013.practice.task19;

import java.time.LocalDate;

/**
 * Computes the date of Easter Sunday in the Gregorian calendar.
 *
 * <p>Uses the "Anonymous Gregorian algorithm" (Meeus, Jones and Butcher). It is pure integer arithmetic, so it
 * is fast and needs no table of dates. The algorithm is only valid for the Gregorian calendar, which began in
 * October 1582 in most of Europe; 1583 is the first full year of it.
 */
public final class Easter {

    static final int FIRST_GREGORIAN_YEAR = 1583;

    private Easter() {
    }

    /**
     * @throws IllegalArgumentException if {@code year} is before 1583, when the Gregorian calendar was introduced
     */
    public static LocalDate sunday(int year) {
        if (year < FIRST_GREGORIAN_YEAR) {
            throw new IllegalArgumentException(
                    "Easter is only computed for years from " + FIRST_GREGORIAN_YEAR + ", was " + year);
        }
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(year, month, day);
    }
}
