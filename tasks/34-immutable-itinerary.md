# Task 34 · The itinerary that changed by itself: immutability and defensive copies

| Difficulty | Topics | Time box |
|---|---|---|
| 🟢 Easy | Minimizing mutability · Defensive copies (in and out) · Copy before validating · Why not `clone()` for untrusted input · `List.copyOf` · Final classes | 1 h |

> **Background reading:** *Effective Java* 3rd ed. Item 17 ("Minimize mutability"), Item 50 ("Make defensive copies when needed", the `Period` example), Item 13 ("Override clone judiciously"), Item 15 ("Minimize the accessibility of classes and members"), Item 19 ("Design and document for inheritance or else prohibit it") · *Java Concurrency in Practice* ch. 3.4 ("Immutability", optional)

---

## The situation

Our tour operator has a booking system. Every booking holds an `Itinerary`: who travels, when they leave, when they are back, and which cities they visit. Other teams build itineraries from their own code, and they pass in `java.util.Date`. We cannot change that API, because too many callers depend on it.

Last week customer support saw two bad itineraries:

* **The return date was before the departure date.** The booking screen showed a trip of minus three days.
* **A city appeared that the customer never booked.** Its city list had an extra entry, Reykjavik, which nobody had chosen.

We found both causes in our code, and they are the same bug in two places:

* `Itinerary` **stores the caller's `Date` objects and `List` directly.** One caller builds a `Date`, passes it in, and then calls `setTime()` on the same object in a loop to make the next booking. Every itinerary built in that loop now shares one `Date` object, and that object has the last value of the loop.
* `Itinerary` **hands its own `List` back from `cities()`.** A controller called `cities().add(...)` to add a stop to a booking. That changed the booking without going through `withExtraCity`, and the validation never ran.

The validation itself was fine. It ran once, at construction time, on objects that were still open to change. We need to make the class **immutable behind a `Date`-based API**: nothing a caller does with the objects they passed in or got back may change an itinerary after it has been built.

## The domain

| Term | Meaning |
|---|---|
| **Itinerary** | A traveller, a departure instant, a return instant and a non-empty list of cities. It is a value: two itineraries with the same fields are the same trip. |
| **Immutable** | Its state never changes after the constructor returns. A "changed" itinerary is a new object. |
| **Defensive copy** | A private copy of a mutable object, made so that the outside cannot reach the original. We copy on the way **in** (from parameters) and on the way **out** (from getters). |
| **Wither** | A method such as `withExtraCity` that returns a new object with one field changed, and leaves the old one alone. |
| **Day trip** | An itinerary whose departure and return are the same instant. It is allowed. |
| **Duration in days** | The whole number of days between the two instants, using `ChronoUnit.DAYS`. 23 hours is 0 days. |

### The bug, step by step

```mermaid
sequenceDiagram
    participant Loop as Booking loop
    participant Date as One shared Date object
    participant Trip1 as Itinerary 1
    participant Trip2 as Itinerary 2
    Loop->>Date: setTime to May 1st
    Loop->>Trip1: new Itinerary with the Date
    Trip1->>Date: stores the reference, no copy
    Loop->>Date: setTime to May 9th
    Loop->>Trip2: new Itinerary with the same Date
    Trip2->>Date: stores the same reference again
    Note over Trip1,Date: Trip 1 now has departure May 9th
    Note over Trip2,Date: Both trips share one object that changes
```

Nothing in `Itinerary` is wrong on its own line. The problem is that **the object we store is also owned by someone else**. Our validation ran on May 1st, and the stored value became May 9th afterwards.

### The shape of the change

```mermaid
classDiagram
    class ItineraryBefore {
        -String traveller
        -Date departure
        -Date returnDate
        -List~String~ cities
        +departure() Date
        +cities() List
        +withExtraCity(String) Itinerary
    }
    class ItineraryAfter {
        -String traveller
        -Instant departure
        -Instant returnDate
        -List~String~ cities
        +departure() Date
        +cities() List
        +withExtraCity(String) Itinerary
        +rescheduled(Date, Date) Itinerary
    }
    ItineraryBefore ..> ItineraryAfter : replaced by
```

The public methods keep the same names and types. What changes is what sits behind them. The fields become `private final`. `Date` is converted to `Instant` on the way in, and back to a fresh `Date` on the way out. `withExtraCity` returns a new object.

## What I want from you

This task has **no skeleton**. [`Itinerary`](../src/main/java/io/github/chris013/practice/task34/Itinerary.java) in `src` is the legacy class. It compiles, its signatures are the ones other teams use, and it is mutable and leaky. Your job is to fix it, not to fill in TODOs. Keep every public signature, and make the class immutable.

