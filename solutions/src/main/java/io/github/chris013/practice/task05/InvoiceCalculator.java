package io.github.chris013.practice.task05;

import java.time.YearMonth;
import java.util.List;

/** Phase 1: turns visits into an {@link Invoice}. Knows the business rules, knows nothing about output formats. */
final class InvoiceCalculator {

    private static final int VISITS_NEEDED_FOR_REGULARS_BONUS = 11;
    private static final int REGULARS_BONUS_POINTS = 5;

    Invoice calculate(Member member, YearMonth month, List<Visit> visits) {
        MembershipType membership = MembershipType.fromCode(member.membershipType());
        MonthlyUsage usage = MonthlyUsage.of(month, visits);

        List<Invoice.Line> lines = List.of(
                new Invoice.Line("Membership fee", 1, membership.monthlyFee()),
                new Invoice.Line(membership.classesItemName(), usage.classes(), membership.chargeForClasses(usage.classes())),
                new Invoice.Line("Sauna", usage.saunaVisits(), membership.chargeForSaunaVisits(usage.saunaVisits())));

        return new Invoice(member, month, lines, loyaltyPoints(membership, usage));
    }

    private static int loyaltyPoints(MembershipType membership, MonthlyUsage usage) {
        int regularsBonus = usage.totalVisits() >= VISITS_NEEDED_FOR_REGULARS_BONUS ? REGULARS_BONUS_POINTS : 0;
        return membership.pointsForClasses(usage.classes()) + regularsBonus;
    }
}
