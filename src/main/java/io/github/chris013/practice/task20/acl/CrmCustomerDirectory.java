package io.github.chris013.practice.task20.acl;

import io.github.chris013.practice.task20.crm.LegacyCrmClient;
import io.github.chris013.practice.task20.domain.Customer;
import io.github.chris013.practice.task20.domain.CustomerDirectory;
import io.github.chris013.practice.task20.domain.CustomerId;
import java.util.Optional;

/**
 * The anti-corruption layer's only public class: it answers {@link CustomerDirectory} with domain customers
 * and keeps every CRM detail (field names, codes, formats, exceptions) behind it.
 *
 * <p>Task 20: read {@code tasks/20-anti-corruption-layer.md} first.
 */
public final class CrmCustomerDirectory implements CustomerDirectory {

    /**
     * @param crm the generated CRM client; must not be {@code null}
     */
    public CrmCustomerDirectory(LegacyCrmClient crm) {
        throw new UnsupportedOperationException("TODO: Task 20");
    }

    /**
     * Looks the customer up in the CRM and translates the record.
     *
     * <p>A {@code null} record from the CRM means "not found" and gives an empty Optional. Any
     * {@link io.github.chris013.practice.task20.crm.CrmException} becomes a
     * {@link io.github.chris013.practice.task20.domain.CustomerDirectoryUnavailableException} that keeps the
     * original exception as its cause.
     */
    @Override
    public Optional<Customer> findById(CustomerId id) {
        throw new UnsupportedOperationException("TODO: Task 20");
    }
}
