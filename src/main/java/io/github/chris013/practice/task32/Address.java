package io.github.chris013.practice.task32;

import java.util.Objects;
import java.util.regex.Pattern;

/** A postal address. The country code is ISO 3166-1 alpha-2, for example {@code DE}. */
public record Address(String street, String postalCode, String city, String countryCode) {

    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

    public Address {
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(postalCode, "postalCode");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(countryCode, "countryCode");
        if (!COUNTRY_CODE.matcher(countryCode).matches()) {
            throw new IllegalArgumentException("Country code must be two upper-case letters, was " + countryCode);
        }
    }
}
