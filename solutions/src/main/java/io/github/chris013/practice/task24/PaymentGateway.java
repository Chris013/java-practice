package io.github.chris013.practice.task24;

import java.math.BigDecimal;

/** Port: the payment provider. Returns a transaction id, or throws {@link PaymentDeclinedException}. */
public interface PaymentGateway {

    String charge(String customerId, BigDecimal amount);
}
