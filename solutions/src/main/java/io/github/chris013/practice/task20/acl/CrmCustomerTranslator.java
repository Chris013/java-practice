package io.github.chris013.practice.task20.acl;

import static java.util.Map.entry;

import io.github.chris013.practice.task20.crm.CrmCustomerRecord;
import io.github.chris013.practice.task20.domain.Address;
import io.github.chris013.practice.task20.domain.BusinessCustomer;
import io.github.chris013.practice.task20.domain.CorruptCustomerDataException;
import io.github.chris013.practice.task20.domain.Country;
import io.github.chris013.practice.task20.domain.Customer;
import io.github.chris013.practice.task20.domain.CustomerId;
import io.github.chris013.practice.task20.domain.CustomerStatus;
import io.github.chris013.practice.task20.domain.EmailAddress;
import io.github.chris013.practice.task20.domain.PrivateCustomer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Turns one legacy CRM record into a domain {@link Customer}. Package-private on purpose: only the
 * directory may use it, so nothing outside the layer can call it with a half-trusted record.
 *
 * <p>Parse, don't validate: every value is checked once, here, and what comes out is always valid.
 * The first problem found is reported; the message names the legacy field so support can find it.
 *
 * <p>Task 20: read {@code tasks/20-anti-corruption-layer.md} first.
 */
final class CrmCustomerTranslator {

    private static final DateTimeFormatter GEBDAT_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final String UNKNOWN_BIRTH_DATE = "00.00.0000";

    /** German number format with an optional comma-separated decimal part: 1.234,56 or 1234 or 0,5. */
    private static final Pattern KREDITLIMIT_FORMAT = Pattern.compile("(?:\\d{1,3}(?:\\.\\d{3})+|\\d+)(?:,\\d{1,2})?");

    /** Keys are upper-case, so lookups are case-insensitive. An empty LAND means Germany (legacy default). */
    private static final Map<String, Country> COUNTRIES = Map.ofEntries(
            entry("D", Country.DE), entry("DE", Country.DE), entry("DEU", Country.DE),
            entry("DEUTSCHLAND", Country.DE), entry("GERMANY", Country.DE),
            entry("A", Country.AT), entry("AT", Country.AT), entry("AUT", Country.AT),
            entry("ÖSTERREICH", Country.AT), entry("OESTERREICH", Country.AT), entry("AUSTRIA", Country.AT),
            entry("CH", Country.CH), entry("CHE", Country.CH), entry("SCHWEIZ", Country.CH),
            entry("SWITZERLAND", Country.CH));

    /**
     * Translates {@code record} for the customer {@code id} that was asked for.
     *
     * <p>The record's own KUNDNR is not used: we asked the CRM for {@code id}, so {@code id} is the
     * trusted identity of the customer.
     *
     * @throws CorruptCustomerDataException for any field that cannot be turned into a valid value
     */
    Customer toDomain(CustomerId id, CrmCustomerRecord record) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(record, "record");
        String type = clean(record.TYP);
        if (!type.equals("P") && !type.equals("F")) {
            throw corrupt(id, "TYP", "invalid TYP '%s'".formatted(type), null);
        }
        CustomerStatus status = status(id, record.STATUS);
        Address address = address(id, record);
        Optional<EmailAddress> email = firstValidEmail(clean(record.EMAIL));
        BigDecimal creditLimit = creditLimit(id, record.KREDITLIMIT);

        if (type.equals("P")) {
            String lastName = required(id, "NAME1", record.NAME1);
            String firstName = required(id, "NAME2", record.NAME2);
            Optional<LocalDate> birthDate = birthDate(id, record.GEBDAT);
            return new PrivateCustomer(id, firstName, lastName, birthDate, address, status, email, creditLimit);
        }
        String companyName = required(id, "NAME1", record.NAME1);
        String contact = clean(record.NAME2);
        Optional<String> contactPerson = contact.isEmpty() ? Optional.empty() : Optional.of(contact);
        // GEBDAT is ignored for businesses: it has no meaning there, so junk in it is not our problem.
        return new BusinessCustomer(id, companyName, contactPerson, address, status, email, creditLimit);
    }

    private static CustomerStatus status(CustomerId id, String rawStatus) {
        return switch (clean(rawStatus)) {
            case "A" -> CustomerStatus.ACTIVE;
            case "I" -> CustomerStatus.INACTIVE;
            case "G" -> CustomerStatus.BLOCKED;
            default -> throw corrupt(id, "STATUS", "invalid STATUS '%s'".formatted(clean(rawStatus)), null);
        };
    }

    private static Address address(CustomerId id, CrmCustomerRecord record) {
        Country country = country(id, record.LAND);
        String postalCode = clean(record.PLZ);
        try {
            return new Address(clean(record.STRASSE), postalCode, clean(record.ORT), country);
        } catch (IllegalArgumentException e) {
            // Keep the Address check as the single source of truth, but report it with the legacy field name.
            throw corrupt(id, "PLZ", "invalid PLZ '%s'".formatted(postalCode), e);
        }
    }

    private static Country country(CustomerId id, String rawLand) {
        String land = clean(rawLand);
        if (land.isEmpty()) {
            return Country.DE;
        }
        Country country = COUNTRIES.get(land.toUpperCase(Locale.ROOT));
        if (country == null) {
            throw corrupt(id, "LAND", "invalid LAND '%s'".formatted(land), null);
        }
        return country;
    }

    private static Optional<LocalDate> birthDate(CustomerId id, String rawBirthDate) {
        String value = clean(rawBirthDate);
        if (value.isEmpty() || value.equals(UNKNOWN_BIRTH_DATE)) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value, GEBDAT_FORMAT));
        } catch (DateTimeParseException e) {
            // STRICT resolving rejects 31.02.1980 instead of quietly turning it into 03.03.1980.
            throw corrupt(id, "GEBDAT", "invalid GEBDAT '%s'".formatted(value), e);
        }
    }

    private static BigDecimal creditLimit(CustomerId id, String rawLimit) {
        String value = clean(rawLimit);
        if (value.isEmpty()) {
            return new BigDecimal("0.00");
        }
        // A regex, not NumberFormat: NumberFormat.parse("1.234,56abc") succeeds and silently ignores the junk.
        if (!KREDITLIMIT_FORMAT.matcher(value).matches()) {
            throw corrupt(id, "KREDITLIMIT", "invalid KREDITLIMIT '%s'".formatted(value), null);
        }
        return new BigDecimal(value.replace(".", "").replace(',', '.')).setScale(2);
    }

    private static Optional<EmailAddress> firstValidEmail(String rawEmails) {
        // The CRM stores zero, one or several addresses separated by ";". Junk is ignored, not corruption.
        return Arrays.stream(rawEmails.split(";"))
                .map(CrmCustomerTranslator::clean)
                .filter(candidate -> !candidate.isEmpty())
                .map(CrmCustomerTranslator::tryEmail)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static Optional<EmailAddress> tryEmail(String candidate) {
        try {
            return Optional.of(new EmailAddress(candidate));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String required(CustomerId id, String field, String raw) {
        String value = clean(raw);
        if (value.isEmpty()) {
            throw corrupt(id, field, "missing " + field, null);
        }
        return value;
    }

    /** Null-safe trim: legacy fields may be null, and we treat null the same as blank. */
    private static String clean(String raw) {
        return raw == null ? "" : raw.strip();
    }

    private static CorruptCustomerDataException corrupt(CustomerId id, String field, String problem, Throwable cause) {
        return new CorruptCustomerDataException(id, field, problem, cause);
    }
}
