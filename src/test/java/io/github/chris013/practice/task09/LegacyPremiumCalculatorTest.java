package io.github.chris013.practice.task09;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

// Written in 2019 by someone who has since left. Nobody knows what test1 was meant to cover.
class LegacyPremiumCalculatorTest {

    PremiumCalculator calc = new PremiumCalculator();

    @Test
    void test1() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        Policyholder p = new Policyholder(LocalDate.of(1980, 1, 1), LocalDate.of(2000, 1, 1), 0, "10115");
        Vehicle v = new Vehicle("VW Golf", 110, 25000, 2022);
        assertEquals(new BigDecimal("300.00"), calc.annualPremium(p, v, Coverage.LIABILITY, start));
        assertEquals(new BigDecimal("420.00"), calc.annualPremium(p, v, Coverage.PARTIAL, start));
        assertEquals(new BigDecimal("600.00"), calc.annualPremium(p, v, Coverage.FULL, start));

        // young driver, one claim, two years licensed
        Policyholder p2 = new Policyholder(LocalDate.of(2005, 1, 1), LocalDate.of(2024, 1, 1), 1, "10115");
        assertEquals(new BigDecimal("702.00"), calc.annualPremium(p2, v, Coverage.LIABILITY, start));
    }

    @Test
    void testPremium() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        Policyholder p = new Policyholder(LocalDate.of(1980, 1, 1), LocalDate.of(2000, 1, 1), 0, "10115");
        Vehicle fast = new Vehicle("BMW M3", 300, 60000, 2020);
        assertEquals(new BigDecimal("420.00"), calc.annualPremium(p, fast, Coverage.LIABILITY, start));
        assertEquals(new BigDecimal("840.00"), calc.annualPremium(p, fast, Coverage.FULL, start));

        // senior driver
        Policyholder old = new Policyholder(LocalDate.of(1950, 1, 1), LocalDate.of(1970, 1, 1), 0, "80331");
        Vehicle golf = new Vehicle("VW Golf", 110, 25000, 2022);
        assertEquals(new BigDecimal("360.00"), calc.annualPremium(old, golf, Coverage.LIABILITY, start));
    }

    @Test
    void testUninsurable() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        Vehicle v = new Vehicle("VW Golf", 110, 25000, 2022);
        Policyholder kid = new Policyholder(LocalDate.of(2010, 1, 1), LocalDate.of(2020, 1, 1), 0, "10115");
        assertThrows(UninsurableException.class, () -> calc.annualPremium(kid, v, Coverage.LIABILITY, start));
        Policyholder crashy = new Policyholder(LocalDate.of(1980, 1, 1), LocalDate.of(2000, 1, 1), 3, "10115");
        assertThrows(UninsurableException.class, () -> calc.annualPremium(crashy, v, Coverage.LIABILITY, start));
        Vehicle expensive = new Vehicle("Porsche", 150, 120000, 2024);
        Policyholder p = new Policyholder(LocalDate.of(1980, 1, 1), LocalDate.of(2000, 1, 1), 0, "10115");
        assertThrows(UninsurableException.class, () -> calc.annualPremium(p, expensive, Coverage.FULL, start));
    }
}
