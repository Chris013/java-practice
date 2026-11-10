# Task 28 · Write your own Collectors

| Difficulty | Topics | Time box |
|---|---|---|
| 🟡 Medium | The `Collector` interface (supplier, accumulator, combiner, finisher, characteristics) · Mutable reduction vs. `reduce` · Parallel streams and why the combiner must be correct · Numerically stable statistics (Welford, Chan) · Generic APIs with PECS · Composing with `groupingBy` and `collectingAndThen` | 2–3 h |

> **Background reading:** *Modern Java in Action* ch. 6 (especially 6.5 "The Collector interface" and 6.6 "Developing your own collector") and ch. 7 (parallel streams) · Javadoc of `java.util.stream.Collector` (the identity and associativity constraints) · *Effective Java* Item 46 ("Prefer side-effect-free functions in streams") and Item 48 ("Use caution when making streams parallel") · B. P. Welford (1962) and Chan, Golub & LeVeque (1979) on computing variance; the Wikipedia article "Algorithms for calculating variance" summarises both.

---

## The situation

Our logistics company runs cold-storage warehouses. Thousands of temperature sensors send readings, and the monitoring service computes statistics over millions of readings per hour. Three things went wrong in the last month:

* The code calls `stream().mapToDouble(...).average()` five times per sensor, so every sensor's readings are walked five times.
* The standard deviation uses the textbook formula `sqrt(E[x²] - E[x]²)`. For one sensor it returned `NaN`. The formula subtracts two large, nearly equal numbers, and the rounding error can be bigger than the answer.
* Someone added `.parallel()` to a custom reduction that adds readings to a shared `ArrayList`. The results change from run to run, because threads write to the same list at the same time.

The fix is to write proper collectors. A collector is the reusable piece that says how to build a result from a stream of values, and it can be run in parallel if you give it the right parts. You will write four of them.

## The domain

| Term | Meaning |
|---|---|
| **Reading** | One temperature sample: sensor id, instant, degrees Celsius. The celsius value is finite. |
| **Statistics** | Count, min, max, mean and population standard deviation of a non-empty set of readings. |
| **Population standard deviation** | Divide the sum of squared deviations by `n`, not by `n - 1`. |
| **Partial result** | The mutable state of a collector after it has seen some of the readings. Each thread has its own. |
| **Combiner** | The operation that merges two partial results into one. Parallel streams use it. |

### How a parallel collect works

```mermaid
flowchart TD
    Source[Readings in a parallel stream] --> Split[Split into chunks]
    Split --> C1[Chunk 1: supplier then accumulator]
    Split --> C2[Chunk 2: supplier then accumulator]
    Split --> C3[Chunk 3: supplier then accumulator]
    Split --> C4[Chunk 4: supplier then accumulator]
    C1 --> M1[Combiner: chunk 1 with chunk 2]
    C2 --> M1
    C3 --> M2[Combiner: chunk 3 with chunk 4]
    C4 --> M2
    M1 --> M3[Combiner: final merge]
    M2 --> M3
    M3 --> F[Finisher turns the partial result into R]
```

Each chunk gets its **own** container from the supplier, so the accumulators never touch each other. The combiner tree then merges the containers in an order the library chooses, not you. That is why the combiner must give the same answer whatever the grouping is.

### The type parameters of `Collector<T, A, R>`

```mermaid
classDiagram
    class Collector~T, A, R~ {
        <<interface>>
        +supplier() Supplier~A~
        +accumulator() BiConsumer~A, T~
        +combiner() BinaryOperator~A~
        +finisher() Function~A, R~
        +characteristics() Set~Characteristics~
    }
    class Reading {
        sensorId
        at
        celsius
    }
    class RunningStatistics {
        count
        mean
        m2
        min
        max
    }
    class Statistics {
        count
        min
        max
        mean
        standardDeviation
    }
    Collector ..> Reading : T is the input element
    Collector ..> RunningStatistics : A is the mutable container
    Collector ..> Statistics : R is the result
```

* **T** is what goes in: a `Reading`.
* **A** is the mutable container that exists while the collect runs. It is never shown to the caller.
* **R** is what comes out. `Optional<Statistics>` for our statistics collector.

## What I want from you

Implement the four public methods of [`SensorCollectors`](../src/main/java/io/github/chris013/practice/task28/SensorCollectors.java). The records [`Reading`](../src/main/java/io/github/chris013/practice/task28/Reading.java) and [`Statistics`](../src/main/java/io/github/chris013/practice/task28/Statistics.java) are given. Do not change them.

