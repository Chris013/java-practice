package io.github.chris013.practice.task05;

import java.math.BigDecimal;

/**
 * What each membership costs and earns. The three memberships differ only in their numbers, not in their rules,
 * so they are data, not subclasses.
 */
enum MembershipType {
    //      monthly fee         per class           included classes  per sauna visit     points per class
    BASIC(new BigDecimal("29.00"), new BigDecimal("8.00"), 0, new BigDecimal("5.00"), 1),
    PREMIUM(new BigDecimal("59.00"), new BigDecimal("4.00"), 8, new BigDecimal("0.00"), 2),
    STUDENT(new BigDecimal("19.00"), new BigDecimal("6.00"), 0, new BigDecimal("5.00"), 1);

    private final BigDecimal monthlyFee;
    private final BigDecimal pricePerClass;
    private final int includedClasses;
    private final BigDecimal pricePerSaunaVisit;
    private final int pointsPerClass;

    MembershipType(BigDecimal monthlyFee, BigDecimal pricePerClass, int includedClasses,
                   BigDecimal pricePerSaunaVisit, int pointsPerClass) {
        this.monthlyFee = monthlyFee;
        this.pricePerClass = pricePerClass;
        this.includedClasses = includedClasses;
        this.pricePerSaunaVisit = pricePerSaunaVisit;
        this.pointsPerClass = pointsPerClass;
    }

    /** Known quirk kept from the legacy code: unknown membership types are billed like BASIC. */
    static MembershipType fromCode(String code) {
        for (MembershipType type : values()) {
            if (type.name().equals(code)) {
                return type;
            }
        }
        return BASIC;
    }

    BigDecimal monthlyFee() {
        return monthlyFee;
    }

    BigDecimal chargeForClasses(int classes) {
        int billableClasses = Math.max(0, classes - includedClasses);
        return pricePerClass.multiply(BigDecimal.valueOf(billableClasses));
    }

    String classesItemName() {
        return includedClasses == 0 ? "Classes" : "Classes (%d included)".formatted(includedClasses);
    }

    BigDecimal chargeForSaunaVisits(int saunaVisits) {
        return pricePerSaunaVisit.multiply(BigDecimal.valueOf(saunaVisits));
    }

    int pointsForClasses(int classes) {
        return pointsPerClass * classes;
    }
}
