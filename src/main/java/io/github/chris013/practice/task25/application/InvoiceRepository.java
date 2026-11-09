package io.github.chris013.practice.task25.application;

import io.github.chris013.practice.task25.domain.Invoice;
import io.github.chris013.practice.task25.domain.SubscriptionId;
import java.time.LocalDate;
import java.util.List;

/** Given. Port: where invoices are stored. */
public interface InvoiceRepository {

    void save(Invoice invoice);

    /**
     * True if an invoice for this subscription was issued on {@code periodStart}. A renewal invoice is issued on
     * the first day of the period it bills, so this is how the nightly job recognises one it already created.
     */
    boolean existsFor(SubscriptionId subscriptionId, LocalDate periodStart);

    List<Invoice> findBySubscription(SubscriptionId subscriptionId);
}