1. **`statistics()`** returns a `Collector<Reading, ?, Optional<Statistics>>`.
   * One pass over the readings. The accumulator uses Welford's online update, and the combiner uses Chan et al.'s pairwise merge. Both are written out in the "How I'd approach it" section.
   * Empty input gives `Optional.empty()`. A single reading gives a standard deviation of `0`.
   * `min` and `max` are exact. The mean and the standard deviation must match a sequential run within a relative tolerance of `1e-9`.
   * The combiner must give the same result as one pass over both halves. It must also work when one half is empty.
   * The characteristics are `UNORDERED`, and no more. Explain in the Javadoc why `IDENTITY_FINISH` and `CONCURRENT` are not set.
2. **`topK(int k, Comparator<? super T> order)`** returns a `Collector<T, ?, List<T>>` with the `k` greatest elements, greatest first.
   * The result is unmodifiable.
   * `k` must be at least 1. Otherwise throw `IllegalArgumentException`.
   * If the input has fewer than `k` elements, return all of them.
   * Keep at most `k` elements in memory at any time, using a bounded `PriorityQueue`. Do not sort the whole input.
   * The combiner merges two partial results and keeps `k` elements.
   * With no ties, parallel and sequential runs give the same list. Ties at the cut-off may go either way, and the Javadoc says so.
   * The parameter is `Comparator<? super T>`, so a `Comparator<Object>` can rank a stream of Strings.
3. **`statisticsPerSensor()`** returns a `Collector<Reading, ?, SortedMap<String, Statistics>>`.
   * Build it from `groupingBy` (with `TreeMap::new`), `statistics()`, and `collectingAndThen`. Do not write a supplier or accumulator here.
   * Each group has at least one reading, so the `Optional` from `statistics()` is never empty for a group. Unwrap it.
   * The returned map is sorted by sensor id and unmodifiable.
   * Empty input gives an empty map.
4. **`countAbove(double thresholdCelsius)`** returns a `Collector<Reading, ?, Map<Boolean, Long>>`.
   * It counts the readings strictly above the threshold. A reading equal to the threshold is not above it.
   * Both keys, `true` and `false`, are always present, even when one partition is empty.
   * A threshold that is NaN or infinite throws `IllegalArgumentException`.
   * Use `partitioningBy` and `counting()`.

Every collector gives the same result sequentially and in parallel.

## Constraints & non-goals

* **No shared mutable state outside the accumulation containers.** Do not capture a list, a map or a counter in a lambda and write to it from the accumulator. That is the bug in the story above.
* **No `reduce` with a mutable identity.** `reduce` gives the same identity object to every chunk. Use a collector, which creates a new container per chunk.
* **No sorting for `topK`.** The point of the task is memory and time proportional to `k`, not `n`.
* **No `IDENTITY_FINISH` and no `CONCURRENT`** on the statistics collector. Explain why in the Javadoc, as the task asks.
* **No dependencies.** Plain Java 25, no libraries, no streaming frameworks.
* **No LaTeX in the Javadoc.** Plain text or `{@code}` only.

## How I'd approach it

* **Start with `topK`.** It is the smallest, and it shows the shape of every collector: a supplier, an accumulator, a combiner, and a finisher. Write the sequential version first, then add the combiner, then run the parallel test.
* **Why `reduce` with a mutable container is wrong.** `stream.reduce(new ArrayList<>(), (list, x) -> { list.add(x); return list; }, combiner)` shares one `ArrayList` between every chunk, so the parallel run races on it. Even with a fresh container per chunk, `reduce` passes the same identity object to every chunk. With a mutable container, every chunk's accumulator mutates that one shared object. `reduce` is designed for immutable values, where the accumulator returns a new value. `collect` with a `Collector` is the mutable-reduction counterpart: its supplier creates a fresh container for every chunk.
* **The two constraints from the `Collector` Javadoc.** The supplier's container must be an identity for the combiner: merging an empty container into any partial result leaves that result unchanged. And the combiner must be associative: `combine(combine(a, b), c)` must equal `combine(a, combine(b, c))`. Parallel streams choose the tree of merges themselves, so you must not depend on it.
* **Characteristics.** `IDENTITY_FINISH` means the finisher is a plain cast, so `A` and `R` are the same type. Use it only when that is true. `UNORDERED` means the result does not depend on the encounter order. `CONCURRENT` means one shared container is updated by several threads, so the accumulator must be thread-safe. It is rarely what you want: a thread-safe container is harder to get right and usually slower than one container per chunk. The usual exception is grouping into a `ConcurrentHashMap`.
* **When parallel streams help.** They help when the source splits cheaply (an `ArrayList`, an array, `IntStream.range`), the work per element is CPU-bound and not trivial, and the combine step is cheap. They hurt on small inputs, on sources that split badly (`LinkedList`, `Stream.iterate`, a stream of lines from a socket), and in a server, where the shared common pool is used by other requests too. *Effective Java* Item 48 gives the same advice.
* **Welford's online update.** For each new value `x`, with `n` the count after adding it:

