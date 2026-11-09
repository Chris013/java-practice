package io.github.chris013.practice.task13;

/**
 * What a product is, for the till. Each category knows its own VAT rate and minimum age, so the rest of the code
 * never needs a {@code switch} on the category. Adding a category is one line here and nothing else.
 */
public enum Category {

    FRUIT_AND_VEGETABLES(VatRate.REDUCED, 0),
    BAKERY(VatRate.REDUCED, 0),
    DAIRY(VatRate.REDUCED, 0),
    BOOKS(VatRate.REDUCED, 0),
    SOFT_DRINKS(VatRate.STANDARD, 0),
    BEER_AND_WINE(VatRate.STANDARD, 16),
    SPIRITS(VatRate.STANDARD, 18),
    TOBACCO(VatRate.STANDARD, 18),
    HOUSEHOLD(VatRate.STANDARD, 0);

    private final VatRate vatRate;
    private final int minimumAge;

    Category(VatRate vatRate, int minimumAge) {
        this.vatRate = vatRate;
        this.minimumAge = minimumAge;
    }

    /** The VAT rate that applies to products in this category. */
    public VatRate vatRate() {
        return vatRate;
    }

    /** The age the cashier must verify, or {@code 0} when no check is needed. */
    public int minimumAge() {
        return minimumAge;
    }

    /** {@code true} when the till must ask for ID before selling this category. */
    public boolean isAgeRestricted() {
        return minimumAge > 0;
    }
}
