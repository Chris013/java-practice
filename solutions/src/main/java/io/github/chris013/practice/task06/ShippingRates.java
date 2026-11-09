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
