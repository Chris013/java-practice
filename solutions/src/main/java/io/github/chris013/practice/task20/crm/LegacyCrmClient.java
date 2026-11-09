package io.github.chris013.practice.task20.crm;

/** The generated client for the CRM. Given: we use it, we do not change it. */
public interface LegacyCrmClient {

    /**
     * Fetches one customer by its CRM number.
     *
     * @return the record, or {@code null} when the CRM has no customer with this number
     * @throws CrmException when the CRM cannot be reached or refuses the request
     */
    CrmCustomerRecord fetch(String kundnr) throws CrmException;
}
