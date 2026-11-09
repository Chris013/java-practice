package io.github.chris013.practice.task24.application;

import java.util.Objects;

/**
 * The outcome of placing an order: either the order number, or the reason it was refused. A result type instead
 * of an exception, because a refused order is an expected business outcome, not a bug.
 */
public sealed interface PlaceOrderResult permits PlaceOrderResult.Placed, PlaceOrderResult.Rejected {

    record Placed(String orderId) implements PlaceOrderResult {

        public Placed {
            Objects.requireNonNull(orderId, "orderId");
        }
    }

    /**
     * {@code sku} names the product that is out of stock. It is set for {@link RejectionReason#OUT_OF_STOCK}
     * and {@code null} for every other reason. The constructor enforces that, so no caller can get it wrong.
     */
    record Rejected(RejectionReason reason, String sku) implements PlaceOrderResult {

        public Rejected {
            Objects.requireNonNull(reason, "reason");
            if (reason == RejectionReason.OUT_OF_STOCK) {
                Objects.requireNonNull(sku, "sku");
            } else if (sku != null) {
                throw new IllegalArgumentException("sku is only set for OUT_OF_STOCK, not for " + reason);
            }
        }

        public static Rejected because(RejectionReason reason) {
            return new Rejected(reason, null);
        }

        public static Rejected outOfStock(String sku) {
            return new Rejected(RejectionReason.OUT_OF_STOCK, sku);
        }
    }
}
