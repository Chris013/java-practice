# Task 19 · Test-drive a working-day calendar

| Difficulty | Topics | Time box |
|---|---|---|
| 🟡 Medium | Test-driven development: red, green, refactor, test list, triangulation, fake it · `java.time` (`LocalDate`, `DayOfWeek`, `MonthDay`) · Holiday rules as small objects · Implementing an algorithm from a reference (Easter) | 3 h |

> **Background reading:** Kent Beck, *Test-Driven Development: By Example*: Part I (the Money example) and the Part III patterns "Test List", "Fake It" and "Triangulation" · Kent Beck, "Canon TDD" (2023, on his *Tidy First?* Substack) · *Modern Java in Action* (Urma, Fusco, Mycroft), ch. 12 "New Date and Time API" · The "Anonymous Gregorian algorithm" for Easter by Meeus, Jones and Butcher (the Wikipedia article "Date of Easter" describes it)

---

## The situation

Our German logistics company promises "delivery in 3 working days". The promise is shown on the checkout page and printed on the shipping label. Today the code counts Monday to Friday and forgets the holidays. A parcel posted on the Tuesday before Easter is promised for Good Friday, and the customer service inbox fills up every time a holiday comes after a long weekend.

A first draft exists. It is one 300-line class with a `Set<LocalDate>` of holidays typed in by hand for 2026. Nobody knows what happens in 2027, and nobody can say which test covers Easter.

We want a `WorkingDayCalendar` that we can trust, and we will build it **test-first**. This is not dogma. The rules are full of edge cases: weekends, leap days, Easter, and two states that disagree about Corpus Christi. A test list and small steps find those edge cases before the code does. A big class written in one go finds them in production.

## The domain

| Term | Meaning |
|---|---|
| **Working day** | A Monday to Friday that is not a public holiday in the calendar's region. |
| **Public holiday** | A named day that closes the offices. Nationwide holidays apply everywhere; state holidays apply only in that state. |
| **Holiday rule** | How the date of a holiday is computed for a given year: a fixed day of the year, or a number of days before or after Easter Sunday. |
| **Easter Sunday** | Not stored in a table. It is computed for each year, and it moves between 22 March and 25 April. |
| **Working days after a date** | When we add 3 working days to Thursday, the count starts on Friday. The start day itself is not counted. |

The rules are small objects, and the calendar is a list of them:

```mermaid
classDiagram
    class WorkingDayCalendar {
        +germany() WorkingDayCalendar
        +germany(GermanState) WorkingDayCalendar
        +isWorkingDay(LocalDate) boolean
        +holidayOn(LocalDate) Optional
        +addWorkingDays(LocalDate, int) LocalDate
        +workingDaysBetween(LocalDate, LocalDate) long
        +holidaysIn(int) List
    }
    class Holiday {
        <<interface>>
        +name() String
        +dateIn(int) Optional
    }
    class FixedHoliday {
        +String name
        +MonthDay day
    }
    class EasterBasedHoliday {
        +String name
        +int daysAfterEasterSunday
    }
    class Easter {
        +sunday(int) LocalDate
    }
    class GermanState {
        <<enumeration>>
        BAVARIA
        BERLIN
        NORTH_RHINE_WESTPHALIA
    }
    WorkingDayCalendar "1" o--> "many" Holiday : holds
    Holiday <|.. FixedHoliday
    Holiday <|.. EasterBasedHoliday
    EasterBasedHoliday ..> Easter : uses
    WorkingDayCalendar ..> GermanState : germany(state)
```

Easter week 2026 shows why the calendar matters. Thursday 2 April is the last working day before a four-day weekend:

