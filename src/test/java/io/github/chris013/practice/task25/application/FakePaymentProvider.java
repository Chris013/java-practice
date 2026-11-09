package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.CustomerId;
import io.github.chris013.practice.task25.domain.InvoiceId;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Records every charge. Accepts them all, unless the test calls {@link #declineEverything()}. */
final class FakePaymentProvider implements PaymentProvider {

    record Charge(CustomerId customerId, BigDecimal amount, InvoiceId reference) {}

    private final List<Charge> charges = new ArrayList<>();
    private boolean declining;

    void declineEverything() {
        declining = true;
    }

    @Override
    public PaymentResult charge(CustomerId customerId, BigDecimal amount, InvoiceId reference) {
        charges.add(new Charge(customerId, amount, reference));
        return declining
                ? new PaymentResult.Declined("card declined")
                : new PaymentResult.Paid("tx-" + charges.size());
    }

    List<Charge> charges() {
        return List.copyOf(charges);
    }
}
