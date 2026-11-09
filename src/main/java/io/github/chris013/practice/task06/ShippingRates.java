package io.github.chris013.practice.task06;

import java.math.BigDecimal;

/** Our price list and delivery promises. Task 06: read {@code tasks/06-shipping-sealed-types.md} first. */
public final class ShippingRates {

    /**
     * @return the price in EUR, rounded half-up to cents
     * @throws NotShippableException if we can't ship this shipment to this zone
     */
    public BigDecimal priceFor(Shipment shipment, Zone zone) {
        throw new UnsupportedOperationException("TODO: Task 06");
    }

    /**
     * @return how many working days the shipment is usually underway
     * @throws NotShippableException if we can't ship this shipment to this zone
     */
    public int transitDays(Shipment shipment, Zone zone) {
        throw new UnsupportedOperationException("TODO: Task 06, part 2");
    }
}
