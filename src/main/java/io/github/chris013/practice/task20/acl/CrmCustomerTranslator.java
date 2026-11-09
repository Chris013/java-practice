package io.github.chris013.practice.task20.acl;

import io.github.chris013.practice.task20.crm.CrmCustomerRecord;
import io.github.chris013.practice.task20.domain.Customer;
import io.github.chris013.practice.task20.domain.CustomerId;

/**
 * Turns one legacy CRM record into a domain {@link Customer}. Package-private on purpose: only the
 * directory may use it, so nothing outside the layer can call it with a half-trusted record.
 *
 * <p>Task 20: read {@code tasks/20-anti-corruption-layer.md} first.
 */
final class CrmCustomerTranslator {

    /**
     * Translates {@code record} for the customer {@code id} that was asked for.
     *
     * <p>The record's own KUNDNR is not used: we asked the CRM for {@code id}, so {@code id} is the
     * trusted identity of the customer.
     *
     * @throws io.github.chris013.practice.task20.domain.CorruptCustomerDataException for any field that
     *         cannot be turned into a valid value
     */
    Customer toDomain(CustomerId id, CrmCustomerRecord record) {
        throw new UnsupportedOperationException("TODO: Task 20");
    }
}
