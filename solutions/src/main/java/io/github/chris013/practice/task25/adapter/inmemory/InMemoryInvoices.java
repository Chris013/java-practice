package io.github.chris013.practice.task25.adapter.inmemory;

import io.github.chris013.practice.task25.application.InvoiceRepository;
import io.github.chris013.practice.task25.domain.Invoice;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Keeps invoices in a list, in the order they were saved. For tests and local runs. */
public final class InMemoryInvoices implements InvoiceRepository {

    private final List<Invoice> all = new ArrayList<>();

    @Override
    public void save(Invoice invoice) {
        all.add(Objects.requireNonNull(invoice, "invoice"));
    }

    @Override
    public boolean existsFor(SubscriptionId subscriptionId, LocalDate periodStart) {
        Objects.requireNonNull(subscriptionId, "subscriptionId");
        Objects.requireNonNull(periodStart, "periodStart");
        return all.stream().anyMatch(invoice -> invoice.subscriptionId().equals(subscriptionId)
                && invoice.issuedOn().equals(periodStart));
    }

    @Override
    public List<Invoice> findBySubscription(SubscriptionId subscriptionId) {
        Objects.requireNonNull(subscriptionId, "subscriptionId");
        return all.stream().filter(invoice -> invoice.subscriptionId().equals(subscriptionId)).toList();
    }
}
