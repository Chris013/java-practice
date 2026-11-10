package io.github.chris013.practice.task32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeliveryAddressResolverTest {

    private static final Set<String> DELIVERABLE = Set.of("DE", "AT");
    private static final Address HOME = new Address("Hauptstrasse 1", "10115", "Berlin", "DE");
    private static final Address PICKUP = new Address("Kantstrasse 9", "10623", "Berlin", "DE");
    private static final Address PICKUP_IN_VIENNA = new Address("Ringstrasse 3", "1010", "Vienna", "AT");
    private static final Address FOREIGN_HOME = new Address("Rue de Rivoli 2", "75001", "Paris", "FR");
    private static final CustomerId ANNA = new CustomerId("anna");
    private static final CustomerId BEN = new CustomerId("ben");
    private static final CustomerId NOBODY = new CustomerId("nobody");

    private final FakeCustomerDirectory customers = new FakeCustomerDirectory();
    private final FakePickupPointDirectory pickupPoints = new FakePickupPointDirectory();

    private DeliveryAddressResolver resolver() {
        return new DeliveryAddressResolver(customers, pickupPoints, DELIVERABLE);
    }

    private static Customer customer(CustomerId id, Address home, String pickupId) {
        return new Customer(id, "Test customer", home, pickupId);
    }

    @Nested
    class Construction {

        @Test
        void rejects_null_collaborators() {
            assertThatThrownBy(() -> new DeliveryAddressResolver(null, pickupPoints, DELIVERABLE))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DeliveryAddressResolver(customers, null, DELIVERABLE))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new DeliveryAddressResolver(customers, pickupPoints, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejects_a_null_customer_id() {
            assertThatThrownBy(() -> resolver().resolve(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class PickupPointPreferred {

        @Test
        void uses_the_preferred_pickup_point_when_it_is_open_and_deliverable() {
            customers.with(customer(ANNA, HOME, "PP-1"));
            pickupPoints.with(new PickupPoint("PP-1", PICKUP, true));

            assertThat(resolver().resolve(ANNA)).contains(PICKUP);
        }

        @Test
        void prefers_the_pickup_point_over_the_home_address() {
            customers.with(customer(ANNA, HOME, "PP-1"));
            pickupPoints.with(new PickupPoint("PP-1", PICKUP, true));

            assertThat(resolver().resolve(ANNA)).isNotEqualTo(Optional.of(HOME));
        }

        @Test
        void accepts_a_pickup_point_in_another_deliverable_country() {
            customers.with(customer(ANNA, FOREIGN_HOME, "PP-AT"));
            pickupPoints.with(new PickupPoint("PP-AT", PICKUP_IN_VIENNA, true));

            assertThat(resolver().resolve(ANNA)).contains(PICKUP_IN_VIENNA);
        }
    }

    @Nested
    class FallsBackToHome {

        @ParameterizedTest(name = "{0}")
        @CsvSource({
            "pickup id unknown to the directory, false, true, DE",
            "pickup point is closed, true, false, DE",
            "pickup point is in a country we do not deliver to, true, true, FR"
        })
        void falls_back_to_the_home_address(String scenario, boolean stored, boolean open, String country) {
            customers.with(customer(ANNA, HOME, "PP-1"));
            if (stored) {
                Address pickupAddress = new Address("Somewhere 1", "00001", "Elsewhere", country);
                pickupPoints.with(new PickupPoint("PP-1", pickupAddress, open));
            }

            assertThat(resolver().resolve(ANNA)).contains(HOME);
        }

        @Test
        void uses_the_home_address_when_the_customer_has_no_pickup_point() {
            customers.with(customer(BEN, HOME, null));

            assertThat(resolver().resolve(BEN)).contains(HOME);
        }
    }

    @Nested
    class NoAddress {

        @Test
        void is_empty_for_an_unknown_customer() {
            assertThat(resolver().resolve(NOBODY)).isEmpty();
        }

        @Test
        void is_empty_when_there_is_no_home_address_and_no_pickup_point() {
            customers.with(customer(BEN, null, null));

            assertThat(resolver().resolve(BEN)).isEmpty();
        }

        @Test
        void is_empty_when_the_home_address_is_not_deliverable() {
            customers.with(customer(BEN, FOREIGN_HOME, null));

            assertThat(resolver().resolve(BEN)).isEmpty();
        }
    }

    @Nested
    class ResolveOrThrow {

        @Test
        void returns_the_resolved_address() {
            customers.with(customer(BEN, HOME, null));

            assertThat(resolver().resolveOrThrow(BEN)).isEqualTo(HOME);
        }

        @Test
        void throws_with_the_customer_id_in_the_message() {
            assertThatThrownBy(() -> resolver().resolveOrThrow(NOBODY))
                    .isInstanceOf(NoDeliveryAddressException.class)
                    .hasMessage("no delivery address for customer nobody");
        }
    }

    @Nested
    class DeliveryCity {

        @Test
        void is_the_city_of_the_resolved_address() {
            customers.with(customer(ANNA, FOREIGN_HOME, null)).with(customer(BEN, HOME, null));

            assertThat(resolver().deliveryCity(BEN)).isEqualTo("Berlin");
        }

        @Test
        void is_unknown_when_nothing_resolves() {
            customers.with(customer(ANNA, FOREIGN_HOME, null));

            assertThat(resolver().deliveryCity(ANNA)).isEqualTo("unknown");
            assertThat(resolver().deliveryCity(NOBODY)).isEqualTo("unknown");
        }
    }

    @Nested
    class ResolveAll {

        @Test
        void returns_the_addresses_in_input_order() {
            customers.with(customer(ANNA, HOME, null)).with(customer(BEN, PICKUP_IN_VIENNA, null));

            assertThat(resolver().resolveAll(List.of(BEN, ANNA))).containsExactly(PICKUP_IN_VIENNA, HOME);
            assertThat(resolver().resolveAll(List.of(ANNA, BEN))).containsExactly(HOME, PICKUP_IN_VIENNA);
        }

        @Test
        void skips_customers_without_an_address() {
            customers.with(customer(ANNA, HOME, null)).with(customer(BEN, FOREIGN_HOME, null));

            assertThat(resolver().resolveAll(List.of(ANNA, BEN, NOBODY))).containsExactly(HOME);
        }

        @Test
        void returns_an_empty_list_for_no_ids() {
            assertThat(resolver().resolveAll(List.of())).isEmpty();
        }

        @Test
        void returns_an_unmodifiable_list() {
            customers.with(customer(ANNA, HOME, null));
            List<Address> addresses = resolver().resolveAll(List.of(ANNA));

            assertThatThrownBy(() -> addresses.add(PICKUP)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Laziness {

        @Test
        void resolve_looks_up_the_customer_exactly_once() {
            customers.with(customer(ANNA, HOME, "PP-1"));
            pickupPoints.with(new PickupPoint("PP-1", PICKUP, true));

            resolver().resolve(ANNA);

            assertThat(customers.lookups()).isEqualTo(1);
        }

        @Test
        void resolve_or_throw_looks_up_the_customer_exactly_once() {
            customers.with(customer(BEN, HOME, null));

            resolver().resolveOrThrow(BEN);

            assertThat(customers.lookups()).isEqualTo(1);
        }

        @Test
        void delivery_city_looks_up_the_customer_exactly_once() {
            customers.with(customer(BEN, HOME, null));

            resolver().deliveryCity(BEN);

            assertThat(customers.lookups()).isEqualTo(1);
        }

        @Test
        void resolve_all_looks_up_each_customer_exactly_once() {
            customers.with(customer(ANNA, HOME, null)).with(customer(BEN, HOME, null));

            resolver().resolveAll(List.of(ANNA, BEN));

            assertThat(customers.lookups()).isEqualTo(2);
        }

        @Test
        void does_not_touch_the_pickup_directory_without_a_pickup_point_id() {
            customers.with(customer(BEN, HOME, null));

            resolver().resolve(BEN);

            assertThat(pickupPoints.lookups()).isZero();
        }

        @Test
        void does_not_touch_the_pickup_directory_for_an_unknown_customer() {
            resolver().resolve(NOBODY);

            assertThat(pickupPoints.lookups()).isZero();
        }
    }
}
