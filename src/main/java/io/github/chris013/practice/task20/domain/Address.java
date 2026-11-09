package io.github.chris013.practice.task20.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/** A postal address. The postal code format depends on the country, so it is checked here. */
public record Address(String street, String postalCode, String city, Country country) {

    private static final Pattern GERMAN_POSTAL_CODE = Pattern.compile("\\d{5}");
    private static final Pattern AUSTRIAN_OR_SWISS_POSTAL_CODE = Pattern.compile("\\d{4}");

    public Address {
        Objects.requireNonNull(street, "street");
        Objects.requireNonNull(postalCode, "postalCode");
        Objects.requireNonNull(city, "city");
        Objects.requireNonNull(country, "country");
        Pattern format = country == Country.DE ? GERMAN_POSTAL_CODE : AUSTRIAN_OR_SWISS_POSTAL_CODE;
        if (!format.matcher(postalCode).matches()) {
            throw new IllegalArgumentException("invalid postal code '%s' for %s".formatted(postalCode, country));
        }
    }
}