```mermaid
gantt
    title Easter week 2026, Easter Sunday is 5 April
    dateFormat YYYY-MM-DD
    axisFormat %a %d
    section Working days
    Mon 30 Mar       :active, d1, 2026-03-30, 1d
    Tue 31 Mar       :active, d2, 2026-03-31, 1d
    Wed 1 Apr        :active, d3, 2026-04-01, 1d
    Thu 2 Apr        :active, d4, 2026-04-02, 1d
    Tue 7 Apr        :active, d5, 2026-04-07, 1d
    section Not working
    Good Friday      :crit, h1, 2026-04-03, 1d
    Sat and Sun      :h2, 2026-04-04, 2d
    Easter Monday    :crit, h4, 2026-04-06, 1d
```

## What I want from you

Implement the calendar in the starter. The types are in [`src/main/java/io/github/chris013/practice/task19`](../src/main/java/io/github/chris013/practice/task19/). Every rule below must be covered by a test that you wrote.

1. **[`GermanState`](../src/main/java/io/github/chris013/practice/task19/GermanState.java)** is given. Do not change it. It has three values for now.

2. **[`FixedHoliday`](../src/main/java/io/github/chris013/practice/task19/FixedHoliday.java)`(name, day)`** is a record. `dateIn(year)` returns the date of the holiday in that year. On 29 February in a non-leap year the holiday does not happen, so `dateIn` returns an empty `Optional`. A `null` name or day throws `NullPointerException`.

3. **[`EasterBasedHoliday`](../src/main/java/io/github/chris013/practice/task19/EasterBasedHoliday.java)`(name, daysAfterEasterSunday)`** is a record. It returns the date that many days after Easter Sunday. Negative numbers are allowed: Good Friday is `-2`. This holiday happens every year.

4. **[`Easter`](../src/main/java/io/github/chris013/practice/task19/Easter.java)`.sunday(year)`** returns the date of Easter Sunday in the Gregorian calendar. Use the Anonymous Gregorian algorithm. For a year before 1583 it throws `IllegalArgumentException`.

5. **[`WorkingDayCalendar`](../src/main/java/io/github/chris013/practice/task19/WorkingDayCalendar.java)`.germany()`** contains the nationwide holidays:

   | Holiday | Rule |
   |---|---|
   | New Year's Day | 1 January |
   | Good Friday | Easter Sunday minus 2 days |
   | Easter Monday | Easter Sunday plus 1 day |
   | Labour Day | 1 May |
   | Ascension Day | Easter Sunday plus 39 days |
   | Whit Monday | Easter Sunday plus 50 days |
   | German Unity Day | 3 October |
   | Christmas Day | 25 December |
   | Boxing Day | 26 December |

6. **`WorkingDayCalendar.germany(GermanState)`** adds the state holidays to the nationwide ones:

   | State | Holidays added |
   |---|---|
   | `BAVARIA` | Epiphany (6 January), Corpus Christi (Easter plus 60 days), Assumption Day (15 August), All Saints' Day (1 November) |
   | `BERLIN` | International Women's Day (8 March) |
   | `NORTH_RHINE_WESTPHALIA` | Corpus Christi (Easter plus 60 days), All Saints' Day (1 November) |

7. **`isWorkingDay(date)`** is true if the date is a Monday to Friday and no holiday of this calendar falls on it. Dates before 1583 are rejected, see rule 13.

8. **`holidayOn(date)`** returns the name of the holiday on that date, or an empty `Optional`. A holiday on a Saturday or Sunday still has a name. Example: German Unity Day 2026 is a Saturday, and `holidayOn` still names it. If two holidays fall on the same date, it returns the first one in calendar order: the nationwide holidays come first, then the state holidays in the order `germany(state)` adds them.

9. **`addWorkingDays(start, days)`** returns the date reached after `days` working days. The count starts **after** `start`. Thursday 2 April 2026 plus 3 working days is Thursday 9 April 2026. If `days` is negative, throw `IllegalArgumentException`. If `days` is `0`, return `start` when it is a working day, otherwise the next working day.

10. **`workingDaysBetween(startInclusive, endExclusive)`** counts the working days in that half-open range. Equal dates give `0`. If the end is before the start, throw `IllegalArgumentException`.

