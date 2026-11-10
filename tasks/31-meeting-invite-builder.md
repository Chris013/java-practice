# Task 31 · A meeting invite with a proper Builder

| Difficulty | Topics | Time box |
|---|---|---|
| 🟢 Easy | Builder pattern · Static factories · Immutability · Validating in `build()` · `toBuilder()` | 1–1.5 h |

> **Background reading:** *Effective Java* 3rd ed., Item 1 ("Consider static factory methods instead of constructors"), Item 2 ("Consider a builder when faced with many constructor parameters"), Item 17 ("Minimize mutability"), Item 49 ("Check parameters for validity"), Item 50 ("Make defensive copies when needed") · GoF *Design Patterns*, "Builder" (and how Bloch's builder differs from the GoF one)

---

## The situation

The calendar service creates meeting invites through one constructor with nine parameters. Five of them are optional:

```java
// Before: the telescoping constructor
new MeetingInvite("anna@x.de", "Planning", start, Duration.ofMinutes(30), null, null, 15, "", attendees)
```

Nobody can read that line without looking up the signature. Last month that caused two bugs:

1. Someone swapped `location` and `description`. Both are `String`, so the compiler said nothing, and the invite showed the room name as the agenda.
2. Someone passed `0` for the reminder, meaning "use the default". `0` is a legal value for us: it means "no reminder". The invite silently had no reminder.

The fix is not "add a comment to the constructor". The fix is to stop asking callers to pass nine positional values. The **Builder** pattern from *Effective Java* Item 2 gives us named steps, and it lets each part of the invite have its own rule.

I want you to build the `MeetingInvite` and its builder. I also want you to understand *which* checks belong to which step, because that decides whether the API is pleasant or dangerous.

## The domain

| Term | Meaning |
|---|---|
| **Organizer** | The person who creates the invite. Required. Stored as a normalized email address. |
| **Attendee** | Someone who is invited. Optional, up to 50. Never the organizer. |
| **Duration** | How long the meeting lasts: 5 minutes to 8 hours, inclusive. |
| **End** | Derived, never stored: `start + duration`. |
| **Reminder** | How long before the start the reminder is sent: 0 (no reminder) to 24 hours. Default 15 minutes. |
| **Location** | Optional. A blank location means "no location". |

```mermaid
classDiagram
    class MeetingInvite {
        +organizer() String
        +title() String
        +start() Instant
        +duration() Duration
        +end() Instant
        +attendees() List
        +location() Optional
        +description() String
        +reminderBefore() Duration
        +toBuilder() Builder
    }
    class Builder {
        +start(Instant) Builder
        +duration(Duration) Builder
        +title(String) Builder
        +attendee(String) Builder
        +attendees(Collection) Builder
        +location(String) Builder
        +description(String) Builder
        +reminderBefore(Duration) Builder
        +build() MeetingInvite
    }
    MeetingInvite --> Builder : creates via toBuilder
    Builder ..> MeetingInvite : build returns
```

`Builder` is a nested class of `MeetingInvite`. The dependency goes both ways: the builder makes the invite, and the invite can make a builder for a copy. The invite does not keep a reference to the builder that made it.

### How a client uses it

```mermaid
sequenceDiagram
    participant C as Client
    participant B as Builder
    participant M as MeetingInvite
    C->>B: builder(organizer, title, start, duration)
    Note over B: null for a required value fails here
    C->>B: attendee(email)
    C->>B: location(room)
    C->>B: reminderBefore(duration)
    C->>B: build()
    B->>M: new MeetingInvite(this)
    Note over M: cross-field rules are checked here
    M-->>C: a valid, immutable invite
```

The null check happens at the line that passes the wrong value. The other rules run inside `build()`, because they need more than one value at a time.

## What I want from you

Create `MeetingInvite` in package `io.github.chris013.practice.task31`. It is a `final` class with a private constructor that takes the builder. Every rule below is checked by a test.

1. **Factory and builder.** [`MeetingInvite.builder(organizer, title, start, duration)`](../src/main/java/io/github/chris013/practice/task31/MeetingInvite.java) takes the four required values. The optional values are set with named setters on the nested [`MeetingInvite.Builder`](../src/main/java/io/github/chris013/practice/task31/MeetingInvite.java). Every setter returns `this`, so calls can be chained.
2. **Null is rejected at the setter.** Every argument that is `null` throws `NullPointerException` with the parameter name as the message: `Objects.requireNonNull(x, "x")`. For `attendee(email)` the name is `email`, for `attendees(emails)` it is `emails`, and a `null` element inside that collection is `email` again.
3. **Title.** Trimmed. Blank → `IllegalArgumentException` with `title` in the message. More than 100 characters after trimming → `IllegalArgumentException` with `title`.
4. **Duration.** From 5 minutes to 8 hours inclusive. Anything else (including zero and negative values) → `IllegalArgumentException` with `duration`.
5. **Email addresses.** The organizer and every attendee are trimmed and lower-cased. Each must contain exactly one `@`, with at least one character before and after it. Otherwise → `IllegalArgumentException` whose message contains `email` and the bad value.
6. **Attendees.**
   * `attendee(email)` adds one, `attendees(emails)` adds all of them, in order. Both can be called many times.
   * Duplicates, after normalization, are dropped silently. The first position is kept.
   * The organizer must not be an attendee (after normalization). Otherwise → `IllegalArgumentException` with `organizer`.
   * At most 50 attendees after de-duplication. A 51st → `IllegalArgumentException` with `attendees`.
7. **Reminder.** From 0 (no reminder) to 24 hours inclusive. Default: 15 minutes. Anything else → `IllegalArgumentException` with `reminder`.
8. **Location.** Trimmed. A blank location means no location: `location()` returns `Optional.empty()`.
9. **Description.** Kept as given. Default: `""`.
10. **Validation happens in `build()`.** The rules in points 3 to 7 are checked there, never in a setter. That is why the order of the setter calls never matters.
11. **Accessors.** `organizer()`, `title()`, `start()`, `duration()`, `end()` (= start + duration), `attendees()` (unmodifiable, in insertion order), `location()` (`Optional<String>`), `description()`, `reminderBefore()`.
12. **Immutability.** The list returned by `attendees()` cannot be changed. A change to the builder after `build()` never changes an invite that was already built, so `build()` copies the attendee list. Calling `build()` twice gives two invites that are equal, but not the same object.
13. **`toBuilder()`** returns a builder with all the values of this invite. It is the way to reschedule: `invite.toBuilder().start(later).build()`.
14. **`equals`, `hashCode`** cover all fields. **`toString()`** looks like `MeetingInvite[Planning, 2026-03-02T09:00:00Z, 30 min, 2 attendees]`.

The starter has all signatures. Every body throws `UnsupportedOperationException("TODO: Task 31")`, so the new tests fail until you implement the behaviour.

## Constraints & non-goals

* Plain Java 25. No Lombok, no libraries. Lombok's `@Builder` would generate most of this code, but we want to see the design, not hide it behind an annotation processor.
* **No `Optional` parameters or `Optional` fields.** `location()` returns an `Optional`, which is fine. Store the value however you like inside the class.
* **No boolean or `null` that means "the default".** A reminder of `0` means "no reminder", and it must not be confused with "not set". Use the default value in the builder's field initializer.
* No `Thread.sleep`, no `Instant.now()`. The tests pass fixed instants.
* No recurrence rules, no time zones, no persistence in this task. Those are stretch goals.

## How I'd approach it

* **Start with the tests.** Read [`MeetingInviteTest`](../src/test/java/io/github/chris013/practice/task31/MeetingInviteTest.java) and find which rule each nested group checks. Some boundary values look odd (the 8-hour duration plus one minute). They are on purpose.
* **Decide where each check belongs before you write it.** Ask yourself: "Can this rule be checked with one argument alone?" If yes, check it in the setter. If it needs two or more fields (the organizer and the attendee list, for example), check it in `build()`.
* **Normalize before you compare.** `Bob@X.de` and `bob@x.de` are the same person. Trim and lower-case first, then de-duplicate. If you de-duplicate first, the 50-attendee limit counts typos as people.
* **Keep the attendee order.** A `Set` that does not keep insertion order is the wrong collection. Think about which Java collection keeps both uniqueness and order.
* **Defensive copies.** The builder keeps a mutable list while you build. The invite must not share that list. Think about when the copy happens: in the setter, in `build()`, or in the accessor, and what each choice costs.
* **The builder is reusable.** After `build()`, the builder still holds the values. Nothing in `build()` may clear it or change it.
* **Equality.** `equals` and `hashCode` must use the same fields. Remember that `Optional` is not a good field type, but you can compare the value inside it.

## Tests

Run the tests for this task:

```bash
mvn test -Dtest='**/task31/**'
```

The tests are in [`MeetingInviteTest`](../src/test/java/io/github/chris013/practice/task31/MeetingInviteTest.java). They use nested groups for each part of the contract. The table tests use `@ValueSource` with the boundary values written in ISO-8601 (`PT5M` is 5 minutes, `PT8H1M` is 8 hours and 1 minute).

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task31/MeetingInviteTest.java -->
<!-- /embed -->

</details>

## Definition of done

- [ ] `MeetingInvite.builder(...)` checks the four required values with `Objects.requireNonNull`.
- [ ] Every setter rejects `null` with the parameter name as the message.
- [ ] `build()` checks title, duration, reminder and the emails, and it never stores a value that failed.
- [ ] The attendees are normalized, de-duplicated (first position kept), checked against the organizer, and limited to 50.
- [ ] `attendees()` is unmodifiable, and the built invite does not share the builder's list.
- [ ] Building twice gives two equal, independent invites. Changing the builder afterwards changes nothing.
- [ ] `toBuilder()` round-trips, and rescheduling changes only the start and the end.
- [ ] `equals`, `hashCode` and `toString` match the contract above.
- [ ] `mvn -B -o clean test-compile` shows no warnings, and all tests in `**/task31/**` pass.

## Stretch goals

1. **A staged builder.** Make the required values impossible to forget at compile time: `MeetingInvite.organizer(...)` returns a step that offers only `title(...)`, then only `start(...)`, and so on, before the optional setters are available. Think about what the compiler error looks like for the caller.
2. **A recurrence rule.** Add `weekly(int count)` to the builder with its own validation (for example, at most 52 occurrences). Decide whether each occurrence is a new `MeetingInvite` or whether the invite holds the rule.
3. **A `withStart(Instant)` wither.** Write `withStart` as a method on `MeetingInvite` that returns a new invite, without going through a builder. Compare the two designs: which one is clearer, and which one has more code that can drift out of date?
4. **Time zones.** Store the start as a `ZonedDateTime` or add an explicit `ZoneId`. Decide whether `equals` should compare the zone, and write a test that proves your choice.

---

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. Required values go into the factory, optional values go into setters.**
The four required values are parameters of `builder(...)`, so a caller cannot build an invite without them, and the compiler checks the count. The optional values are setters, so the call site names each one. The alternative was a builder with an empty constructor and a `build()` that checks for missing values at runtime. That moves a compile-time error into a runtime error for no benefit.

**2. Null is checked at the setter, validity is checked in `build()`.**
A `null` is always a programming error at the line that passes it, so the setter fails there and the stack trace points to the right line. The other rules (the title length, the duration range, the organizer in the attendee list) need several values together, or at least the final value. A setter cannot know that a later `start(...)` will fix an earlier bad `duration(...)`. Checking in `build()` lets the caller set the values in any order.

**3. The invite's constructor is the only place where the rules run.**
`build()` calls `new MeetingInvite(this)`, and the constructor checks everything. The alternative was a static `validate` method called from `build()` and a constructor that trusts its arguments. That is two places that must agree. Because the constructor is private and only reachable from `build()`, there is exactly one path in.

**4. Normalize, then de-duplicate, then count.**
A `LinkedHashSet` removes duplicates and keeps the first position. Normalizing before the set is the important part: `Bob@X.de` and `bob@x.de` must collide. The organizer check and the size limit both use the set after de-duplication.

**5. Defensive copies at both ends.**
The builder collects the raw addresses in its own `ArrayList`. The invite never keeps that list: it stores `List.copyOf(...)` of the normalized set, which is unmodifiable and shares nothing with the builder. The alternative, copying only in `attendees()`, would still let a caller who holds the builder change the invite's state.

**6. `location` is nullable inside, `Optional` outside.**
`Optional` is good as a return type, and bad as a field or parameter: it adds an object per value and says nothing new to the reader. The builder's default is `null`, the constructor turns a blank location into `null`, and the accessor wraps it in `Optional.ofNullable`.

**7. The reminder default is a value, not a flag.**
The builder starts with `DEFAULT_REMINDER` (15 minutes). `0` means "no reminder", and it is a valid value. Nothing in the class uses a `null` or a boolean to say "use the default", so `0` can never be confused with "not set".

**8. `toBuilder()` is the "wither" for a value class with many fields.**
`toBuilder()` copies every field into a new builder. Rescheduling is then one line: `invite.toBuilder().start(later).build()`, and the new invite goes through all the checks again. The alternative, one `withX` method per field, is shorter per field, but it is N methods that each have to know about the others. With `toBuilder()`, the only code that knows all the fields is the builder.

**9. Records do not fix this.**
A record with nine components still has a canonical constructor with nine positional parameters. Two `String` components can still be swapped. A compact constructor can check values, but it cannot give a parameter a default, and it cannot make the caller name the value. A record is a good choice for a value with two or three fields. For nine fields with defaults, a builder is still better.

### Code

#### The invite and its builder

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task31/MeetingInvite.java -->
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| A setter that checks a cross-field rule (for example, "the organizer is not an attendee") | The order of the calls now matters. `attendee(organizer)` before `organizer(...)` is not caught, and a later correction is rejected. Check in `build()`. |
| `build()` that passes the builder's `List` straight into the invite | Later calls to `attendee(...)` change an invite that is already built. Copy the list. |
| `Optional` as a field, or as a parameter of a setter | It adds an object per value and gives callers a second way to say "absent" (`null` or `Optional.empty()`). Use `null` inside, `Optional` as the return type. |
| `0` or `false` or `null` meaning "use the default" | `0` is a valid reminder (no reminder). Give the builder a real default value instead. |
| Public mutable fields on the builder, or a builder that returns a field instead of `this` | Callers can change state the builder is supposed to protect, and chaining breaks. Keep the fields private and return `this`. |
| De-duplicating before normalizing | `Bob@X.de` and `bob@x.de` stay as two attendees, and the 50-attendee limit counts typos. |
| Using `HashSet` for the attendees | The order that the caller gave is lost, so `attendees()` cannot promise insertion order. |

</details>