```text
delta = x - mean
mean  = mean + delta / n
m2    = m2 + delta * (x - mean)
```

  The population variance is `m2 / n`. The key is the third line. It multiplies `delta`, the distance from the old mean, by `x - mean`, the distance from the new mean. Both are small numbers, so the update never forms `x*x` and never subtracts two large, nearly equal sums.

* **Chan's merge.** Two partial results A and B, with `n = nA + nB`:

```text
delta = meanB - meanA
mean  = meanA + delta * nB / n
m2    = m2A + m2B + delta * delta * nA * nB / n
```

  Guard the empty cases explicitly. If both parts are empty, `n` is 0 and `nB / n` is `0 / 0`, which is NaN. If only one part is empty, the formulas happen to give the other part, but an early return is clearer and does not depend on that.
* **Min and max** are easy to forget in the merge. They are not in the formulas above, and a merge that only updates `count`, `mean` and `m2` gives wrong answers silently.
* **Worked example: why the textbook formula fails.** Take the four values `1e9 + 4`, `1e9 + 7`, `1e9 + 13` and `1e9 + 16`. Their true mean is `1e9 + 10`, and their true variance is `22.5`. A double has about 16 significant digits, so the squares are about `1e18` and each one is rounded to a multiple of 128.

  | Quantity | Computed in doubles | Exact |
  |---|---|---|
  | sum of squares / n (E[x²]) | `1.00000002E18` | 1000000020000000122.5 |
  | mean² (E[x]²) | `1.0000000200000001E18` | 1000000020000000100 |
  | difference | `-128.0` | `22.5` |

  The difference should be `22.5`, but rounding leaves `-128.0`. The square root of a negative number is `NaN`, which is the bug from the story. Welford's method on the same four values gives a mean of `1e9 + 10` and a standard deviation of `4.7434`, because it never forms the large squares.
* **PECS in `topK`.** The comparator *consumes* the elements; it does not produce them. So the parameter is `Comparator<? super T>`: a `Comparator<Object>` is a valid consumer of Strings. *Effective Java* Item 31 ("Use bounded wildcards to increase API flexibility") gives the rule. Try the method with `Comparator<String>` and with `Comparator<Object>`, and see what the compiler accepts.
* **The min-heap for `topK`.** A `PriorityQueue` with the comparator, used as a min-heap, has the least element at its head. You keep the `k` greatest elements by asking one question for each new element: is it greater than the head? If yes, replace the head. Think about why this is the cheapest check you can make.
* **Composition.** `statisticsPerSensor()` should read like a sentence. `groupingBy(sensorId, TreeMap::new, oneSensor)` where `oneSensor` is `statistics()` with the `Optional` unwrapped. Then `collectingAndThen` makes the map unmodifiable. If you find yourself writing a loop, you are re-implementing `groupingBy`.
* **Counting with two keys.** `groupingBy` only creates keys that appear in the input. `partitioningBy` always creates both `true` and `false`, which is what the dashboard needs.
* **Test with real parallelism.** `parallelStream()` on an `ArrayList` of 200,000 items splits into several chunks on a multi-core machine. A test that passes sequentially and fails in parallel is what you are looking for.

## Tests

Run: `mvn test -Dtest='**/task28/**'`

* [`StatisticsCollectorTest`](../src/test/java/io/github/chris013/practice/task28/StatisticsCollectorTest.java) covers the statistics, the numerical stability on the 1e9 data, the combiner called directly, and parallel against sequential.
* [`TopKCollectorTest`](../src/test/java/io/github/chris013/practice/task28/TopKCollectorTest.java) covers `k` larger than the input, `k` of one, Strings and Readings, the unmodifiable result, `k` of zero, PECS, and the parallel run.
* [`ComposedCollectorsTest`](../src/test/java/io/github/chris013/practice/task28/ComposedCollectorsTest.java) covers the per-sensor map and `countAbove`, including the case where one partition is empty.
* [`TestReadings`](../src/test/java/io/github/chris013/practice/task28/TestReadings.java) holds the deterministic random data and the helper that compares statistics with a tolerance.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task28/StatisticsCollectorTest.java -->
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task28/TopKCollectorTest.java -->
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task28/ComposedCollectorsTest.java -->
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task28/TestReadings.java -->
<!-- /embed -->

</details>

## Definition of done

