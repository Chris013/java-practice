# Task 36 · Reading a sorted access log with streams

| Difficulty | Topics | Time box |
|---|---|---|
| 🟢 Easy | Streams on sorted data: `takeWhile` and `dropWhile` · `flatMap` · `Stream.iterate` with a predicate · `partitioningBy` · `LongSummaryStatistics` · side-effect-free stream functions | 1–1.5 h |

> **Background reading:** *Modern Java in Action* ch. 4 ("Introducing streams"), ch. 5 ("Working with streams": slicing with `takeWhile`/`dropWhile`, mapping with `flatMap`, finding and matching, numeric streams, building streams), ch. 6 ("Collecting data": partitioning) · *Effective Java* 3rd ed. Item 45 ("Use streams judiciously"), Item 46 ("Prefer side-effect-free functions in streams"), Item 47 ("Prefer `Collection` to `Stream` as a return type")

---

## The situation

The ops team asks questions about one day's access log of the shop backend:

* "What happened between 14:00 and 14:15?"
* "What was the first 5xx of the day?"
* "How slow were we?"

The log is written in time order, so the file is already sorted. The script the team uses today has two problems. It uses `forEach` with mutable counters that are changed from inside the loop. And it filters the whole day for every time window, even though the data is sorted and the answer for a window is a contiguous block in the middle of the file.

Streams fit this problem well, but only if we use them for the right things. Some operations are only correct because the data is sorted. Some are only clean if the lambdas are free of side effects. This task is about both.

## The domain

| Term | Meaning |
|---|---|
| **Log entry** | One request: time, user, method, path, status, duration in milliseconds. [`LogEntry`](../src/main/java/io/github/chris013/practice/task36/LogEntry.java) is given and complete. |
| **Access log** | One day's entries, sorted by time. [`AccessLog`](../src/main/java/io/github/chris013/practice/task36/AccessLog.java) is the class you implement. |
| **Half-open window** | `[from, to)`: the start is included and the end is excluded. Two adjacent windows never count the same entry twice. |
| **Server error** | Status 500 to 599. |
| **Client error** | Status 400 to 499. |

The shape of a question about a window is always the same: skip everything before the window, take everything inside it, and stop. The picture below shows the query `between(14:03:11, 14:10:00)` on the sorted log from the tests.

```mermaid
flowchart LR
    e0[Entry 0 at 14-00-02 dropped by dropWhile] --> e1[Entry 1 at 14-00-59 dropped by dropWhile]
    e1 --> e2[Entry 2 at 14-03-11 kept]
    e2 --> e3[Entry 3 at 14-03-11 kept]
    e3 --> e4[Entry 4 at 14-07-45 kept]
    e4 --> e5[Entry 5 at 14-10-00 fails takeWhile and the stream stops]
    e5 --> e6[Entries 6 to 11 are never looked at]
```

`dropWhile` moves past the entries that are too early. `takeWhile` takes entries until the first one that is too late, and then it stops. Both only work if "too early" is followed by "in the window" and then by "too late", with nothing in between. That is what sorting guarantees.

## What I want from you

Implement [`AccessLog`](../src/main/java/io/github/chris013/practice/task36/AccessLog.java). `LogEntry` is complete. Read it first, because the parsing rules and the validation are part of the contract.