11. **`holidaysIn(year)`** returns the holiday dates of that year, in date order and without duplicates. It includes holidays that fall on a weekend. Two holidays can share a date: in 2008 Ascension Day (Easter plus 39 days, with Easter on 23 March) and Labour Day are both 1 May, so the list shows that date once and `holidaysIn(2008)` has eight entries.

12. Any `null` argument throws `NullPointerException` with the parameter name, for example `Objects.requireNonNull(date, "date")`.

13. **Years before 1583 are rejected.** Easter is undefined before the Gregorian calendar, so every method throws `IllegalArgumentException` for a date or year before 1583, even on a weekend or on 1 January. Check this first, not as a side effect of computing Easter.

## Constraints & non-goals

* **Test first.** Write the next test from your test list, watch it fail, then write the code that makes it pass. Do not write the whole class and then the tests. Your commit history will show it, and so will the reviewer.
* **The acceptance test is the north star, not a thing to edit.** [`DeliveryDateAcceptanceTest`](../src/test/java/io/github/chris013/practice/task19/DeliveryDateAcceptanceTest.java) is given. Do not change its expected values to make it pass.
* **No external holiday library** and no HTTP holiday API. The rules are small enough to own them.
* **No `LocalDate.now()`, no `Clock`.** Every method receives its dates as arguments. A calendar that knows "today" is a different, harder problem.
* **No hardcoded dates per year.** A `Map<Integer, LocalDate>` of Easter dates works until 2031 and then fails silently. The rule is the knowledge, not the table.
* **No mutable static state.** A calendar is immutable. Two calendars must never share a list that someone can change.
* **Only the three states from the enum.** Adding the other 13 is a stretch goal, not part of this task.

## How I'd approach it

Every cycle has the same four steps, and the loop ends when the list is empty:

```mermaid
flowchart LR
    L[Pick next item from the test list] --> R[Red: write one failing test]
    R --> G[Green: simplest code that passes]
    G --> F[Refactor: remove duplication while green]
    F --> Q{List empty}
    Q -- no --> L
    Q -- yes --> A[Run the acceptance test]
```

Keep the steps separate. Red proves that the test can fail and that it talks about new behaviour, so a green bar later means something. Green lets you write the dumbest code that works, which keeps the design honest, because you cannot yet guess what the code needs. Refactor is allowed only while the bar is green, so every change in it is a change of structure and not of behaviour. If you mix them, a failure tells you nothing about which step caused it.

**Start with a test list, not with a test.** Write the list on paper or in a comment, one behaviour per line. Do not write the algorithm in the list. Here is a starting list. It will grow, and you will cross items off as you go:

- [ ] Monday to Friday without a holiday is a working day
- [ ] Saturday and Sunday are not working days
- [ ] A fixed holiday falls on the same day every year
- [ ] 29 February gives an empty result in a non-leap year
- [ ] Easter Sunday for several years (triangulate, see below)
- [ ] Good Friday is two days before Easter Sunday
- [ ] Each nationwide holiday is named on the right date
- [ ] A holiday on a weekend is still named but is not a working day
- [ ] Bavaria, Berlin and North Rhine-Westphalia each add their own holidays
- [ ] `addWorkingDays` with `0` on a working day returns the same day
- [ ] `addWorkingDays` with `0` on a weekend returns the next working day
- [ ] `addWorkingDays` skips weekends and holidays
- [ ] `addWorkingDays` with a negative number throws
- [ ] `workingDaysBetween` excludes the end date and returns `0` for equal dates
- [ ] `workingDaysBetween` throws when the end is before the start
- [ ] `holidaysIn` is sorted and includes weekend holidays
- [ ] Null arguments throw `NullPointerException`

**Outside-in or inside-out?** Both, in turns. The acceptance test is outside-in: it describes the feature as the business sees it, and it fails for a reason you can read. It tells you *that* something is missing, but it does not tell you *which small class* to write next. For that, use inside-out unit tests: `FixedHoliday` first, then `Easter`, then the calendar. Each unit test is small, so each red bar has one cause. Go back to the acceptance test after every few green steps, and see which of its lines turned green.