- [ ] All tests green, and the whole task runs in well under 5 seconds.
- [ ] `statistics()` uses Welford in the accumulator and Chan's merge in the combiner. Empty input and single values are handled.
- [ ] The combiner handles an empty half on either side, and it also merges `min` and `max`.
- [ ] `topK` keeps at most `k` elements at any time, and the result is unmodifiable and greatest first.
- [ ] `statisticsPerSensor()` is built from `groupingBy`, `statistics()` and `collectingAndThen`, with no hand-written supplier or accumulator.
- [ ] `countAbove` always has both keys, and a NaN threshold is rejected.
- [ ] No shared mutable state outside the accumulation containers.
- [ ] The Javadoc of `statistics()` says why `IDENTITY_FINISH` and `CONCURRENT` are not set, and whether `UNORDERED` is safe.

## Stretch goals

1. Write `histogram(double bucketWidth)` as a collector that counts readings per bucket. Which key type do you use, and how do you keep the map sorted? Make sure the combiner adds counts without losing a bucket.
2. Percentiles cannot be merged exactly from partial results: the median of two medians is not the median. Explain why, and read about the t-digest idea (Dunning and Ertl, "Computing Extremely Accurate Quantiles Using t-Digests"). What would you give up?
3. JEP 485 ("Stream Gatherers", final in JDK 24) adds `Stream.gather`, which is a stateful intermediate step. Write a moving average over the last `n` readings with a gatherer. Then explain why a moving average is a poor fit for a `Collector`, which only produces one result at the end.
4. Benchmark `statistics()` against five `averagingDouble` calls with JMH on a realistic input size. Report the numbers and what they do not tell you.

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. The statistics container is a private mutable class, not a `double[]` or a `List`.** `RunningStatistics` holds `count`, `mean`, `m2`, `min` and `max`. The container is the state of the collector, and the finisher turns it into an `Optional<Statistics>`. Because the finisher changes the type, `IDENTITY_FINISH` is not allowed.

**2. Welford in the accumulator, Chan in the combiner.** Both are numerically stable. The combiner checks for an empty side first, because an empty side has no mean. The merge uses the sizes of both sides, so the result does not depend on how the parallel stream split the data.

**3. `UNORDERED` and nothing else.** The order of readings does not change the mean or the variance mathematically. It can change the last few bits of the floating-point result, which is why the tests compare with a relative tolerance. `CONCURRENT` is not set, because each chunk has its own container; the alternative would be a shared, locked container, which is slower and harder to reason about.

**4. `topK` uses a bounded min-heap, not a sort.** The heap holds at most `k` elements. Each new element is compared with the head, and it replaces the head only if it is greater. The combiner offers the elements of the other heap to this one, so it still holds at most `k`. Sorting everything would cost O(n log n) time and O(n) memory.

**5. PECS on the comparator.** The parameter is `Comparator<? super T>`, because the comparator only reads the elements. The result is sorted once at the end with `order.reversed()`, and `List.copyOf` makes it unmodifiable.

**6. `statisticsPerSensor()` is composition, not a loop.** `collectingAndThen(statistics(), Optional::orElseThrow)` gives a collector for one sensor. `groupingBy` with `TreeMap::new` puts one of those in each group, and `collectingAndThen` makes the sorted map unmodifiable. The `orElseThrow` is safe because a group only exists when it has at least one reading.

**7. `countAbove` uses `partitioningBy`.** It always creates both keys, which `groupingBy` would not do for an empty partition. A NaN threshold is rejected up front, because every comparison with NaN is false, and the counts would be wrong without any error.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task28/SensorCollectors.java -->
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| Textbook variance `sqrt(E[x²] - E[x]²)` | Subtracts two nearly equal large numbers. The 1e9 example gives a variance of -128 and a NaN. Use Welford. |
| A combiner that ignores the right side, or throws on an empty side | Parallel runs silently lose data, or fail only when the stream splits in a particular way. Test the combiner directly. |
| A shared `ArrayList` filled with `forEach` in a parallel stream | Threads write to one list at the same time. Results change from run to run. Use a collector with one container per chunk. |
| `Collectors.reducing` or `reduce` with a mutable identity | The same object is shared between chunks, and the combiner may mutate it. Use `Collector.of` with a supplier. |
| `topK` by sorting all the readings | O(n log n) time and O(n) memory, when a heap of size `k` needs O(n log k) time and O(k) memory. |
| Returning the internal mutable list from the finisher | The caller can change the result, and the result changes if the collector is reused. Return `List.copyOf`. |
| Forgetting `min` and `max` in the merge | Merged results look right in the mean, but the extremes are wrong. Test the extremes after a parallel run. |
| Calling `averagingDouble` five times per sensor | Walks the readings five times, and gives no standard deviation. One collector gives everything in one pass. |

</details>