1. **`of(List<LogEntry> entries)`** copies the list. Equal times are allowed. If an entry is earlier than the entry before it, throw `IllegalArgumentException`. The message must name the index of the first entry that is out of order.
2. **`parse(List<String> lines)`** skips blank lines and lines that start with `#`. It parses every other line with `LogEntry.parse`. A malformed line fails the whole call (the `IllegalArgumentException` from `LogEntry.parse` is passed through).
3. **`between(Instant fromInclusive, Instant toExclusive)`** returns the entries with `from <= time < to`, in log order. It uses `dropWhile` and `takeWhile`. If `from` is after `to`, throw `IllegalArgumentException`. If `from` equals `to`, the result is empty.
4. **`firstServerError()`** returns the first entry with a 5xx status, or `Optional.empty()`.
5. **`usersSeen()`** returns the distinct user names in the order they first appear.
6. **`pathSegments()`** returns every distinct non-empty segment of every path, split on `/`. `/api/orders/42` contributes `api`, `orders` and `42`. The result is a `SortedSet` that cannot be modified. Use `flatMap`.
7. **`partitionByError()`** returns a `Map<Boolean, List<LogEntry>>`. The key `true` holds the entries with status 400 and up, and the key `false` holds the rest. Both keys are always present, even when one list is empty. The lists cannot be modified.
8. **`durations()`** returns the `LongSummaryStatistics` of `durationMs` over all entries.
9. **`countSlowerThan(Duration threshold)`** counts the entries whose `durationMs` is strictly greater than the threshold, in whole milliseconds.
10. **`requestsPerWindow(Instant start, Instant end, Duration window)`** returns one count for each window `[start + i*window, start + (i+1)*window)`. Windows are produced while the window start is before `end`. Only entries before `end` are counted, even in a last window that runs past `end`. The window must be positive, otherwise throw `IllegalArgumentException`. If `start` is after `end`, throw `IllegalArgumentException`. If `start` equals `end`, the result is empty. Build the window starts with `Stream.iterate(start, t -> t.isBefore(end), t -> t.plus(window))`.
11. Every returned `List` is immutable. Do not use `forEach` with side effects, and do not keep mutable counters outside of collectors.

## Constraints & non-goals

* **No mutable state inside lambdas.** A counter updated by a `forEach` is the thing we are removing. Collectors and reductions are fine, because they are designed for it.
* **No file I/O in `AccessLog`.** The log is passed in as lines. Reading from a file is a stretch goal.
* **No new public methods** beyond the signatures above. Private helpers are welcome.
* **Do not change `LogEntry`.** It is part of the contract for this task, and the tests for it must pass on your starter as they are.
* Keep the log immutable. A query never changes the object.

## How I'd approach it

* **Start with `of` and `parse`.** They are the only place where the sortedness is checked. Everything after them can rely on the sortedness. That is why the check must happen once, in one place. A plain `for` loop with an index is fine here. Item 45 is not a rule against loops. A stream is a good choice when the stream expresses the question ("the first index where the order breaks"), and a loop is a good choice when the code is about the procedure. Choose what reads better to you and be ready to say why.
* **Why `takeWhile` and `dropWhile` only work on sorted data.** Take the unsorted list `14:00 (anna)`, `15:00 (ben)`, `14:30 (cara)` and the query `between(14:00, 15:00)`. With `takeWhile`, you get `[anna]`. The stream stops at `ben`, because `ben` is at 15:00 and is not before `to`. The `cara` entry at 14:30 is inside the window, but it is never seen. With `filter`, you get `[anna, cara]`, which is correct. The `takeWhile` version looks right on sorted input, and it fails quietly on unsorted input. That is why the sortedness check exists.
* **`filter` versus `takeWhile`.** `filter` must look at every entry of the day, every time. `takeWhile` stops at the first entry after the window, so the lines after the window are never read. The saving depends on where the window is. Take a day of 500 000 lines spread evenly over 24 hours, and the window 14:00 to 14:15. `filter` reads all 500 000 lines. `dropWhile` and `takeWhile` together read the lines up to 14:15, about 300 000 of them, and then stop. `dropWhile` still walks every line before the window, so a late window still costs most of the day. A binary search would find the start in logarithmic time. That is an idea for later, not for this task. You can measure the difference with `peek` if you like, but it is not part of the tests.
* **Early stopping is not tested.** Laziness is an implementation detail, and a test would need a counting predicate to check it. The behaviour is what matters: the result is the same either way. Verify the early stop yourself with a `peek` that counts how many entries it sees.
* **`flatMap` for path segments.** `path.split("/")` returns an array for each path. `map(path -> path.split("/"))` gives a `Stream<String[]>`, which is a stream of arrays. `flatMap(path -> Arrays.stream(path.split("/")))` gives one flat `Stream<String>`. Then `filter` out the empty strings that come from the leading slash.
* **`Stream.iterate` with three arguments.** The two-argument form makes an infinite stream, and you would need `limit`, which needs a count. The three-argument form (Java 9) stops by itself when the predicate fails. Use it to produce window starts, and then map each start to a count. Think about what the last window looks like when `end` is not a multiple of the window length. The window still starts before `end`, so it is produced. Its count must still include only entries before `end`.
* **`partitioningBy` versus `groupingBy`.** `partitioningBy` always has both keys, and a `groupingBy` only has the keys that occur. For a yes/no question like "error or not", you want both keys every time, so the caller never has to write `map.getOrDefault(true, List.of())`.
* **Numeric streams.** `mapToLong(LogEntry::durationMs)` gives a `LongStream`. The `summaryStatistics()` call returns a `LongSummaryStatistics` with count, sum, min, max and average in one pass. `map(LogEntry::durationMs)` would box every value into a `Long` first, and a stream of boxed numbers does more work than it needs to.
* **Side effects.** Item 46: the only correct use of `forEach` is at the very end of a pipeline, to report or print. It is not for building a result. If you find yourself writing `list.add(...)` inside a `forEach`, use `collect` or `toList()`.
* **Return `List`, not `Stream`.** Item 47. A `Stream` can only be consumed once, and a getter that returns a `Stream` pushes the work onto every caller. Returning an immutable `List` is cheap here, and it lets the caller use it again.
* **Timing of `between` in `requestsPerWindow`.** Each window calls `between`, which starts again from the beginning of the day. That makes the work O(windows x entries). It is good enough for one day, and it reads clearly. A stretch goal asks you to do better.
* **Work the boundaries by hand before you run anything.** Take the day from the tests. `requestsPerWindow(14:00, 14:05, 5 min)` gives one window, and it counts the four entries before 14:05. The window that would start at 14:05 is not produced, because 14:05 is not before 14:05. Now take `requestsPerWindow(14:00, 14:12, 5 min)`. The last window starts at 14:10 and runs to 14:15, but only entries before 14:12 count, so the 14:12:30 entry is left out. Write the expected lists down first, then check them against the tests.
* **From loops to streams.** Most of the old script maps directly onto one stream operation each. The table shows the translation. Read it as a list of questions to ask about each loop, not as a rule that every loop must become a stream.

