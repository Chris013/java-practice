package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.CustomerId;
import io.github.chris013.practice.task25.domain.InvoiceId;
import java.math.BigDecimal;

/** Given. Port: the payment gateway. A declined card is a normal result, not an exception. */
public interface PaymentProvider {

    PaymentResult charge(CustomerId customerId, BigDecimal amount, InvoiceId reference);
}