1. **[`Itinerary`](../src/main/java/io/github/chris013/practice/task34/Itinerary.java)`(traveller, departure, returnDate, cities)`** with `java.util.Date` for the two dates and `List<String>` for the cities.
   * `null` for any argument → `NullPointerException`, including a `null` element in `cities`.
   * `traveller` is trimmed and must not be blank → `IllegalArgumentException`.
   * `departure` must not be after `returnDate`. Equal is allowed (a day trip). A return one millisecond before departure is rejected.
   * `cities` must not be empty. Each city is trimmed and must not be blank.
2. **Accessors:**
   * `String traveller()` returns the trimmed name.
   * `Date departure()` and `Date returnDate()` return a **new** `Date` on every call. A caller who changes the result must not change the itinerary.
   * `List<String> cities()` returns an **unmodifiable** list. Adding to it throws `UnsupportedOperationException`.
3. **`long durationInDays()`**: the whole days between departure and return, `ChronoUnit.DAYS`. Same instant → 0. 23 hours → 0. 24 hours → 1. Three days → 3.
4. **`Itinerary withExtraCity(String city)`** returns a **new** itinerary with the city added. The original does not change. The new city follows the same rules as the constructor (not blank, not null).
5. **`Itinerary rescheduled(Date newDeparture, Date newReturn)`** returns a new itinerary with new dates. The same date rules apply, and the original does not change.
6. **`equals`, `hashCode` and `toString`** cover all four fields. Two itineraries with the same traveller, dates and cities are equal.
7. **The class is `final`, and every field is `private final`.** The tests check this by reflection.

## Constraints & non-goals

* **Keep the public API.** The signatures in the list above are fixed. Other teams pass `java.util.Date`, so that type stays in the public API. Stretch goal 1 is the way to move away from it later.
* **Do not use `clone()`** on the parameters. The rules are in the Key decisions.
* **No libraries.** Plain Java 25, JUnit and AssertJ for the tests.
* Times come from `Date` and `Instant`. There is no `Clock` in this task, because nothing here depends on "now".

## How I'd approach it

* **Work from the attacks backwards.** Read the test class first. Each `@Nested` group in `Attacks` describes one way the caller can reach into your object. Make each one fail on the starter, then make it pass.
* **Decide the internal types first.** A `Date` is mutable, so do not store one. Store `Instant` or `long` instead. `Instant` is immutable and has `isAfter`, `until` and `Date.from` to go back and forth. Write down why in your PR description.
* **Copy before you validate.** Take the snapshot of each `Date` first, then check the snapshot. If you validate the caller's object and copy it later, another thread (or a `Date` subclass that reports different values on each call) can change it in between. This is the time-of-check/time-of-use problem. It is cheap to avoid: read the value once, then use only that value.
* **Read `getTime()` exactly once per date.** It is a public, overridable method. A `Date` subclass can return one value now and a different value later.
* **Lists.** Copy the list first with `List.copyOf`. That gives you an unmodifiable copy and rejects `null` elements in one call, but it does not trim. Trim the copy, then copy it again if you need to. Compare `List.copyOf` with `Collections.unmodifiableList`, which is only a read-only **view** on the list you were given. If the caller still has the list, they can still change your itinerary through the view. Find that test in `Attacks`.
* **Withers.** `withExtraCity` should build a new list, and then call the constructor. It should not add to the field and return `this`. The constructor already has every rule, so do not repeat them.
* **Final class.** A non-final class can be subclassed, and a subclass can add mutable state or override a getter. A class that is "immutable" but can be subclassed is not immutable. That's why `Itinerary` is `final` (*Effective Java* Item 19 and Item 17).
* **`Date` is the cautionary tale.** It has `setTime`, `getTime` and `clone`, and all of them are public. `java.time` types such as `Instant`, `LocalDate` and `ZonedDateTime` are immutable. In new code, do not use `Date` at all unless an API forces you to.
* **Records.** If you wrote `record Itinerary(...)`, you would still need a compact constructor that copies the list and the dates, because a record's accessor returns whatever was stored. A record does not give you defensive copies for free.

## Tests

Run: `mvn test -Dtest='**/task34/**'`

[`ItineraryTest`](../src/test/java/io/github/chris013/practice/task34/ItineraryTest.java) has five groups. `Validation` checks the rules from the constructor. `Attacks` tries to change an itinerary through every way a caller can reach it. `Values` checks the durations and equality. `Structure` checks the class and its fields by reflection.

The `Attacks` group uses two `Date` subclasses that the test defines itself. One overrides `clone()` to return `this`, which is what an attacker can do to any copy made with `clone()`. The other reports a different time on each `getTime()` call. Both are ordinary Java, and both break a naive implementation.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task34/ItineraryTest.java -->
<!-- /embed -->

</details>

## Definition of done

