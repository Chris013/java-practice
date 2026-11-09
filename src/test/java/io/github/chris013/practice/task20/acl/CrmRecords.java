package io.github.chris013.practice.task20.acl;

import io.github.chris013.practice.task20.crm.CrmCustomerRecord;

/** Builds valid legacy records. Override only the fields a test is about, with {@link #with(String, String)}. */
final class CrmRecords {

    private final CrmCustomerRecord record = new CrmCustomerRecord();

    private CrmRecords() {
    }

    /** A valid private customer: Erika Mustermann, Berlin, active, born 31.12.1980. */
    static CrmRecords aPrivateCustomerRecord() {
        return new CrmRecords()
                .with("KUNDNR", "4711")
                .with("TYP", "P")
                .with("NAME1", "Mustermann")
                .with("NAME2", "Erika")
                .with("STRASSE", "Musterstrasse 1")
                .with("PLZ", "10115")
                .with("ORT", "Berlin")
                .with("LAND", "DE")
                .with("STATUS", "A")
                .with("GEBDAT", "31.12.1980")
                .with("EMAIL", "erika@example.de")
                .with("KREDITLIMIT", "1.234,56");
    }

    /** A valid business customer: Muster GmbH, contact Max Muster, Berlin, active. */
    static CrmRecords aBusinessCustomerRecord() {
        return new CrmRecords()
                .with("KUNDNR", "4711")
                .with("TYP", "F")
                .with("NAME1", "Muster GmbH")
                .with("NAME2", "Max Muster")
                .with("STRASSE", "Hauptstrasse 7")
                .with("PLZ", "10115")
                .with("ORT", "Berlin")
                .with("LAND", "DE")
                .with("STATUS", "A")
                .with("GEBDAT", "")
                .with("EMAIL", "info@muster.example")
                .with("KREDITLIMIT", "50.000,00");
    }

    CrmRecords with(String field, String value) {
        switch (field) {
            case "KUNDNR" -> record.KUNDNR = value;
            case "TYP" -> record.TYP = value;
            case "NAME1" -> record.NAME1 = value;
            case "NAME2" -> record.NAME2 = value;
            case "STRASSE" -> record.STRASSE = value;
            case "PLZ" -> record.PLZ = value;
            case "ORT" -> record.ORT = value;
            case "LAND" -> record.LAND = value;
            case "STATUS" -> record.STATUS = value;
            case "GEBDAT" -> record.GEBDAT = value;
            case "EMAIL" -> record.EMAIL = value;
            case "KREDITLIMIT" -> record.KREDITLIMIT = value;
            default -> throw new IllegalArgumentException("not a CRM field: " + field);
        }
        return this;
    }

    CrmCustomerRecord build() {
        return record;
    }
}
