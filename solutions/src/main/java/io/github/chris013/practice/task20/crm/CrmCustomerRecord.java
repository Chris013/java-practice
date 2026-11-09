package io.github.chris013.practice.task20.crm;

/**
 * One customer row, exactly as the 25-year-old CRM sends it. Generated code: never change it, and never let
 * it leave the anti-corruption layer. Every value is text, because that is what the CRM stores.
 *
 * <p>Field meanings are in the task's mapping table (see {@code tasks/20-anti-corruption-layer.md}).
 */
public final class CrmCustomerRecord {

    public String KUNDNR;
    public String TYP;
    public String NAME1;
    public String NAME2;
    public String STRASSE;
    public String PLZ;
    public String ORT;
    public String LAND;
    public String STATUS;
    public String GEBDAT;
    public String EMAIL;
    public String KREDITLIMIT;
}
