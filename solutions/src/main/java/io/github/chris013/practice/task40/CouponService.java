package io.github.chris013.practice.task40;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Validates and normalizes coupon codes, computes discounts and notifies listeners about redemptions.
 *
 * <p>Every rule is a small function. The class composes them with {@code andThen}, {@code and} and
 * {@code Comparator} combinators instead of subclassing a template or chaining step objects.
 */
public final class CouponService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Set<String> BLOCKLIST = Set.of("TEST00", "ADMIN0");

    private static final Function<String, String> TRIM = String::trim;
    private static final Function<String, String> NORMALIZE = TRIM
            .andThen(s -> s.replace(" ", "").replace("-", ""))
            .andThen(s -> s.toUpperCase(Locale.ROOT))
            .andThen(s -> s.replace('O', '0'));

    private static final Predicate<String> LENGTH_6_TO_12 = code -> code.length() >= 6 && code.length() <= 12;
    private static final Predicate<String> ONLY_UPPER_CASE_AND_DIGITS =
            code -> code.chars().allMatch(CouponService::isUpperCaseLetterOrDigit);
    private static final Predicate<String> NOT_STARTING_WITH_DIGIT =
            code -> !code.isEmpty() && !isDigit(code.charAt(0));
    private static final Predicate<String> NOT_BLOCKLISTED = code -> !BLOCKLIST.contains(code);
    // and() short-circuits in the order written, so the length check runs before charAt(0) can fail.
    private static final Predicate<String> VALID_CODE = LENGTH_6_TO_12
            .and(ONLY_UPPER_CASE_AND_DIGITS)
            .and(NOT_STARTING_WITH_DIGIT)
            .and(NOT_BLOCKLISTED);

    /**
     * One rule per type, in an {@code EnumMap}. A switch on the enum would also work, but javac compiles it
     * to a synthetic {@code CouponService$1} class, which is exactly the kind of class this refactoring removes.
     */
    private static final Map<DiscountType, DiscountRule> RULES = buildRules();

    private static final Comparator<Coupon> DISPLAY_ORDER = Comparator.comparing(Coupon::type)
            .thenComparing(Coupon::value, Comparator.reverseOrder())
            .thenComparing(Coupon::code);

    private final List<RedemptionListener> listeners = new ArrayList<>();

    /** Trims, removes spaces and dashes, upper-cases, and replaces the letter O with the digit 0. */
    public String normalize(String raw) {
        Objects.requireNonNull(raw, "raw");
        return NORMALIZE.apply(raw);
    }

    /** Expects an already normalized code. Lower-case input is rejected on purpose. */
    public boolean isValid(String normalizedCode) {
        Objects.requireNonNull(normalizedCode, "normalizedCode");
        return VALID_CODE.test(normalizedCode);
    }

    /**
     * Normalizes and validates the code, then checks the value against the rules of the discount type.
     *
     * @throws IllegalArgumentException if the code or the value is not acceptable
     */
    public Coupon create(String rawCode, DiscountType type, BigDecimal value) {
        Objects.requireNonNull(rawCode, "rawCode");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        String code = normalize(rawCode);
        if (!isValid(code)) {
            throw new IllegalArgumentException("Invalid coupon code: " + code);
        }
        if (!ruleFor(type).allowsValue().test(value)) {
            throw new IllegalArgumentException("Value " + value + " is not allowed for " + type);
        }
        return new Coupon(code, type, value);
    }

    /** Returns the amount the customer pays, with scale 2, and notifies every listener with it. */
    public BigDecimal apply(Coupon coupon, BigDecimal orderTotal, BigDecimal shippingCost) {
        Objects.requireNonNull(coupon, "coupon");
        Objects.requireNonNull(orderTotal, "orderTotal");
        Objects.requireNonNull(shippingCost, "shippingCost");
        DiscountRule rule = ruleFor(coupon.type());
        BigDecimal discounted = rule.totalAfterDiscount().apply(orderTotal, coupon.value());
        BigDecimal payable = rule.chargesShipping() ? discounted.add(shippingCost) : discounted;
        BigDecimal amount = payable.setScale(2, RoundingMode.HALF_UP);
        listeners.forEach(listener -> listener.redeemed(coupon, amount));
        return amount;
    }

    public void addListener(RedemptionListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Sorts by type (enum order), then value (highest first), then code. The input is not changed. */
    public List<Coupon> sortForDisplay(Collection<Coupon> coupons) {
        Objects.requireNonNull(coupons, "coupons");
        return coupons.stream().sorted(DISPLAY_ORDER).toList();
    }

    /**
     * The behaviour of one discount type, as data: a record of functions instead of a subclass per type.
     *
     * @param allowsValue          is this value acceptable for the type?
     * @param totalAfterDiscount   (order total, coupon value) to the total before shipping
     * @param chargesShipping      is the shipping cost added?
     */
    private record DiscountRule(
            Predicate<BigDecimal> allowsValue,
            BiFunction<BigDecimal, BigDecimal, BigDecimal> totalAfterDiscount,
            boolean chargesShipping) {
    }

    private static Map<DiscountType, DiscountRule> buildRules() {
        Map<DiscountType, DiscountRule> rules = new EnumMap<>(DiscountType.class);
        rules.put(DiscountType.PERCENT, new DiscountRule(
                percent -> percent.compareTo(BigDecimal.ONE) >= 0 && percent.compareTo(HUNDRED) <= 0,
                (total, percent) -> total.subtract(percentOf(total, percent)),
                true));
        rules.put(DiscountType.FIXED, new DiscountRule(
                amount -> amount.signum() > 0,
                (total, amount) -> total.subtract(amount).max(BigDecimal.ZERO),
                true));
        rules.put(DiscountType.FREE_SHIPPING, new DiscountRule(
                value -> value.signum() == 0,
                (total, ignored) -> total,
                false));
        return Collections.unmodifiableMap(rules);
    }

    private static DiscountRule ruleFor(DiscountType type) {
        DiscountRule rule = RULES.get(type);
        if (rule == null) {
            // Only reachable if someone adds a DiscountType constant without adding its rule above.
            throw new IllegalStateException("No rule for " + type);
        }
        return rule;
    }

    private static BigDecimal percentOf(BigDecimal total, BigDecimal percent) {
        return total.multiply(percent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isUpperCaseLetterOrDigit(int c) {
        return (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }
}
