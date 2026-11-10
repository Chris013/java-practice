package io.github.chris013.practice.task40;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Validates and normalizes coupon codes, computes discounts and notifies listeners about redemptions.
 *
 * <p>Task 40: this is the legacy version, written in Java 6 style. It works and its characterization tests are green.
 * Refactor it while keeping them green: read {@code tasks/40-lambdas-refactor-coupons.md} first.
 */
public final class CouponService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Set<String> BLOCKLIST = Set.of("TEST00", "ADMIN0");

    private static final ProcessingStep NORMALIZATION = buildNormalizationChain();

    private static final List<ValidationRule> VALIDATION_RULES = List.of(
            new ValidationRule() {
                @Override
                public boolean holds(String normalizedCode) {
                    return normalizedCode.length() >= 6 && normalizedCode.length() <= 12;
                }
            },
            new ValidationRule() {
                @Override
                public boolean holds(String normalizedCode) {
                    for (int i = 0; i < normalizedCode.length(); i++) {
                        char c = normalizedCode.charAt(i);
                        boolean upperCaseLetter = c >= 'A' && c <= 'Z';
                        boolean digit = c >= '0' && c <= '9';
                        if (!upperCaseLetter && !digit) {
                            return false;
                        }
                    }
                    return true;
                }
            },
            new ValidationRule() {
                @Override
                public boolean holds(String normalizedCode) {
                    if (normalizedCode.isEmpty()) {
                        return false;
                    }
                    char first = normalizedCode.charAt(0);
                    if (first >= '0' && first <= '9') {
                        return false;
                    }
                    return true;
                }
            },
            new ValidationRule() {
                @Override
                public boolean holds(String normalizedCode) {
                    return !BLOCKLIST.contains(normalizedCode);
                }
            });

    private final List<RedemptionListener> listeners = new ArrayList<>();

    /**
     * Trims the code, removes spaces and dashes, upper-cases it and replaces the letter O with the digit 0,
     * because customers type one for the other.
     */
    public String normalize(String raw) {
        Objects.requireNonNull(raw, "raw");
        return NORMALIZATION.process(raw);
    }

    /** Expects an already normalized code. Lower-case input is rejected on purpose. */
    public boolean isValid(String normalizedCode) {
        Objects.requireNonNull(normalizedCode, "normalizedCode");
        for (ValidationRule rule : VALIDATION_RULES) {
            if (!rule.holds(normalizedCode)) {
                return false;
            }
        }
        return true;
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
        if (!calculatorFor(type, value).allowsValue()) {
            throw new IllegalArgumentException("Value " + value + " is not allowed for " + type);
        }
        return new Coupon(code, type, value);
    }

    /**
     * Returns the amount the customer pays, with scale 2, and notifies every listener with it.
     */
    public BigDecimal apply(Coupon coupon, BigDecimal orderTotal, BigDecimal shippingCost) {
        Objects.requireNonNull(coupon, "coupon");
        Objects.requireNonNull(orderTotal, "orderTotal");
        Objects.requireNonNull(shippingCost, "shippingCost");
        BigDecimal amount = calculatorFor(coupon.type(), coupon.value()).apply(orderTotal, shippingCost);
        for (RedemptionListener listener : listeners) {
            listener.redeemed(coupon, amount);
        }
        return amount;
    }

    public void addListener(RedemptionListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Sorts by type (in enum order), then by value (highest first), then by code. Does not change the input. */
    public List<Coupon> sortForDisplay(Collection<Coupon> coupons) {
        Objects.requireNonNull(coupons, "coupons");
        List<Coupon> sorted = new ArrayList<>(coupons);
        sorted.sort(new Comparator<Coupon>() {
            @Override
            public int compare(Coupon a, Coupon b) {
                if (a.type() != b.type()) {
                    return a.type().compareTo(b.type());
                } else {
                    int byValueDescending = b.value().compareTo(a.value());
                    if (byValueDescending != 0) {
                        return byValueDescending;
                    } else {
                        return a.code().compareTo(b.code());
                    }
                }
            }
        });
        return Collections.unmodifiableList(sorted);
    }

    private static ProcessingStep buildNormalizationChain() {
        ProcessingStep trim = new ProcessingStep() {
            @Override
            String doProcess(String input) {
                return input.trim();
            }
        };
        ProcessingStep removeSpacesAndDashes = new ProcessingStep() {
            @Override
            String doProcess(String input) {
                return input.replace(" ", "").replace("-", "");
            }
        };
        ProcessingStep upperCase = new ProcessingStep() {
            @Override
            String doProcess(String input) {
                return input.toUpperCase(Locale.ROOT);
            }
        };
        ProcessingStep letterOToDigitZero = new ProcessingStep() {
            @Override
            String doProcess(String input) {
                return input.replace('O', '0');
            }
        };
        trim.setSuccessor(removeSpacesAndDashes).setSuccessor(upperCase).setSuccessor(letterOToDigitZero);
        return trim;
    }

    private static AbstractDiscountCalculator calculatorFor(DiscountType type, BigDecimal couponValue) {
        switch (type) {
            case PERCENT:
                return new AbstractDiscountCalculator(couponValue) {
                    @Override
                    BigDecimal discountedTotal(BigDecimal orderTotal) {
                        BigDecimal discount = orderTotal.multiply(value).divide(HUNDRED, 2, RoundingMode.HALF_UP);
                        return orderTotal.subtract(discount);
                    }

                    @Override
                    boolean chargesShipping() {
                        return true;
                    }

                    @Override
                    boolean allowsValue() {
                        return value.compareTo(BigDecimal.ONE) >= 0 && value.compareTo(HUNDRED) <= 0;
                    }
                };
            case FIXED:
                return new AbstractDiscountCalculator(couponValue) {
                    @Override
                    BigDecimal discountedTotal(BigDecimal orderTotal) {
                        return orderTotal.subtract(value).max(BigDecimal.ZERO);
                    }

                    @Override
                    boolean chargesShipping() {
                        return true;
                    }

                    @Override
                    boolean allowsValue() {
                        return value.signum() > 0;
                    }
                };
            case FREE_SHIPPING:
                return new AbstractDiscountCalculator(couponValue) {
                    @Override
                    BigDecimal discountedTotal(BigDecimal orderTotal) {
                        return orderTotal;
                    }

                    @Override
                    boolean chargesShipping() {
                        return false;
                    }

                    @Override
                    boolean allowsValue() {
                        return value.signum() == 0;
                    }
                };
            default:
                throw new IllegalArgumentException("Unknown discount type: " + type);
        }
    }
}