- [ ] All tests green, and the test class runs in well under a second.
- [ ] `Itinerary` is `final`, and every field is `private final`.
- [ ] A `Date` passed in, a `Date` returned, and a `List` passed in can all be changed without changing the itinerary.
- [ ] The list returned by `cities()` cannot be modified.
- [ ] `withExtraCity` and `rescheduled` return new objects, and the original is unchanged.
- [ ] Every argument is copied before it is validated, and every `getTime()` is read only once.
- [ ] You can explain, in one sentence, why `clone()` is not a defensive copy for a parameter whose type is not final.

## Stretch goals

1. Add a new public API that uses `Instant` (`Itinerary.of(String, Instant, Instant, List)`), and mark the `Date` constructor `@Deprecated` with a Javadoc that names the replacement. What does a caller of the old constructor lose, if anything? Time zones are not part of the data here, so think about what `Date` and `Instant` each mean.
2. Make `cities` a list of a `City` value object (a record with a trimmed, non-blank `name`) instead of `String`. Which rules move into `City`, and which stay in `Itinerary`?
3. Check the immutability with a property-based test. For example, generate random lists of cities and random dates, then check that a change to any copy never affects the original. Write the generator yourself, without a library.
4. Look at the class with `javap -p` and compare it with a `record` of the same fields. Which parts does the compiler generate for you, and which parts are your responsibility?

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. Store `Instant`, not `Date`.** `Date` is mutable, so storing one means storing a value that a caller can change. `Instant` is immutable, has the same precision (milliseconds in this case), and has `isAfter` and `ChronoUnit.DAYS.between`. The alternative was storing a `long`. That also works, but `Instant` keeps the code readable and makes the duration calculation a one-liner.

**2. Snapshot each `Date` once, before validation.** The public constructor converts each `Date` to an `Instant` with `Instant.ofEpochMilli(date.getTime())`, and the private constructor validates those `Instant`s. The `getTime()` call happens exactly once per date, so the value we check is the value we store. The alternative was `new Date(d.getTime())` followed by validation of the copy. It is correct, but it still goes through a `Date`, and it needs a second `Date` for every getter anyway.

**3. Not `clone()`.** `Date.clone()` is public and overridable, so a `Date` subclass can return `this` (the `EvilDate` test). A copy made with `clone()` is then no copy at all. *Effective Java* Item 50 makes the same point. The constructor-based copy, or here the conversion to `Instant`, always gives a plain `Date` or a value that is not a `Date` at all.

**4. Copy the list first, then validate and trim, then copy again.** `List.copyOf` takes a snapshot and rejects `null` elements. After that we only look at our own copy. We trim each city into a new `ArrayList` and finish with `List.copyOf`, which gives an unmodifiable list that nobody else holds. The alternative, `Collections.unmodifiableList(cities)`, is only a read-only view. The caller can still change the list underneath it.

**5. Two constructors: one for the outside world, one for the inside.** The public constructor takes `Date`, because that is the API we must keep. It converts and delegates to a private constructor that takes `Instant`. The private one holds all the rules, so `withExtraCity` and `rescheduled` reuse them without repeating the checks. The alternative was a static factory method. It would work too, but a private constructor keeps the initialization of the `final` fields in one place.

**6. `withExtraCity` and `rescheduled` return new objects.** Both build a new list or new instants and call the private constructor. There is no mutator and no `return this`. The original is never touched, so a caller who keeps the old itinerary (for a receipt, say) still has the trip that was booked.

**7. `final` class and `private final` fields.** A non-final class can be subclassed, and a subclass could add a mutable field or override `durationInDays`. Making the class `final` closes that door (*Effective Java* Item 17, rule 2, and Item 19). Making the fields `private final` is rules 3 and 4 of Item 17. The reflection test checks both.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task34/Itinerary.java -->
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| `Collections.unmodifiableList(cities)` in the constructor | It is a view on the caller's list. The caller can still change the itinerary by changing their own list. Use `List.copyOf`. |
| Validating the `Date`s, then copying them | Between the check and the copy, another thread or a `Date` subclass can change the value. Copy first, then check the copy. |
| `date.clone()` on a parameter | `Date` is not final, so `clone()` can be overridden to return `this`. Use `Instant.ofEpochMilli(date.getTime())` or `new Date(date.getTime())`. |
| A "final" design that is not `final` | A class that can be subclassed is not immutable. The subclass can add state or change behaviour. Make the class `final`. |
| Returning the internal `Date` from a getter | The caller changes it with `setTime` and changes the itinerary. Return `Date.from(departure)` instead. |
| `withExtraCity` that adds to the list and returns `this` | The original changes, and validation does not run. A wither returns a new object and runs the constructor rules. |
| Mutator-style `rescheduled` that sets the fields and returns `this` | The same problem as above, plus any existing reference to the old trip now shows the new dates. |

</details>
