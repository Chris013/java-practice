package io.github.chris013.practice.task25.adapter.inmemory;

import io.github.chris013.practice.task25.application.InvoiceIds;
import io.github.chris013.practice.task25.domain.InvoiceId;

/**
 * Produces INV-0001, INV-0002, and so on.
 *
 * <p>Task 25: read {@code tasks/25-capstone-subscription-billing.md} first.
 */
public final class SequentialInvoiceIds implements InvoiceIds {

    @Override
    public InvoiceId next() {
        throw new UnsupportedOperationException("TODO: Task 25");
    }
}
