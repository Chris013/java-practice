# Task 13 · A supermarket receipt with smart enums

| Difficulty | Topics | Time box |
|---|---|---|
| 🟢 Easy | Enums with fields and behaviour · EnumMap and EnumSet · Never use `ordinal()` · Grouping with streams into an EnumMap · Rounding money per group, not per line | 1–2 h |

> **Background reading:** *Effective Java* Items 34 ("Use enums instead of int constants"), 35 ("Use instance fields instead of ordinals"), 36 ("Use `EnumSet` instead of bit fields"), 37 ("Use `EnumMap` instead of ordinal indexing") · *Modern Java in Action* ch. 6 (collecting data with streams: grouping, and the `groupingBy` overload that takes a map factory)

---

## The situation

Our till software runs in a German supermarket chain. Two rules shape the receipt.

**First, shelf prices are gross.** The price on the shelf already includes VAT. The till must split it back out, because German law requires the receipt to show a VAT breakdown per rate: the net amount, the VAT and the gross amount for the 19 % rate and for the 7 % rate. The printed receipt labels the rates with the letters **A** (19 %) and **B** (7 %).

**Second, some products need an ID check.** Beer and wine need the cashier to check that the customer is 16 or older. Spirits and tobacco need 18. If the basket contains both, the cashier must check for 18.

Today, the categories and the VAT rates are `public static final int` constants, and a `switch` over them is written in six places. When the VAT on restaurant food changed, someone updated five of the six switches. Nothing failed at compile time, and the receipt printed the wrong VAT until someone found it.

That is the problem this task fixes. The category and the VAT rate are **one concept**, so they belong in one place: an enum that carries its own data. Then the compiler and the code structure make it hard to forget a place. The rest of the code asks the category what its rate is, and it never has to `switch` on the category.

## The domain

| Term | Meaning |
|---|---|
| **Gross price** | The shelf price. It includes VAT. This is what the customer pays. |
| **Net price** | The gross price without VAT. |
| **VAT rate** | Standard (19 %, code `A`) or reduced (7 %, code `B`). |
| **Category** | What a product is for the till: bakery, beer and wine, and so on. A category fixes the VAT rate and the minimum age. |
| **Minimum age** | The age the cashier must verify before selling. `0` means no check. |
| **Rate group** | All line items that have the same VAT rate. The VAT is computed per group, not per line. |

Categories and their rules (this table is the contract, and the tests check it):

| Category | VAT rate | Minimum age |
|---|---|---|
| `FRUIT_AND_VEGETABLES`, `BAKERY`, `DAIRY`, `BOOKS` | REDUCED (B, 7 %) | none |
| `SOFT_DRINKS`, `HOUSEHOLD` | STANDARD (A, 19 %) | none |
| `BEER_AND_WINE` | STANDARD (A, 19 %) | 16 |
| `SPIRITS`, `TOBACCO` | STANDARD (A, 19 %) | 18 |

### The class structure

```mermaid
classDiagram
    class Receipt {
        +List~LineItem~ items()
        +BigDecimal totalGross()
        +Map~VatRate, VatLine~ vatBreakdown()
        +OptionalInt minimumAgeToVerify()
        +Set~Category~ categories()
    }
    class LineItem {
        +String name()
        +Category category()
        +BigDecimal unitPriceGross()
        +int quantity()
        +BigDecimal totalGross()
    }
    class Category {
        +VatRate vatRate()
        +int minimumAge()
        +boolean isAgeRestricted()
    }
    class VatRate {
        +char code()
        +int percent()
        +fromCode(char) VatRate
    }
    class VatLine {
        +VatRate rate()
        +BigDecimal gross()
        +BigDecimal net()
        +BigDecimal vat()
    }
    Receipt *-- LineItem : contains
    LineItem --> Category : sold as
    Category --> VatRate : taxed at
    Receipt ..> VatLine : produces
```

Read the arrows as "knows about". The receipt knows its items. An item knows its category. A category knows its VAT rate. Nothing points back up, and nothing needs a `switch` to walk down this chain.

### What the printed receipt looks like

This is the output we want for a small basket. The VAT table at the bottom is the breakdown from the `Receipt` class.

```
SUPERMARKET 0412                       2026-11-09 14:32
------------------------------------------------------
Bananas 1 kg                                    1.99 B
Milk 1 l, 2 x 1.09                              2.18 B
Beer 0.5 l, 6 x 0.89                            5.34 A
Dish soap                                       1.79 A
------------------------------------------------------
SUM                                            11.30 EUR
AGE CHECK                                       16+

RATE   VAT     NET       VAT AMOUNT     GROSS
A      19 %    5.99      1.14           7.13
B       7 %    3.90      0.27           4.17
```

