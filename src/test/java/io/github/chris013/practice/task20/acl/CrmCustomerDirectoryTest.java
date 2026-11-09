package io.github.chris013.practice.task20.acl;

import static io.github.chris013.practice.task20.acl.CrmRecords.aBusinessCustomerRecord;
import static io.github.chris013.practice.task20.acl.CrmRecords.aPrivateCustomerRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task20.crm.CrmException;
import io.github.chris013.practice.task20.domain.Address;
import io.github.chris013.practice.task20.domain.BusinessCustomer;
import io.github.chris013.practice.task20.domain.CorruptCustomerDataException;
import io.github.chris013.practice.task20.domain.Country;
import io.github.chris013.practice.task20.domain.Customer;
import io.github.chris013.practice.task20.domain.CustomerDirectoryUnavailableException;
import io.github.chris013.practice.task20.domain.CustomerId;
import io.github.chris013.practice.task20.domain.CustomerStatus;
import io.github.chris013.practice.task20.domain.EmailAddress;
import io.github.chris013.practice.task20.domain.PrivateCustomer;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CrmCustomerDirectoryTest {

    private static final CustomerId ID = new CustomerId("4711");

    private Optional<Customer> lookup(CrmRecords record) {
        return new CrmCustomerDirectory(new FakeLegacyCrmClient().with(record.build())).findById(ID);
    }

    private Customer lookupOrFail(CrmRecords record) {
        return lookup(record).orElseThrow();
    }

    @Nested
    class PrivateCustomers {

        @Test
        void maps_the_id_the_names_and_the_address() {
            assertThat(lookupOrFail(aPrivateCustomerRecord())).isInstanceOfSatisfying(PrivateCustomer.class, p -> {
                assertThat(p.id()).isEqualTo(ID);
                assertThat(p.firstName()).isEqualTo("Erika");
                assertThat(p.lastName()).isEqualTo("Mustermann");
                assertThat(p.address()).isEqualTo(new Address("Musterstrasse 1", "10115", "Berlin", Country.DE));
            });
        }

        @Test
        void trims_every_field() {
            CrmRecords record = aPrivateCustomerRecord()
                    .with("NAME1", "  Mustermann ")
                    .with("NAME2", " Erika\t")
                    .with("STRASSE", " Musterstrasse 1 ")
                    .with("PLZ", " 10115 ")
                    .with("ORT", " Berlin ");

            assertThat(lookupOrFail(record)).isInstanceOfSatisfying(PrivateCustomer.class, p -> {
                assertThat(p.lastName()).isEqualTo("Mustermann");
                assertThat(p.firstName()).isEqualTo("Erika");
                assertThat(p.address()).isEqualTo(new Address("Musterstrasse 1", "10115", "Berlin", Country.DE));
            });
        }

        @Test
        void treats_a_null_legacy_field_as_blank() {
            CrmRecords record = aPrivateCustomerRecord().with("EMAIL", null).with("KREDITLIMIT", null);

            assertThat(lookupOrFail(record)).isInstanceOfSatisfying(PrivateCustomer.class, p -> {
                assertThat(p.email()).isEmpty();
                assertThat(p.creditLimit().toPlainString()).isEqualTo("0.00");
            });
        }

        @ParameterizedTest
        @CsvSource({"A,ACTIVE", "I,INACTIVE", "G,BLOCKED"})
        void maps_the_status_codes(String code, CustomerStatus expected) {
            assertThat(lookupOrFail(aPrivateCustomerRecord().with("STATUS", code)).status()).isEqualTo(expected);
        }
    }

    @Nested
    class BusinessCustomers {

        @Test
        void maps_the_company_name_and_the_contact_person() {
            assertThat(lookupOrFail(aBusinessCustomerRecord())).isInstanceOfSatisfying(BusinessCustomer.class, b -> {
                assertThat(b.id()).isEqualTo(ID);
                assertThat(b.companyName()).isEqualTo("Muster GmbH");
                assertThat(b.contactPerson()).contains("Max Muster");
            });
        }

        @Test
        void a_blank_contact_person_becomes_empty() {
            CrmRecords record = aBusinessCustomerRecord().with("NAME2", "   ");

            assertThat(lookupOrFail(record)).isInstanceOfSatisfying(BusinessCustomer.class,
                    b -> assertThat(b.contactPerson()).isEmpty());
        }

        @Test
        void ignores_the_birth_date_field_for_businesses() {
            CrmRecords record = aBusinessCustomerRecord().with("GEBDAT", "31.02.1980");

            assertThat(lookupOrFail(record)).isInstanceOfSatisfying(BusinessCustomer.class,
                    b -> assertThat(b.companyName()).isEqualTo("Muster GmbH"));
        }

        @Test
        void a_business_without_a_company_name_is_corrupt() {
            CrmRecords record = aBusinessCustomerRecord().with("NAME1", "");

            assertThatThrownBy(() -> lookup(record))
                    .isInstanceOfSatisfying(CorruptCustomerDataException.class,
                            e -> assertThat(e.field()).isEqualTo("NAME1"));
        }
    }

    @Nested
    class Countries {

        @ParameterizedTest
        @CsvSource({
            "DE,10115,DE", "D,10115,DE", "DEU,10115,DE", "Deutschland,10115,DE", "germany,10115,DE", "'',10115,DE",
            "A,1010,AT", "AUT,1010,AT", "Österreich,1010,AT", "Oesterreich,1010,AT", "austria,1010,AT",
            "CH,8001,CH", "CHE,8001,CH", "Schweiz,8001,CH", "switzerland,8001,CH"
        })
        void maps_the_legacy_country_names_to_a_country(String land, String plz, Country expected) {
            CrmRecords record = aPrivateCustomerRecord().with("LAND", land).with("PLZ", plz);

            assertThat(lookupOrFail(record).address().country()).isEqualTo(expected);
        }

        @Test
        void an_unknown_country_is_corrupt() {
            CrmRecords record = aPrivateCustomerRecord().with("LAND", "FR");

            assertThatThrownBy(() -> lookup(record))
                    .isInstanceOfSatisfying(CorruptCustomerDataException.class,
                            e -> assertThat(e.field()).isEqualTo("LAND"));
        }
    }

    @Nested
    class BirthDates {

        @ParameterizedTest
        @CsvSource({"31.12.1980,1980-12-31", "29.02.2024,2024-02-29", "01.01.1900,1900-01-01"})
        void parses_the_german_date_format(String gebdat, LocalDate expected) {
            assertThat(lookupOrFail(aPrivateCustomerRecord().with("GEBDAT", gebdat)))
                    .isInstanceOfSatisfying(PrivateCustomer.class, p -> assertThat(p.birthDate()).contains(expected));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "00.00.0000"})
        void an_empty_or_zero_date_means_unknown(String gebdat) {
            assertThat(lookupOrFail(aPrivateCustomerRecord().with("GEBDAT", gebdat)))
                    .isInstanceOfSatisfying(PrivateCustomer.class, p -> assertThat(p.birthDate()).isEmpty());
        }

        @Test
        void a_date_that_does_not_exist_is_corrupt_and_not_moved_to_march() {
            // Lenient parsing would turn 31.02.1980 into 03.03.1980 without a word.
            CrmRecords record = aPrivateCustomerRecord().with("GEBDAT", "31.02.1980");

            assertThatThrownBy(() -> lookup(record))
                    .isInstanceOfSatisfying(CorruptCustomerDataException.class,
                            e -> assertThat(e.field()).isEqualTo("GEBDAT"));
        }
    }

    @Nested
    class Emails {

        @Test
        void lower_cases_and_trims_the_address() {
            CrmRecords record = aPrivateCustomerRecord().with("EMAIL", "  Erika@Example.DE ");

            assertThat(lookupOrFail(record).email()).contains(new EmailAddress("erika@example.de"));
        }

        @Test
        void takes_the_first_valid_address_of_several() {
            CrmRecords record = aPrivateCustomerRecord().with("EMAIL", "junk; erika@example.de ;other@example.de");

            assertThat(lookupOrFail(record).email()).contains(new EmailAddress("erika@example.de"));
        }

        @Test
        void an_empty_email_field_means_no_email() {
            assertThat(lookupOrFail(aPrivateCustomerRecord().with("EMAIL", "")).email()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"no-at-sign", "a@b@c.de", "@example.de", "erika@", "erika@localhost", "erika@.de", "erika@example."})
        void junk_emails_are_ignored_and_are_not_corruption(String junk) {
            CrmRecords record = aPrivateCustomerRecord().with("EMAIL", junk);

            assertThat(lookupOrFail(record).email()).isEmpty();
        }
    }

    @Nested
    class CreditLimits {

        @ParameterizedTest
        @CsvSource({
            "'',0.00", "'0,5',0.50", "'1234',1234.00", "'1234,5',1234.50",
            "'1.234,56',1234.56", "'1.234.567,00',1234567.00"
        })
        void reads_the_german_number_format_with_two_decimals(String raw, String expected) {
            CrmRecords record = aPrivateCustomerRecord().with("KREDITLIMIT", raw);

            assertThat(lookupOrFail(record).creditLimit().toPlainString()).isEqualTo(expected);
        }

        @Test
        void a_negative_limit_is_corrupt() {
            CrmRecords record = aPrivateCustomerRecord().with("KREDITLIMIT", "-5");

            assertThatThrownBy(() -> lookup(record))
                    .isInstanceOfSatisfying(CorruptCustomerDataException.class,
                            e -> assertThat(e.field()).isEqualTo("KREDITLIMIT"));
        }
    }

    @Nested
    class CorruptData {

        @ParameterizedTest
        @CsvSource({
            "TYP,X", "TYP,''",
            "STATUS,Z", "STATUS,''",
            "LAND,FR",
            "PLZ,123", "PLZ,ABCDE", "PLZ,1011", "PLZ,''",
            "NAME1,''", "NAME2,''",
            "GEBDAT,31.02.1980", "GEBDAT,29.02.2023", "GEBDAT,1980-12-31", "GEBDAT,32.01.1980",
            "KREDITLIMIT,abc", "KREDITLIMIT,'1.234,56abc'"
        })
        void names_the_legacy_field_and_the_customer(String field, String value) {
            CrmRecords record = aPrivateCustomerRecord().with(field, value);

            assertThatThrownBy(() -> lookup(record)).isInstanceOfSatisfying(CorruptCustomerDataException.class, e -> {
                assertThat(e.field()).isEqualTo(field);
                assertThat(e.customerId()).isEqualTo(ID);
            });
        }

        @Test
        void the_message_says_which_customer_and_what_is_wrong() {
            CrmRecords record = aPrivateCustomerRecord().with("PLZ", "123");

            assertThatThrownBy(() -> lookup(record)).hasMessage("customer 4711: invalid PLZ '123'");
        }

        @Test
        void keeps_the_postal_code_problem_as_the_cause() {
            CrmRecords record = aPrivateCustomerRecord().with("PLZ", "123");

            assertThatThrownBy(() -> lookup(record)).hasCauseInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void a_postal_code_must_match_the_country() {
            CrmRecords record = aPrivateCustomerRecord().with("LAND", "AT"); // AT codes have 4 digits, 10115 has 5

            assertThatThrownBy(() -> lookup(record))
                    .isInstanceOfSatisfying(CorruptCustomerDataException.class,
                            e -> assertThat(e.field()).isEqualTo("PLZ"));
        }
    }

    @Nested
    class Availability {

        @Test
        void returns_empty_when_the_crm_has_no_such_customer() {
            CrmCustomerDirectory directory = new CrmCustomerDirectory(new FakeLegacyCrmClient());

            assertThat(directory.findById(ID)).isEmpty();
        }

        @Test
        void asks_the_crm_for_the_customer_number() {
            FakeLegacyCrmClient crm = new FakeLegacyCrmClient().with(aPrivateCustomerRecord().build());

            new CrmCustomerDirectory(crm).findById(ID);

            assertThat(crm.lastKundnr()).isEqualTo("4711");
        }

        @ParameterizedTest
        @ValueSource(strings = {"TIMEOUT", "CONNECTION", "AUTH"})
        void turns_every_crm_failure_into_unavailable_and_keeps_the_cause(String code) {
            CrmCustomerDirectory directory = new CrmCustomerDirectory(new FakeLegacyCrmClient().failsWith(code));

            assertThatThrownBy(() -> directory.findById(ID))
                    .isInstanceOfSatisfying(CustomerDirectoryUnavailableException.class, e ->
                            assertThat(e.getCause()).isInstanceOfSatisfying(CrmException.class,
                                    crmException -> assertThat(crmException.code()).isEqualTo(code)));
        }

        @Test
        void rejects_a_missing_client() {
            assertThatThrownBy(() -> new CrmCustomerDirectory(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
