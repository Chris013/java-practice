package io.github.chris013.practice.task05;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * Creates the monthly invoice for a gym member.
 *
 * <p>Task 05: read {@code tasks/05-gym-invoice-refactoring.md} first. Refactor {@link #statement} while
 * {@code InvoiceStatementCharacterizationTest} stays green, then implement {@link #htmlStatement}.
 */
public class InvoiceGenerator {

    public String statement(Member member, YearMonth month, List<Visit> visits) {
        double total = 0;
        int points = 0;
        int classes = 0;
        int sauna = 0;
        int count = 0;
        String result = "Invoice for " + member.name() + " (" + member.membershipType() + ") - " + month + "\n";

        // fee
        double fee;
        if (member.membershipType().equals("PREMIUM")) {
            fee = 59;
        } else if (member.membershipType().equals("STUDENT")) {
            fee = 19;
        } else {
            fee = 29;
        }
        total += fee;
        result += "  Membership fee x1: " + String.format(Locale.ROOT, "%.2f", fee) + "\n";

        for (Visit v : visits) {
            if (!YearMonth.from(v.date()).equals(month)) {
                continue;
            }
            count++;
            if (v.kind().equals("CLASS")) {
                classes++;
                points += member.membershipType().equals("PREMIUM") ? 2 : 1;
            } else if (v.kind().equals("SAUNA")) {
                sauna++;
            }
        }

        // classes
        double classAmount;
        switch (member.membershipType()) {
            case "PREMIUM":
                classAmount = classes > 8 ? (classes - 8) * 4.0 : 0;
                result += "  Classes (8 included) x" + classes + ": "
                        + String.format(Locale.ROOT, "%.2f", classAmount) + "\n";
                break;
            case "STUDENT":
                classAmount = classes * 6.0;
                result += "  Classes x" + classes + ": " + String.format(Locale.ROOT, "%.2f", classAmount) + "\n";
                break;
            default:
                classAmount = classes * 8.0;
                result += "  Classes x" + classes + ": " + String.format(Locale.ROOT, "%.2f", classAmount) + "\n";
        }
        total += classAmount;

        double saunaAmount = member.membershipType().equals("PREMIUM") ? 0 : sauna * 5.0;
        total += saunaAmount;
        result += "  Sauna x" + sauna + ": " + String.format(Locale.ROOT, "%.2f", saunaAmount) + "\n";

        // bonus for regulars
        if (count > 10) {
            points += 5;
        }

        result += "Total: " + String.format(Locale.ROOT, "%.2f", total) + "\n";
        result += "Loyalty points earned: " + points + "\n";
        return result;
    }

    /** New: the same invoice as HTML for the member portal. */
    public String htmlStatement(Member member, YearMonth month, List<Visit> visits) {
        throw new UnsupportedOperationException("TODO: Task 05, part 2");
    }
}