Notice that the `B` rows add up to 4.17 (1.99 + 2.18), and the `A` rows to 7.13 (5.34 + 1.79). The VAT is worked out for each row, not for each product.

---

## What I want from you

Implement the enums, the record and the receipt class so that the tests pass.

1. **[`VatRate`](../src/main/java/io/github/chris013/practice/task13/VatRate.java)**
   * `STANDARD` has code `'A'` and 19 percent. `REDUCED` has code `'B'` and 7 percent.
   * `code()` returns the printed letter. `percent()` returns the rate as an `int`.
   * `fromCode(char)` finds the rate for a code. Codes are case-sensitive, as printed.
   * An unknown code throws `IllegalArgumentException`. The message names the code, for example `Unknown VAT code 'C'`.

2. **[`Category`](../src/main/java/io/github/chris013/practice/task13/Category.java)**
   * `vatRate()` and `minimumAge()` return the values in the table above. The constants are given; do not change them.
   * `isAgeRestricted()` is `true` exactly when the minimum age is greater than `0`.

3. **[`LineItem`](../src/main/java/io/github/chris013/practice/task13/LineItem.java)** (a record)
   * `name` must not be `null` (`NullPointerException`) and must not be blank (`IllegalArgumentException`).
   * `category` must not be `null`.
   * `unitPriceGross` must not be `null`, must not be negative, and must have **scale at most 2**. A price such as `1.005` is rejected with `IllegalArgumentException`. Do not round it: a till must never change a price it was given. A price of `0.00` is valid.
   * Store the price with scale 2, so `1.1` becomes `1.10`.
   * `quantity` must be at least 1.
   * `totalGross()` is the unit price times the quantity, with scale 2. For example, `1.09` x 2 is `2.18`.

4. **[`Receipt`](../src/main/java/io/github/chris013/practice/task13/Receipt.java)**
   * `Receipt.of(List)` copies the list. A `null` list or a `null` item throws `NullPointerException`. An empty list is allowed.
   * `items()` returns the items in scan order. The returned list cannot be changed.
   * `totalGross()` is the sum of all line totals, with scale 2. An empty receipt has total `0.00`.

5. **`vatBreakdown()`** returns a `Map<VatRate, VatLine>`:
   * It has one row per rate that occurs on the receipt, and no row for a rate that does not occur.
   * The rows are in enum order: `STANDARD` (A) before `REDUCED` (B), whatever the scan order. Use an `EnumMap`.
   * The map cannot be changed.
   * For each rate, first add up the gross of all its items. Then compute the VAT **once**: `vat = gross x percent / (100 + percent)`, rounded `HALF_UP` to 2 decimals. Then `net = gross - vat`.
   * Worked examples (these are in the tests):
     * A soft drink at 10.00 gross: `10.00 x 19 / 119 = 1.5966`, so VAT `1.60` and net `8.40`.
     * Three soft drinks at 0.10 each: the rate group is `0.30`, and `0.30 x 19 / 119 = 0.0479`, so VAT `0.05` and net `0.25`. Rounding each line first would give `0.02` three times, which is `0.06`. That is wrong for the tax office, so do not do it.

6. **`minimumAgeToVerify()`** returns the highest minimum age of the items, as an `OptionalInt`. It is empty when no item has a minimum age above `0`. For beer and tobacco together it is `18`.

7. **`categories()`** returns the set of categories on the receipt, each once, in enum order. The set cannot be changed.

## Constraints & non-goals

* **No `ordinal()` anywhere in your code.** The ordinal is the position in the source file. Reordering the constants would silently change your results. Use the fields.
* **No `switch` on `Category` or `VatRate`** in `Receipt` or in new code. If you feel you need one, the rule belongs on the constant. Adding a category must not require touching any switch.
* **No `double` or `float`** for money. Use `BigDecimal`.
* **No rounding per line.** Round once per rate group.
* **No date-dependent rates** in this task. That is stretch goal 1.
* **No printing and no persistence.** The receipt is an in-memory value.

## How I'd approach it

### Why not int constants and ordinal()?

The four Effective Java items below are the reason for the design. Each one is a small "before" and "after". Read the "before" and ask what goes wrong when someone changes the code next year.

**Item 34: use enums instead of int constants.** An `int` accepts any value, and the compiler cannot tell a VAT rate from a count.

```java
// Before: any int is allowed, and nothing says what the numbers mean
public static final int STANDARD = 0;
public static final int REDUCED = 1;
int percent = category == BOOKS ? 7 : 19;

// After: a closed type, and the data sits on the constant
VatRate.REDUCED.percent();   // 7
```

**Item 35: use instance fields instead of ordinals.** `ordinal()` is the position of the constant in the source. It is not a value you own. If someone adds a constant in the middle, every array index and every number you stored moves.