| Imperative loop | Stream operation |
|---|---|
| a loop that skips early entries, then copies until the time is too late | `dropWhile` followed by `takeWhile` |
| `count++` inside an `if` in a loop | `filter(...).count()` or `mapToLong(...).filter(...).count()` |
| `out.add(user)` only if `!seen.contains(user)` | `map(LogEntry::user).distinct().toList()` |
| nested loops that call `set.add(segment)` for every path | `flatMap(path -> Arrays.stream(path.split("/")))` and `collect(toCollection(TreeSet::new))` |
| two lists filled by one `if` (`errors.add` and `ok.add`) | `partitioningBy(...)` with a collector for each side |
| running `sum`, `min` and `max` variables | `mapToLong(...).summaryStatistics()` |
| `while (t.isBefore(end)) { ...; t = t.plus(window); }` | `Stream.iterate(start, t -> t.isBefore(end), t -> t.plus(window))` |
| `for` loop that returns the first 5xx, else `Optional.empty()` | `filter(LogEntry::isServerError).findFirst()` |

* **The parsing contract is strict on purpose.** `LogEntry.parse` splits on single spaces, so a double space makes a field empty and the line is rejected. That is what the tests check. A tolerant parser would hide a broken log export, and the ops team would only find out from wrong numbers. Strictness is cheaper to debug than silent bad data.

## Flags I'll raise in review

| Smell | What to do instead |
|---|---|
| `filter` on sorted data where `takeWhile` would stop early | Use `dropWhile` then `takeWhile`, and keep the sortedness check in `of` |
| `takeWhile` on data that is not known to be sorted | Check the order once, in `of`, and document the assumption |
| `forEach` that calls `add` on an outer list | Use `collect` or `toList()` |
| `map(...)` followed by `flatMap` that gives `Stream<Stream<...>>` | Use `flatMap` alone, with a function that returns a `Stream` |
| `map(LogEntry::durationMs)` followed by a sum | Use `mapToLong` so the values stay primitive |
| A getter that returns a `Stream` | Return an immutable `List` or `Set` |

## Tests

Run: `mvn test -Dtest='**/task36/**'`

[`LogEntryTest`](../src/test/java/io/github/chris013/practice/task36/LogEntryTest.java) is already green on your starter, because `LogEntry` is complete. It shows the parsing rules you must respect. [`AccessLogTest`](../src/test/java/io/github/chris013/practice/task36/AccessLogTest.java) runs every method against a fixed 12-entry log. It has the boundary cases: an entry exactly at `from`, an entry exactly at `to`, a window that ends on a boundary, and a last window that runs past the end.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task36/AccessLogTest.java -->
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task36/LogEntryTest.java -->
<!-- /embed -->