**The Easter test is where triangulation lives.** Start with one assertion: Easter 2026 is 5 April. The simplest green code is `return LocalDate.of(2026, 4, 5)`. That is *fake it*: the code is wrong, but the bar is green in a minute. Now add 2027. The constant fails, and you are forced to think. You could add a second `if`. Do not, yet. Add 2038, 1818 and 2285 too. Different centuries and different places in the 19-year cycle make a table of answers as large as the algorithm. When the next expected value needs more logic than the code has, stop faking and write the algorithm. Look it up in the Anonymous Gregorian reference above. It is integer arithmetic on the year, about ten lines, and it has no table.

**Know when to stop faking.** A fake is good for one step. When two tests force the same duplication, the duplication is the design. Replace the fake with the real rule, and keep every test green while you do it. This is the refactoring step: remove duplication, rename, move code, but only while the bar is green.

**Baby steps.** Each cycle should take a few minutes. If you have been red for 15 minutes, your test is too big. Split it. Typical good first tests: a Monday is a working day, a Saturday is not. Typical bad first tests: "a year of Berlin working days". Keep the year count for later, as a test that you can explain in one sentence.

**Holidays as rules, not as dates.** `Holiday.dateIn(year)` is the seam that makes the calendar work for every year. Keep the rule object small and let the calendar ask it for a date. Do not store the dates. Also keep the lists of holidays in `germany()` and `germany(state)`, so a reader can see every rule in one place.

**Walking the days.** `addWorkingDays` and `workingDaysBetween` can walk one day at a time. That is simple and fast enough for a year. Make sure the loop always ends: validate the input first, and remember that a `LocalDate` near the end of its range throws an exception instead of wrapping around.

**Which tests belong where.** The acceptance test checks business scenarios. Your unit tests check one rule each. If a unit test needs a calendar with three states and a year of data to make sense, it is an acceptance test in disguise. Move it, or cut it down.

## Tests

Run: `mvn test -Dtest='**/task19/**'`

Before you write any production code, the given acceptance test should be red. It fails with `UnsupportedOperationException` for every scenario. That is the starting point, not a bug.

[`DeliveryDateAcceptanceTest`](../src/test/java/io/github/chris013/practice/task19/DeliveryDateAcceptanceTest.java) is the outside-in north star. Write your own unit tests in a new class, for example `WorkingDayCalendarUnitTest` in the same test package. Do not put them into the acceptance test.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task19/DeliveryDateAcceptanceTest.java -->
<!-- /embed -->

</details>

## Definition of done

- [ ] `DeliveryDateAcceptanceTest` is green, and you did not edit it.
- [ ] Every item of your test list is a passing test, or you crossed it out with a reason.
- [ ] Easter is tested for at least four years from different centuries or cycle positions.
- [ ] Every holiday rule in the table appears in `germany()` or `germany(state)`, with no date typed in for a specific year.
- [ ] There are no mutable static fields, and no `LocalDate.now()` anywhere.
- [ ] `addWorkingDays` and `workingDaysBetween` reject bad input with the right exception, and their loops always end.
- [ ] Your commit history shows the red-green-refactor steps, one test at a time.

## Stretch goals

1. **A holiday that moves with the weekday.** Repentance Day is the Wednesday before 23 November, and it exists only in Saxony. Write it as a third `Holiday` implementation. Does `dateIn(year)` still fit? Where would you need more information than the year, and what would you change in the interface?
2. **Cache the holidays per year.** `isWorkingDay` looks up every holiday for every day you ask about. Where would you cache the result? How do you keep the cache from growing forever, and is it still thread-safe?
3. **Another country.** Build a calendar for Austria with its own holidays. How much code can the two calendars share? Compare a constructor that takes a list of holidays with an enum of countries. Which one stops you from forgetting a holiday?
4. **Property-based round trip.** For any start date and any `n >= 1`, the result of `addWorkingDays(start, n)` should satisfy: `workingDaysBetween(start.plusDays(1), result.plusDays(1))` equals `n`. Write the test with a loop over many start dates and many `n`. Why is the `plusDays(1)` on both ends needed, and why does the property fail for `n = 0` on a weekend?

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. Holidays are rules, not dates.** Each `Holiday` answers "when is this in year *y*?" and nothing else. The calendar never stores a date. The alternative, a `Map` of years to dates, is the first attempt most people write, and it stops working on the first of January 2027.