```java
// Before: the array index depends on the declaration order
String[] labels = {"Standard", "Reduced"};
String label = labels[rate.ordinal()];

// After: the data is a field of the constant
VatRate.REDUCED.code();   // 'B'
```

**Item 36: use `EnumSet` instead of bit fields.** Bit flags are unreadable and easy to combine wrongly. An `EnumSet` is type-safe, prints nicely and is just as fast.

```java
// Before: a bit per category, and the arithmetic is the documentation
static final int BEER = 1 << 0;
static final int TOBACCO = 1 << 1;
int restricted = BEER | TOBACCO;

// After
EnumSet<Category> categories = EnumSet.of(Category.BEER_AND_WINE, Category.TOBACCO);
```

**Item 37: use `EnumMap` instead of ordinal indexing.** Indexing an array by `ordinal()` works until someone reorders the enum, and it is hard to read.

```java
// Before: the array slot is the ordinal
BigDecimal[] gross = new BigDecimal[VatRate.values().length];
gross[rate.ordinal()] = gross[rate.ordinal()].add(amount);

// After: the map knows the key, and iterates in declaration order
Map<VatRate, BigDecimal> gross = new EnumMap<>(VatRate.class);
gross.merge(rate, amount, BigDecimal::add);
```

### The order I'd work in

1. Start with `VatRate` and `Category`. They are pure data with a few accessors, and their tests tell you when you are done.
2. Then `LineItem`. Do the validation in the compact constructor, and write `totalGross()` last.
3. Then `Receipt`: `of`, `items`, `totalGross`, `minimumAgeToVerify`, `categories`. Finish with `vatBreakdown`, which is the hardest part.

### Things that will bite you

* **Group first, then compute the VAT.** The three-argument `Collectors.groupingBy` takes a map factory as its second argument. Use it with `EnumMap::new` and a `Collectors.reducing` as the downstream collector. Then turn each group into a `VatLine` in a separate step.
* **Pick the starting value of a sum carefully.** `BigDecimal.ZERO` has scale 0. An empty receipt would then print `0` instead of `0.00`, and an `equals` comparison would fail.
* **`BigDecimal.equals` depends on scale**, and `compareTo` does not. Money in this task always has scale 2, so `equals` is a fair test. Keep it that way.
* **`divide` needs a scale and a rounding mode.** Without them, a non-terminating result like `1/119` throws `ArithmeticException`.

## Tests

Run: `mvn test -Dtest='**/task13/**'`

Four test classes cover the contract:

* [`VatRateTest`](../src/test/java/io/github/chris013/practice/task13/VatRateTest.java): the codes, the percentages and `fromCode`.
* [`CategoryTest`](../src/test/java/io/github/chris013/practice/task13/CategoryTest.java): every category against the table, and the age-restricted flag.
* [`LineItemTest`](../src/test/java/io/github/chris013/practice/task13/LineItemTest.java): the validation rules, the scale and the line total.
* [`ReceiptTest`](../src/test/java/io/github/chris013/practice/task13/ReceiptTest.java): totals, the VAT breakdown with both rates, the rounding example, the enum order, the age check, the categories and the defensive copy.

<details>
<summary>Show the tests</summary>

<!-- embed: src/test/java/io/github/chris013/practice/task13/VatRateTest.java -->
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task13/CategoryTest.java -->
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task13/LineItemTest.java -->
<!-- /embed -->

<!-- embed: src/test/java/io/github/chris013/practice/task13/ReceiptTest.java -->
<!-- /embed -->

</details>

## Definition of done

- [ ] All tests green, and the whole task runs in well under a second.
- [ ] No `ordinal()` and no `switch` on `Category` or `VatRate` anywhere in your code.
- [ ] VAT is computed once per rate group, with `HALF_UP` to 2 decimals.
- [ ] `vatBreakdown()` is an `EnumMap` in enum order and cannot be changed from outside.
- [ ] Money is `BigDecimal` with scale 2. No `double`.
- [ ] Your PR description says what happens when someone adds a new category, and which files change.

## Stretch goals

1. **A VAT rate that depends on the date.** In Germany, from 1 July to 31 December 2020, the standard rate was 16 % and the reduced rate 5 %. A restaurant meal sold in September 2020 gets the reduced rate of 5 %, and the same meal sold in January 2021 gets 7 %. How does the enum design change? Consider a `VatSchedule` that maps a date to a rate, and what is still a constant. Write down your answer before you write code.
2. **Print the receipt.** Write a `ReceiptPrinter` that produces the text from the sample above. Decide where the formatting belongs. It should not be in `Receipt`. Test it with an exact expected string, and think about alignment of the decimal points.
3. **Use `EnumSet` for the rules.** Add a method that returns all categories that are *not* age-restricted, and one that returns all categories from `DAIRY` to `SPIRITS`. Try `EnumSet.complementOf` and `EnumSet.range`. Which one is clearer to read than a hand-written list?
4. **Store the code, not the ordinal.** Imagine the receipt is saved to a database. Write the code that stores the VAT rate, and the code that reads it back. What breaks if you store `ordinal()`, and what changes if the constants are reordered later?

