package io.github.chris013.practice.task25.adapter.inmemory;

import io.github.chris013.practice.task25.application.InvoiceRepository;
import io.github.chris013.practice.task25.domain.Invoice;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.List;

/**
 * Keeps invoices in a list, in the order they were saved. For tests and local runs.
 *
 * <p>Task 25: read {@code tasks/25-capstone-subscription-billing.md} first.
 */
public final class InMemoryInvoices implements InvoiceRepository {

    @Override
    public void save(Invoice invoice) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    @Override
    public boolean existsFor(SubscriptionId subscriptionId, LocalDate periodStart) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }

    @Override
    public List<Invoice> findBySubscription(SubscriptionId subscriptionId) {
        throw new UnsupportedOperationException("TODO: Task 25");
    }
}
