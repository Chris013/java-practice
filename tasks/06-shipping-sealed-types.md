# Task 06 · Shipping rates with sealed types and pattern matching

| Difficulty | Topics | Time box |
|---|---|---|
| 🟡 Medium | Modern Java: sealed interfaces, records, record patterns, `switch` expressions with guards, exhaustiveness · The expression problem (pattern matching vs. polymorphism) · Value objects with invariants | 2–3 h |

> **Background reading:** JEP 409 (sealed classes), JEP 440 (record patterns), JEP 441 (pattern matching for `switch`) · *Effective Java* Item 23 ("Prefer class hierarchies to tagged classes") · Brian Goetz, "Data-Oriented Programming in Java" (InfoQ article, 2022)

---

## The situation

We're a small logistics company. Customers ship **letters**, **parcels** and **pallets** from our depot in Germany. Today the price calculation is a `Shipment` class with a `String type` field, nullable fields that only make sense for some types (`palletStackable` is `null` for letters), and a 200-line method full of `if ("PARCEL".equals(type))`. *Effective Java* calls that a **tagged class** (Item 23), and it says to replace it with a class hierarchy.

Since Java 17 we have a better tool for a hierarchy that is **closed** (we know every kind of shipment, and customers can't invent new ones): a **sealed interface** with **records**, and `switch` expressions that the compiler checks for **exhaustiveness**.

I want you to model the shipments and write the rate calculation. And I want you to understand *when* this style beats classic polymorphism, because that's a question you'll be asked in design reviews.

## The domain

```mermaid
classDiagram
    class Shipment {
        <<sealed interface>>
    }
    class Letter {
        <<record>>
        int weightGrams
    }
    class Parcel {
        <<record>>
        int weightGrams
        Dimensions dimensions
        +billableWeightGrams() int
    }
    class Dimensions {
        <<record>>
        int lengthCm
        int widthCm
        int heightCm
    }
    class Pallet {
        <<record>>
        int weightKg
        boolean stackable
    }
    class Zone {
        <<enum>>
        DOMESTIC
        EU
        WORLD
    }
    Shipment <|.. Letter
    Shipment <|.. Parcel
    Shipment <|.. Pallet
    Parcel --> Dimensions
```

### The price list (domestic prices)

**Letters**, 1 to 1000 g:

| Up to | 20 g | 500 g | 1000 g |
|---|---|---|---|
| Price | 0.95 | 1.60 | 2.75 |

**Parcels** are billed by **billable weight** = the larger of the actual weight and the **volumetric weight**. Volumetric weight: carriers treat 5000 cm³ as 1 kg, so a big box of pillows is billed like a heavy box. Round volumetric weight **up** to the next full gram.

| Up to (billable) | 2 kg | 5 kg | 10 kg | 31.5 kg | more |
|---|---|---|---|---|---|
| Price | 4.99 | 6.99 | 9.99 | 16.99 | not shippable |

**Pallets**, 1 to 1000 kg: 89.00 up to 300 kg, plus 12.00 for every **started** 100 kg above 300 kg (301 kg → 101.00; 400 kg → 101.00; 401 kg → 113.00). **Non-stackable** pallets cost 25 % more. Pallets ship to `DOMESTIC` and `EU` only.

**Zones** multiply the domestic price: `DOMESTIC` × 1.0, `EU` × 1.5, `WORLD` × 2.5. Round the final price **half-up to cents**.

**Transit days** (part 2):

| | DOMESTIC | EU | WORLD |
|---|---|---|---|
| Letter | 1 | 3 | 7 |
| Parcel | 2 | 4 | 8 |
| Pallet | 3 | 5 | not shippable |

### Invalid vs. not shippable

Two different kinds of "no", and the difference matters:

```mermaid
flowchart TD
    A["new Letter(1500)"] --> B{"Is this a valid<br/>letter at all?"}
    B -- "no: a 1.5 kg letter doesn't exist" --> C["IllegalArgumentException<br/>at construction<br/>(programming/input error)"]
    D["priceFor(new Pallet(250, true), WORLD)"] --> E{"Can we ship<br/>this there?"}
    E -- "no, but it's a perfectly valid pallet" --> F["NotShippableException<br/>(business rule, show to customer)"]
```

* A record's **compact constructor** guards what makes the value *meaningful*: a letter of 0 g or 1001 g is not a letter.
* A **business rule** decides what we *offer*: a 40 kg parcel is a valid parcel, we just don't carry it. That's a `NotShippableException` from the rate calculation, not a constructor failure.

## What I want from you

### Part 1: model and price

1. Add the invariants to [`Letter`](../src/main/java/io/github/chris013/practice/task06/Letter.java), [`Parcel`](../src/main/java/io/github/chris013/practice/task06/Parcel.java), [`Dimensions`](../src/main/java/io/github/chris013/practice/task06/Dimensions.java), [`Pallet`](../src/main/java/io/github/chris013/practice/task06/Pallet.java) (see the tests for exact limits). Error messages contain the offending value.
2. Add `Parcel.billableWeightGrams()`.
3. Implement `ShippingRates.priceFor(shipment, zone)`:
   * **One `switch` expression** over the sealed `Shipment`, **no `default` branch**.
   * Use at least one **record pattern** (`case Letter(int weightGrams) -> ...`) and one **guard** (`case Pallet p when ...`).
   * Prices are `BigDecimal` with scale 2.

### Part 2: transit days

4. Implement `transitDays(shipment, zone)`. Same style. This is the "add a new operation" half of the expression problem. Notice how little ceremony it needs.

## The design discussion I want you to have with yourself

There are two classic ways to add behaviour to a closed set of types:

```mermaid
flowchart LR
    subgraph OO["Polymorphism"]
        direction TB
        L1["Letter.price(zone)"]
        P1["Parcel.price(zone)"]
        A1["Pallet.price(zone)"]
    end
    subgraph DOP["Pattern matching"]
        direction TB
        R["ShippingRates.priceFor(shipment, zone)<br/>switch over Letter / Parcel / Pallet"]
    end
```

| | Polymorphism (method per type) | Pattern matching (`switch` per operation) |
|---|---|---|
| Add a new **type** (e.g. `Envelope`) | Easy: one new class | Every `switch` must be updated, **but the compiler finds them all** (no `default`!) |
| Add a new **operation** (e.g. `transitDays`, `customsForm`, `co2Footprint`) | Touch every class | Easy: one new method with one `switch` |
| Where's the price list? | Spread over 3 classes | One class you can read top to bottom |
| Do the records depend on pricing? | Yes: data now knows business policy | No: records stay pure data |

Shipment *types* change once every few years. *Operations* on them (pricing, transit times, customs, CO₂ labels, insurance) get added all the time and are owned by different teams. That's why I'd lean towards pattern matching here, but write down your own reasoning.

**Why "no `default`" is the most important rule of this task:** with `default -> throw new IllegalStateException()`, adding `Envelope` compiles fine and blows up in production. Without `default`, adding `Envelope` to the `permits` clause turns every unhandled `switch` into a **compile error**. Exhaustiveness checking only protects you if you let it.

## Constraints & non-goals

* No `instanceof` chains and no `getClass()` comparisons.
* No pricing logic inside the records (they may have *derived properties* like billable weight; that's a fact about the parcel, not a policy of ours).
* Don't use primitive patterns in `switch` (`case int i when i <= 20`). They're still a preview feature in Java 25, and preview features don't belong in production code.
* No `double`.

## How I'd approach it

* Start with the records' compact constructors and the validation tests. Quick win.
* **Weight tiers are data.** Instead of `if (g <= 20) ... else if (g <= 500) ...`, think about a `NavigableMap<Integer, BigDecimal>` from "max weight" to price. `ceilingEntry(weight)` gives you the cheapest tier that fits, and `null` means "too heavy". The same helper then serves letters *and* parcels.
* Where does the zone factor go? If you multiply inside each `case`, you'll do it three times. Compute the domestic price in the `switch`, then apply the zone once.
* Pallets to `WORLD`: a guard `case Pallet p when zone == WORLD -> throw ...` reads exactly like the rule. Remember that cases are tested **top to bottom**, so the guarded case must come before the unguarded one. The compiler will tell you if you get it wrong ("dominated" case label).
* "Started 100 kg": `Math.ceilDiv(a, b)` exists since Java 18.
* A `switch` expression can be nested inside another one. For a 3×3 table like transit days, nested switches over the sealed type and the enum are both exhaustive, with no `default` anywhere.

## Tests

Run: `mvn test -Dtest='**/task06/**'`

[`ShippingRatesTest`](../src/test/java/io/github/chris013/practice/task06/ShippingRatesTest.java) uses parameterized tests for the tier boundaries. Look at how each boundary is tested on *both* sides (`20`/`21`, `2000`/`2001`). That's where off-by-one errors live.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task06/ShippingRatesTest.java -->
**[`ShippingRatesTest.java`](../src/test/java/io/github/chris013/practice/task06/ShippingRatesTest.java)**

```java
package io.github.chris013.practice.task06;

import static io.github.chris013.practice.task06.Zone.DOMESTIC;
import static io.github.chris013.practice.task06.Zone.EU;
import static io.github.chris013.practice.task06.Zone.WORLD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ShippingRatesTest {

    private static final Dimensions SHOE_BOX = new Dimensions(35, 20, 12);

    private final ShippingRates rates = new ShippingRates();

    @Nested
    class Letters {

        @ParameterizedTest(name = "{0} g cost {1}")
        @CsvSource({"1, 0.95", "20, 0.95", "21, 1.60", "500, 1.60", "501, 2.75", "1000, 2.75"})
        void are_priced_by_weight_tier(int grams, String expectedPrice) {
            assertThat(rates.priceFor(new Letter(grams), DOMESTIC)).isEqualByComparingTo(expectedPrice);
        }

        @Test
        void cost_more_abroad_rounded_half_up_to_cents() {
            assertThat(rates.priceFor(new Letter(20), EU)).isEqualByComparingTo("1.43"); // 0.95 x 1.5 = 1.425
            assertThat(rates.priceFor(new Letter(20), WORLD)).isEqualByComparingTo("2.38"); // 0.95 x 2.5 = 2.375
        }

        @Test
        void must_have_a_plausible_weight() {
            assertThatThrownBy(() -> new Letter(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Letter(1001))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1001");
        }
    }

    @Nested
    class Parcels {

        @Test
        void are_priced_by_actual_weight_when_they_are_compact() {
            assertThat(rates.priceFor(new Parcel(1500, SHOE_BOX), DOMESTIC)).isEqualByComparingTo("4.99");
        }

        @ParameterizedTest(name = "{0} g cost {1}")
        @CsvSource({"2000, 4.99", "2001, 6.99", "5000, 6.99", "5001, 9.99", "10000, 9.99", "10001, 16.99", "31500, 16.99"})
        void are_priced_by_weight_tier(int grams, String expectedPrice) {
            assertThat(rates.priceFor(new Parcel(grams, SHOE_BOX), DOMESTIC)).isEqualByComparingTo(expectedPrice);
        }

        @Test
        void are_priced_by_volumetric_weight_when_they_are_light_but_bulky() {
            // 60 x 40 x 40 cm = 96,000 cm³ -> 19.2 kg volumetric weight
            Parcel pillows = new Parcel(1000, new Dimensions(60, 40, 40));

            assertThat(pillows.billableWeightGrams()).isEqualTo(19_200);
            assertThat(rates.priceFor(pillows, DOMESTIC)).isEqualByComparingTo("16.99");
        }

        @Test
        void round_volumetric_weight_up_to_the_next_gram() {
            assertThat(new Parcel(1, new Dimensions(1, 1, 7)).billableWeightGrams()).isEqualTo(2); // 7 cm³ / 5 = 1.4 g
        }

        @Test
        void cost_more_abroad() {
            assertThat(rates.priceFor(new Parcel(1500, SHOE_BOX), EU)).isEqualByComparingTo("7.49"); // 4.99 x 1.5
            assertThat(rates.priceFor(new Parcel(1500, SHOE_BOX), WORLD)).isEqualByComparingTo("12.48"); // 4.99 x 2.5
        }

        @Test
        void cannot_be_shipped_above_31_5_kg_billable_weight() {
            assertThatThrownBy(() -> rates.priceFor(new Parcel(31_501, SHOE_BOX), DOMESTIC))
                    .isInstanceOf(NotShippableException.class);
            assertThatThrownBy(() -> rates.priceFor(new Parcel(5000, new Dimensions(80, 60, 40)), DOMESTIC))
                    .isInstanceOf(NotShippableException.class);
        }

        @Test
        void must_have_a_positive_weight_and_dimensions() {
            assertThatThrownBy(() -> new Parcel(0, SHOE_BOX)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Parcel(100, null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Dimensions(10, 0, 10)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Pallets {

        @Test
        void cost_a_flat_rate_up_to_300_kg() {
            assertThat(rates.priceFor(new Pallet(250, true), DOMESTIC)).isEqualByComparingTo("89.00");
            assertThat(rates.priceFor(new Pallet(300, true), DOMESTIC)).isEqualByComparingTo("89.00");
        }

        @ParameterizedTest(name = "{0} kg cost {1}")
        @CsvSource({"301, 101.00", "400, 101.00", "401, 113.00", "1000, 173.00"})
        void cost_extra_per_started_100_kg_above_300_kg(int kilograms, String expectedPrice) {
            assertThat(rates.priceFor(new Pallet(kilograms, true), DOMESTIC)).isEqualByComparingTo(expectedPrice);
        }

        @Test
        void cost_25_percent_more_when_not_stackable() {
            assertThat(rates.priceFor(new Pallet(250, false), DOMESTIC)).isEqualByComparingTo("111.25");
        }

        @Test
        void can_be_shipped_within_the_eu() {
            assertThat(rates.priceFor(new Pallet(250, true), EU)).isEqualByComparingTo("133.50");
        }

        @Test
        void cannot_be_shipped_outside_the_eu() {
            assertThatThrownBy(() -> rates.priceFor(new Pallet(250, true), WORLD))
                    .isInstanceOf(NotShippableException.class);
        }

        @Test
        void must_weigh_between_1_and_1000_kg() {
            assertThatThrownBy(() -> new Pallet(0, true)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new Pallet(1001, true)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class TransitDays {

        @ParameterizedTest(name = "{0}")
        @CsvSource({"DOMESTIC, 1", "EU, 3", "WORLD, 7"})
        void for_letters(Zone zone, int days) {
            assertThat(rates.transitDays(new Letter(20), zone)).isEqualTo(days);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"DOMESTIC, 2", "EU, 4", "WORLD, 8"})
        void for_parcels(Zone zone, int days) {
            assertThat(rates.transitDays(new Parcel(1000, SHOE_BOX), zone)).isEqualTo(days);
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource({"DOMESTIC, 3", "EU, 5"})
        void for_pallets(Zone zone, int days) {
            assertThat(rates.transitDays(new Pallet(250, true), zone)).isEqualTo(days);
        }

        @Test
        void are_not_promised_for_shipments_we_cannot_ship() {
            assertThatThrownBy(() -> rates.transitDays(new Pallet(250, true), WORLD))
                    .isInstanceOf(NotShippableException.class);
        }
    }

    @Test
    void prices_are_always_in_cents() {
        BigDecimal price = rates.priceFor(new Letter(20), EU);

        assertThat(price.scale()).isEqualTo(2);
    }
}
```
<!-- /embed -->

</details>

## Definition of done

- [ ] All tests green.
- [ ] No `default` branch in any `switch` over `Shipment` or `Zone`.
- [ ] At least one record pattern and one guarded pattern.
- [ ] Each price tier table exists once and reads like the price list above.
- [ ] Zone factor and rounding are applied in exactly one place.
- [ ] Invalid values fail at construction; un-shippable but valid shipments fail with `NotShippableException`.

## Stretch goals

1. Add a fourth shipment type `Envelope(boolean registered)`: flat 3.50, registered +2.50, transit like letters. Add it to `permits` **first** and let the compiler tell you what to do. Count the errors.
2. Replace `NotShippableException` with a sealed result type `Quote` = `Priced(BigDecimal)` | `NotShippable(String reason)`. How do callers change? Which version would you rather use in a REST controller? (Task 15 goes deep on this.)
3. Implement the same `priceFor` with classic polymorphism (an abstract `price(Zone)` method on each record) in a scratch branch. Then add `co2GramsFor(shipment, zone)`. Compare the diffs.

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. Records validate meaning, `ShippingRates` decides policy.** The 1–1000 g letter range and the 1–1000 kg pallet range are part of *what a letter/pallet is*, so they live in the compact constructors (with the limit as a named constant). The 31.5 kg parcel limit is part of *what we offer*, so it lives in the price table and produces `NotShippableException`.

**2. Price tiers as `NavigableMap`.** `LETTER_PRICES` and `PARCEL_PRICES` look like the price list, and a single `tierPrice(...)` helper with `ceilingEntry` handles both. "Too heavy" falls out naturally as "no tier found". The maps are unmodifiable, because a `static final` mutable map is a global variable.

**3. One `switch`, then one zone multiplication.** The `switch` computes the *domestic* price; zone factor and rounding happen once, afterwards. The zone factor lives in the `Zone` enum, the only place that knows what a zone means for prices.

**4. Record patterns where they help.** `case Letter(int weightGrams)` and `case Pallet(int weightKg, boolean stackable)` deconstruct the data the price needs. For `Parcel` I bind the whole record (`case Parcel parcel`), because the method needed is `billableWeightGrams()`, not a component. Use deconstruction when it makes the line clearer, not everywhere.

**5. Guard ordering.** `case Pallet pallet when zone == WORLD` comes before `case Pallet(...)`. Reversed, the compiler rejects the guarded case as dominated.

**6. Nested exhaustive switches for the transit table.** It's a 3×3 table, and nested `switch` expressions show it as a table. A new shipment type *or* a new zone is a compile error in both methods.

**7. Integer grams, `Math.ceilDiv`.** Volumetric weight is `ceilDiv(volume, 5)`: no `BigDecimal`, no `double`, and the rounding rule ("up to the next gram") is visible in the method name.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task06/ShippingRates.java -->
**[`ShippingRates.java`](../solutions/src/main/java/io/github/chris013/practice/task06/ShippingRates.java)**

```java
package io.github.chris013.practice.task06;

import static io.github.chris013.practice.task06.Zone.WORLD;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Our price list and delivery promises.
 *
 * <p>Every operation is one exhaustive {@code switch} over the sealed {@link Shipment} hierarchy, with no
 * {@code default} branch. Adding a new kind of shipment therefore fails to compile until every operation handles it.
 */
public final class ShippingRates {

    /** Price of the cheapest tier whose maximum weight (in grams, inclusive) is at least the shipment's weight. */
    private static final NavigableMap<Integer, BigDecimal> LETTER_PRICES = tiers(Map.of(
            20, "0.95",
            500, "1.60",
            1000, "2.75"));

    private static final NavigableMap<Integer, BigDecimal> PARCEL_PRICES = tiers(Map.of(
            2_000, "4.99",
            5_000, "6.99",
            10_000, "9.99",
            31_500, "16.99"));

    private static final BigDecimal PALLET_BASE_PRICE = new BigDecimal("89.00");
    private static final int PALLET_KG_INCLUDED = 300;
    private static final int PALLET_SURCHARGE_STEP_KG = 100;
    private static final BigDecimal PALLET_SURCHARGE_PER_STEP = new BigDecimal("12.00");
    private static final BigDecimal NON_STACKABLE_FACTOR = new BigDecimal("1.25");

    public BigDecimal priceFor(Shipment shipment, Zone zone) {
        BigDecimal domesticPrice = switch (shipment) {
            case Letter(int weightGrams) -> tierPrice(LETTER_PRICES, weightGrams, shipment);
            case Parcel parcel -> tierPrice(PARCEL_PRICES, parcel.billableWeightGrams(), shipment);
            case Pallet pallet when zone == WORLD -> throw palletsStayInEurope(pallet);
            case Pallet(int weightKg, boolean stackable) -> palletPrice(weightKg, stackable);
        };
        return domesticPrice.multiply(zone.priceFactor()).setScale(2, RoundingMode.HALF_UP);
    }

    public int transitDays(Shipment shipment, Zone zone) {
        return switch (shipment) {
            case Letter letter -> switch (zone) {
                case DOMESTIC -> 1;
                case EU -> 3;
                case WORLD -> 7;
            };
            case Parcel parcel -> switch (zone) {
                case DOMESTIC -> 2;
                case EU -> 4;
                case WORLD -> 8;
            };
            case Pallet pallet -> switch (zone) {
                case DOMESTIC -> 3;
                case EU -> 5;
                case WORLD -> throw palletsStayInEurope(pallet);
            };
        };
    }

    private static BigDecimal tierPrice(NavigableMap<Integer, BigDecimal> tiers, int weightGrams, Shipment shipment) {
        Map.Entry<Integer, BigDecimal> tier = tiers.ceilingEntry(weightGrams);
        if (tier == null) {
            throw new NotShippableException("%s is too heavy: %d g billable, at most %d g allowed"
                    .formatted(shipment, weightGrams, tiers.lastKey()));
        }
        return tier.getValue();
    }

    private static BigDecimal palletPrice(int weightKg, boolean stackable) {
        int startedSteps = Math.ceilDiv(Math.max(0, weightKg - PALLET_KG_INCLUDED), PALLET_SURCHARGE_STEP_KG);
        BigDecimal price = PALLET_BASE_PRICE.add(PALLET_SURCHARGE_PER_STEP.multiply(BigDecimal.valueOf(startedSteps)));
        return stackable ? price : price.multiply(NON_STACKABLE_FACTOR);
    }

    private static NotShippableException palletsStayInEurope(Pallet pallet) {
        return new NotShippableException("We ship pallets within the EU only: " + pallet);
    }

    private static NavigableMap<Integer, BigDecimal> tiers(Map<Integer, String> pricesByMaxWeight) {
        NavigableMap<Integer, BigDecimal> tiers = new TreeMap<>();
        pricesByMaxWeight.forEach((maxWeight, price) -> tiers.put(maxWeight, new BigDecimal(price)));
        return Collections.unmodifiableNavigableMap(tiers);
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task06/Parcel.java -->
**[`Parcel.java`](../solutions/src/main/java/io/github/chris013/practice/task06/Parcel.java)**

```java
package io.github.chris013.practice.task06;

import java.util.Objects;

public record Parcel(int weightGrams, Dimensions dimensions) implements Shipment {

    public Parcel {
        if (weightGrams < 1) {
            throw new IllegalArgumentException("A parcel must weigh at least 1 g, not %d g".formatted(weightGrams));
        }
        Objects.requireNonNull(dimensions, "dimensions");
    }

    /** What we charge for: the actual weight, or the volumetric weight if the parcel is light but bulky. */
    public int billableWeightGrams() {
        return Math.max(weightGrams, dimensions.volumetricWeightGrams());
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task06/Dimensions.java -->
**[`Dimensions.java`](../solutions/src/main/java/io/github/chris013/practice/task06/Dimensions.java)**

```java
package io.github.chris013.practice.task06;

/** Outer dimensions of a parcel in centimetres. */
public record Dimensions(int lengthCm, int widthCm, int heightCm) {

    /** Carriers bill bulky parcels as if 5000 cm³ weighed 1 kg, i.e. 5 cm³ per gram. */
    private static final int CUBIC_CM_PER_VOLUMETRIC_GRAM = 5;

    public Dimensions {
        if (lengthCm < 1 || widthCm < 1 || heightCm < 1) {
            throw new IllegalArgumentException(
                    "Dimensions must be positive, got %d x %d x %d cm".formatted(lengthCm, widthCm, heightCm));
        }
    }

    int volumetricWeightGrams() {
        return Math.ceilDiv(lengthCm * widthCm * heightCm, CUBIC_CM_PER_VOLUMETRIC_GRAM);
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task06/Letter.java -->
**[`Letter.java`](../solutions/src/main/java/io/github/chris013/practice/task06/Letter.java)**

```java
package io.github.chris013.practice.task06;

/** A letter or large letter, up to 1000 g. */
public record Letter(int weightGrams) implements Shipment {

    static final int MAX_WEIGHT_GRAMS = 1000;

    public Letter {
        if (weightGrams < 1 || weightGrams > MAX_WEIGHT_GRAMS) {
            throw new IllegalArgumentException(
                    "A letter weighs 1 to %d g, not %d g. Heavier items are parcels.".formatted(MAX_WEIGHT_GRAMS, weightGrams));
        }
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task06/Pallet.java -->
**[`Pallet.java`](../solutions/src/main/java/io/github/chris013/practice/task06/Pallet.java)**

```java
package io.github.chris013.practice.task06;

/** A standard euro pallet, up to 1000 kg. Non-stackable pallets take up the space above them as well. */
public record Pallet(int weightKg, boolean stackable) implements Shipment {

    static final int MAX_WEIGHT_KG = 1000;

    public Pallet {
        if (weightKg < 1 || weightKg > MAX_WEIGHT_KG) {
            throw new IllegalArgumentException("A pallet weighs 1 to %d kg, not %d kg".formatted(MAX_WEIGHT_KG, weightKg));
        }
    }
}
```
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task06/Zone.java -->
**[`Zone.java`](../solutions/src/main/java/io/github/chris013/practice/task06/Zone.java)**

```java
package io.github.chris013.practice.task06;

import java.math.BigDecimal;

/** Where a shipment goes, seen from our depot in Germany. */
public enum Zone {
    DOMESTIC("1.0"),
    EU("1.5"),
    WORLD("2.5");

    private final BigDecimal priceFactor;

    Zone(String priceFactor) {
        this.priceFactor = new BigDecimal(priceFactor);
    }

    BigDecimal priceFactor() {
        return priceFactor;
    }
}
```
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| `default -> throw new IllegalStateException("unknown shipment")` | Turns a compile-time guarantee into a runtime crash. |
| `if (shipment instanceof Letter l) ... else if ...` | Not exhaustive: nothing tells you when a type is missing. |
| Zone multiplication in each `case` | Three copies of the same rule, and three places to forget the rounding. |
| `weightGrams / 5000.0` | `double` sneaks in through the volumetric weight. |
| `(weightKg - 300) / 100 + 1` for "started 100 kg" | Wrong for exactly 400 kg (gives 2 steps instead of 1). `ceilDiv` says what it means. |
| `new Parcel(40_000, box)` throws in the constructor | A 40 kg parcel exists; we just don't ship it. Mixing "invalid" with "not offered" means the UI can't tell the customer *why*. |

</details>