---

## Reference solution

<details>
<summary>Spoiler: open only after you have made a serious attempt</summary>

### Key decisions

**1. Every rule about a category lives on the constant.** `Category` holds its `VatRate` and its `minimumAge` as fields, set in the constructor. The rest of the code calls `category.vatRate()`. Adding a category is one line. The rejected alternative was a `switch` in a helper method, which is how the original bug happened.

**2. VAT is computed once per rate group, from the group's gross sum.** The gross of each rate is summed first, then one division produces the VAT. The rejected alternative is rounding each line's VAT and adding the results. That gives `0.06` in the three-line example, instead of `0.05`. The tax office expects the per-rate figure, so the code must produce it.

**3. The VAT formula is `gross x p / (100 + p)`.** The price already includes VAT, so the VAT part is the fraction `p / (100 + p)` of the gross. Multiplying by `p / 100` would compute VAT on a net price. The division uses scale 2 and `HALF_UP`, as the contract in this task says. The rounding rule is part of the spec, not a detail to pick freely.

**4. `LineItem` rejects a price with more than two decimals instead of rounding it.** A till that quietly rounds `1.005` to `1.01` has changed the customer's price. Rejecting it makes the bug visible at the point where the data enters. Storing the price with scale 2 means every amount on the receipt prints the same way.

**5. The breakdown is an `EnumMap`, built by `groupingBy` with a map factory.** A `HashMap` would print the rows in an arbitrary order, and the order would change between JVM runs. `groupingBy` with `() -> new EnumMap<>(VatRate.class)` keeps the enum order and needs no extra loop. The result is wrapped in `Collections.unmodifiableMap`, because the receipt is a value and callers must not change it.

**6. `minimumAgeToVerify` returns `OptionalInt`, not `0`.** Both work for this API. `0` is simpler for a caller that only compares ages: `if (age >= receipt.minimumAgeToVerify().orElse(0))`. `OptionalInt.empty()` says in the type that there is no age check, so a caller cannot read `0` as a real age. I went with `OptionalInt` because the type makes "no age check" visible at the call site, and a reader cannot mistake it for an age of zero. The filter `age > 0` before `max()` is what makes an unrestricted receipt empty.

**7. `categories()` returns a `Set`, not an `EnumSet`.** The implementation is an `EnumSet`, which is fast and ordered. The public type is `Set`, so callers do not depend on it. `EnumSet` has no unmodifiable form, so the set is wrapped in `Collections.unmodifiableSet`.

**8. `Receipt.of` copies the list with `List.copyOf`.** The copy means a later change to the caller's list does not change the receipt. `List.copyOf` also rejects `null` items, so the receipt never holds a `null` line.

### Code

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task13/VatRate.java -->
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task13/Category.java -->
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task13/LineItem.java -->
<!-- /embed -->

<!-- embed: solutions/src/main/java/io/github/chris013/practice/task13/Receipt.java -->
<!-- /embed -->

### What I typically flag in first attempts

| Finding | Why it matters |
|---|---|
| `ordinal()` used as an array index, or stored in a file or database | Reordering the constants silently changes every stored value and every array slot. Use `code()` or the name. |
| A `switch` over `Category` in `Receipt` to find the VAT rate | The rate is data on the constant. A new category or a changed rate then needs edits in several places, which is exactly how the restaurant-food bug happened. |
| Rounding each line's VAT and adding the results | Gives `0.06` instead of `0.05` in the three-line example. The tax office expects the per-rate figure. |
| `double` or `float` for prices and VAT | Binary floating point cannot represent `0.10` exactly, so sums drift. Use `BigDecimal`. |
| `HashMap` instead of `EnumMap` | The iteration order is arbitrary, so the receipt can print rate B before rate A. `EnumMap` follows the declaration order and is faster. |
| `Collectors.groupingBy` without a map factory | It returns a `HashMap`, so the order problem comes back without any warning. |
| Returning `0` for "no age check" without saying so | `0` and `OptionalInt.empty()` both work here. The bug is leaving the meaning unclear, so a caller treats `0` as a real age. |
| `BigDecimal.ZERO` as the start value of the sum | It has scale 0, so an empty receipt prints `0`, not `0.00`. |

</details>