**2. `dateIn` returns an `Optional`, and an empty one means "does not happen this year".** A 29 February holiday has no date in 2027. Throwing an exception for that would force every caller to catch it for an ordinary situation. An empty `Optional` says what happens, and the type makes the caller handle it.

**3. Easter is computed, with its limit written down.** The Anonymous Gregorian algorithm is integer arithmetic. It is valid only for the Gregorian calendar, so `Easter.sunday` rejects years before 1583 instead of returning a wrong date. The alternative, a third-party holiday library, was ruled out by the constraints. The calendar checks the year itself, at its boundary, so the limit does not depend on which holiday happens to be checked first.

**4. The calendar is immutable.** `WorkingDayCalendar` copies its holiday list with `List.copyOf`, and the national list is a `private static final` constant. A public mutable list would let any caller change every calendar in the JVM, and tests would leak state into each other.

**5. A `switch` over the enum without a `default` branch.** If someone adds a fourth state and forgets its holidays, the compiler stops the build. A `default` that throws would only fail at runtime, for the one customer who uses that state.

**6. Walk the days, do not jump.** `addWorkingDays` and `workingDaysBetween` step through the days one at a time. A year is 365 steps, and each step checks a handful of rules. Jumping by weeks would be faster, but it adds code that is hard to check. Measure before you optimise.

**7. The test order is the design.** The tests are ordered as a TDD session produces them. The Easter tests come before `EasterBasedHoliday` uses Easter. The single-date tests come before the counting tests. Read the test file top to bottom and you can see which rule was discovered first and which one depends on it.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task19/Easter.java -->
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task19/FixedHoliday.java -->
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task19/EasterBasedHoliday.java -->
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task19/WorkingDayCalendar.java -->
<!-- /embed -->

The unit tests, in the order a TDD session produces them. They are a second reference, not the only right answer. The order of the nested classes is the order of the test list:

1. `WeekdaysAndWeekends`: Monday to Friday are working days, Saturday and Sunday are not.
2. `FixedDates`: a fixed holiday repeats every year, 29 February is empty in a non-leap year, null checks.
3. `EasterSunday`: triangulation with ten years from four centuries, which no constant or small table can satisfy.
4. `EasterOffsets`: Good Friday is `-2`, and the other offsets of the national table.
5. `NationalHolidays`: each holiday is named on its date, a weekend holiday is still named, and a date with two holidays gets the first one in calendar order.
6. `StateHolidays`: each state adds its own holidays, and Corpus Christi differs between Bavaria and Berlin.
7. `AddingWorkingDays`, `CountingWorkingDays`, `HolidaysInYear` and `ArgumentChecks`: the counting rules, the whole-year totals per state, the null checks and the 1583 limit.


<!-- embed: solutions/src/test/java/io/github/chris013/practice/task19/WorkingDayCalendarTest.java -->
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| All tests written first, then the code | You guessed the API before you knew what it needed. Red-green cycles would have shaped it. |
| A giant first test: "a year of Berlin working days" | Red for a dozen reasons at once. You cannot tell which line to write next. |
| Easter tested for one year only (2026) | A constant `return LocalDate.of(2026, 4, 5)` passes. Triangulate with other years and other centuries. |
| A loop with no input check and no end condition | A negative `days` returns the start date by accident, or a bad loop runs for ever. Validate first, and know when the loop ends. |
| A mutable `static List<Holiday>` for the national holidays | Any caller can add a holiday to every calendar in the JVM, and tests leak state into each other. |
| Hardcoded holiday dates per year | Works for the year you typed in. The rule is the knowledge, the table is not. |

</details>
