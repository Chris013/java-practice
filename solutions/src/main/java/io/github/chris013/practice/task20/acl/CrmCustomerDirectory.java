package io.github.chris013.practice.task20.acl;

import io.github.chris013.practice.task20.crm.CrmCustomerRecord;
import io.github.chris013.practice.task20.crm.CrmException;
import io.github.chris013.practice.task20.crm.LegacyCrmClient;
import io.github.chris013.practice.task20.domain.Customer;
import io.github.chris013.practice.task20.domain.CustomerDirectory;
import io.github.chris013.practice.task20.domain.CustomerDirectoryUnavailableException;
import io.github.chris013.practice.task20.domain.CustomerId;
import java.util.Objects;
import java.util.Optional;

/**
 * The anti-corruption layer's only public class: it answers {@link CustomerDirectory} with domain customers
 * and keeps every CRM detail (field names, codes, formats, exceptions) behind it.
 *
 * <p>Task 20: read {@code tasks/20-anti-corruption-layer.md} first.
 */
public final class CrmCustomerDirectory implements CustomerDirectory {

    private final LegacyCrmClient crm;
    private final CrmCustomerTranslator translator = new CrmCustomerTranslator();

    /**
     * @param crm the generated CRM client; must not be {@code null}
     */
    public CrmCustomerDirectory(LegacyCrmClient crm) {
        this.crm = Objects.requireNonNull(crm, "crm");
    }

    /**
     * Looks the customer up in the CRM and translates the record.
     *
     * <p>A {@code null} record from the CRM means "not found" and gives an empty Optional. Any
     * {@link CrmException} becomes a {@link CustomerDirectoryUnavailableException} that keeps the original
     * exception as its cause, so the stack trace and the CRM error code are not lost.
     */
    @Override
    public Optional<Customer> findById(CustomerId id) {
        Objects.requireNonNull(id, "id");
        CrmCustomerRecord record = fetch(id);
        if (record == null) {
            return Optional.empty();
        }
        return Optional.of(translator.toDomain(id, record));
    }

    private CrmCustomerRecord fetch(CustomerId id) {
        try {
            return crm.fetch(id.value());
        } catch (CrmException e) {
            // Every CRM code is treated as "try again later" here. AUTH could be argued differently:
            // retrying a wrong password will never help, and an alert might be the better answer.
            throw new CustomerDirectoryUnavailableException(
                    "the CRM could not load customer %s (%s)".formatted(id.value(), e.code()), e);
        }
    }
}
