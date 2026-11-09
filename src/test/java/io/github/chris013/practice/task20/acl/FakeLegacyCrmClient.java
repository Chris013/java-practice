package io.github.chris013.practice.task20.acl;

import io.github.chris013.practice.task20.crm.CrmCustomerRecord;
import io.github.chris013.practice.task20.crm.CrmException;
import io.github.chris013.practice.task20.crm.LegacyCrmClient;
import java.util.HashMap;
import java.util.Map;

/** A hand-written fake CRM: holds some records, can be told to fail, and remembers what it was asked. */
final class FakeLegacyCrmClient implements LegacyCrmClient {

    private final Map<String, CrmCustomerRecord> records = new HashMap<>();
    private CrmException failure;
    private String lastKundnr;

    FakeLegacyCrmClient with(CrmCustomerRecord record) {
        records.put(record.KUNDNR, record);
        return this;
    }

    FakeLegacyCrmClient failsWith(String code) {
        failure = new CrmException(code, "CRM said " + code);
        return this;
    }

    String lastKundnr() {
        return lastKundnr;
    }

    @Override
    public CrmCustomerRecord fetch(String kundnr) throws CrmException {
        lastKundnr = kundnr;
        if (failure != null) {
            throw failure;
        }
        return records.get(kundnr);
    }
}