</details>

## Definition of done

- [ ] `mvn test -Dtest='**/task36/**'` is green.
- [ ] Every method in `AccessLog` is implemented, with no `TODO` left.
- [ ] No `forEach` with side effects and no mutable counters.
- [ ] `between` uses `dropWhile` and `takeWhile`, and it is correct for `from` equal to `to`.
- [ ] Every window count respects the half-open rule, including the last window.
- [ ] Every returned list is immutable.
- [ ] You can explain, in one sentence, why `takeWhile` gives the wrong answer on unsorted data.

## Stretch goals

1. Write a `Collector` that computes the 95th percentile of `durationMs`. Sort the durations inside a collector finisher, not with a mutable list you pass around.
2. Read the log from a file. Add a static method that takes a `Path`, uses `Files.lines` in a try-with-resources block, and never loads the whole file into memory. Which of your methods can work on a stream directly, and which need a list?
3. Group the entries by hour into a `TreeMap<Integer, List<LogEntry>>`. Use `Collectors.groupingBy` with a `TreeMap::new` supplier. Why is a `TreeMap` the right map for this?
4. Make `requestsPerWindow` a single pass over the entries, not one pass per window. Hint: the entries are sorted, so you can walk the window boundaries and the entries together. Is the result still easy to read?

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. The sortedness check lives in `of`, and every query relies on it.** The copy is made first, so the caller cannot change the list later. The check is an `IntStream` that finds the first index where the order breaks. Everything after `of` assumes sorted data, which is why it is worth checking once. The alternative, checking in every query, costs time and still lets a bad log produce wrong answers.

**2. `between` is `dropWhile` followed by `takeWhile`.** It is the direct expression of "skip the early entries, take the window, stop". The alternative, `filter` with two conditions, reads the whole day every time. The price is that the method is only correct on sorted data, and the class guarantees that.

**3. `requestsPerWindow` builds the windows with the three-argument `Stream.iterate`, and each window reuses `between`.** The predicate stops the stream at `end`, so there is no trailing empty window. Reusing `between` keeps the half-open rule in one place. The price is O(windows x entries). That is acceptable for a day of logs, and a stretch goal removes it.

**4. `partitionByError` uses `partitioningBy` with `toUnmodifiableList`.** Both keys always exist, and the lists are immutable without a second copy. `groupingBy` would drop a key that has no entries, which forces every caller to handle a missing key.

**5. `pathSegments` uses `flatMap` into a `TreeSet`, then wraps it as unmodifiable.** The natural ordering of `TreeSet` gives the sorted result. The alternative, a `HashSet` followed by a sort, works, but it needs two steps and an extra list.

**6. `durations` returns `LongSummaryStatistics` directly.** Callers get count, sum, min, max and average from one pass, and `mapToLong` keeps the values primitive. The alternative, a custom summary record, adds code without adding information.

**7. `LogEntry.parse` wraps every failure with the line.** Field-count errors, bad numbers and the constructor's own checks all produce an `IllegalArgumentException` whose message names the line. The caller sees one kind of error and can find the bad input from the message alone.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task36/AccessLog.java -->
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task36/LogEntry.java -->
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| `filter` for `between` | Reads the whole day each time, and only works by accident of order. `takeWhile` stops early and says what you mean. |
| `takeWhile` with no sortedness check anywhere | Silently drops entries when the input is not sorted. Check once in `of`. |
| `forEach` that adds to an outer list | A side effect in a stream pipeline. It is also not safe if someone adds `parallel()` later. |
| `map(line -> line.split("/"))` followed by `flatMap(Arrays::stream)` on the wrong level | Produces stream-of-stream confusion. One `flatMap` with a function that returns a stream is enough. |
| `map(LogEntry::durationMs)` before `summaryStatistics()` | Boxes each `long` for no reason. Use `mapToLong`. |
| `durations()` returning a `Stream` | The caller gets a stream they can use once, and every query repeats the work. Return the result, not the pipeline. |
| Counting windows with `between(...).size()` but forgetting the cap at `end` | The last window counts entries after `end`. Cap the window end with the earlier of the two instants. |

</details>
