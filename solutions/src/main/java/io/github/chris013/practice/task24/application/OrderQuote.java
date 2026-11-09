package io.github.chris013.practice.task24.application;

import io.github.chris013.practice.task24.Item;
import io.github.chris013.practice.task24.OrderRequest;
import io.github.chris013.practice.task24.domain.Country;
import io.github.chris013.practice.task24.domain.Coupon;
import io.github.chris013.practice.task24.domain.Money;
import io.github.chris013.practice.task24.domain.OrderLine;
import io.github.chris013.practice.task24.domain.OrderPricing;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Phase 1 of placing an order (Split Phase): turn the request into domain values and work out the price.
 *
 * <p>This is pure. No collaborators, no clock, no stock, no payment. Whatever it returns, nothing has been touched
 * yet. Every refusal that can be decided from the request alone is decided here, which is why an invalid coupon
 * is reported before an out-of-stock product, as the legacy code did.
 */
sealed interface OrderQuote permits OrderQuote.Accepted, OrderQuote.Refused {

    /** A valid order with its price. Everything phase 2 needs is in here. */
    record Accepted(String customerId, String email, List<OrderLine> lines, OrderPricing pricing)
            implements OrderQuote {

        public Accepted {
            Objects.requireNonNull(customerId, "customerId");
            Objects.requireNonNull(email, "email");
            lines = List.copyOf(lines);
            Objects.requireNonNull(pricing, "pricing");
        }
    }

    record Refused(RejectionReason reason) implements OrderQuote {

        public Refused {
            Objects.requireNonNull(reason, "reason");
        }
    }

    static OrderQuote of(OrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            return new Refused(RejectionReason.EMPTY);
        }
        for (Item item : request.items()) {
            if (!OrderLine.isValidQuantity(item.quantity())) {
                return new Refused(RejectionReason.INVALID_QUANTITY);
            }
        }

        Optional<Country> country = Country.fromCode(request.country());
        if (country.isEmpty()) {
            return new Refused(RejectionReason.UNSUPPORTED_COUNTRY);
        }

        Optional<Coupon> coupon = Coupon.parse(request.couponCode());
        if (coupon.isEmpty()) {
            return new Refused(RejectionReason.INVALID_COUPON);
        }

        List<OrderLine> lines = request.items().stream()
                .map(item -> new OrderLine(item.sku(), item.quantity(), new Money(item.unitPrice())))
                .toList();
        Money subtotal = OrderPricing.subtotalOf(lines);
        if (!coupon.get().appliesTo(subtotal)) {
            return new Refused(RejectionReason.INVALID_COUPON);
        }

        OrderPricing pricing = OrderPricing.calculate(subtotal, country.get(), coupon.get());
        return new Accepted(request.customerId(), request.email(), lines, pricing);
    }
}
