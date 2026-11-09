package io.github.chris013.practice.task25.adapter.inmemory;

import io.github.chris013.practice.task25.application.InvoiceIds;
import io.github.chris013.practice.task25.domain.InvoiceId;
import java.util.concurrent.atomic.AtomicInteger;

/** Produces INV-0001, INV-0002, and so on. */
public final class SequentialInvoiceIds implements InvoiceIds {

    private final AtomicInteger last = new AtomicInteger();

    @Override
    public InvoiceId next() {
        return new InvoiceId("INV-%04d".formatted(last.incrementAndGet()));
    }
}
