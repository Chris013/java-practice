package io.github.chris013.practice.task24;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** A fake payment provider that records every attempt and can decline the next few. */
final class FakePayments implements PaymentGateway {

    record Charge(String customerId, BigDecimal amount) {}

    private final List<Charge> charges = new ArrayList<>();
    private int declinesLeft;

    FakePayments declineNextCharges(int times) {
        declinesLeft = times;
        return this;
    }

    List<Charge> charges() {
        return List.copyOf(charges);
    }

    @Override
    public String charge(String customerId, BigDecimal amount) {
        charges.add(new Charge(customerId, amount));
        if (declinesLeft > 0) {
            declinesLeft--;
            throw new PaymentDeclinedException("card declined");
        }
        return "TX-" + charges.size();
    }
}
