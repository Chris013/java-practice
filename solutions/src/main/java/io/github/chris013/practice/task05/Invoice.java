package io.github.chris013.practice.task05;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** A calculated invoice: everything a renderer needs, and nothing it would have to compute. */
record Invoice(Member member, YearMonth month, List<Line> lines, int loyaltyPoints) {

    Invoice {
        lines = List.copyOf(lines);
    }

    BigDecimal total() {
        return lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    String title() {
        return "Invoice for %s (%s) - %s".formatted(member.name(), member.membershipType(), month);
    }

    record Line(String item, int quantity, BigDecimal amount) {}
}
