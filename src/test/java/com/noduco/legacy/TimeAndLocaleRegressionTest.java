package com.noduco.legacy;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class TimeAndLocaleRegressionTest {
    // Java 8 printed "01 Sep 2026", JDK 16+ prints "01 Sept 2026" (CLDR 38).
    // Evidence: migration/m5-p5-sept-probe.txt. Pinned so a JDK or CLDR update cannot change it silently.
    @Test
    public void ukShortMonthForSeptemberIsSept() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.UK);
        assertEquals("01 Sept 2026", f.format(LocalDate.of(2026, 9, 1)));
    }

    // Formats meant for machines must not depend on locale data at all
    @Test
    public void isoDateDoesNotDependOnLocale() {
        assertEquals("2026-09-01", DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.of(2026, 9, 1)));
    }

    // CI runners use UTC, this laptop uses Asia/Kolkata: always name the zone
    @Test
    public void explicitZoneGivesTheSameTextOnEveryMachine() {
        Instant instant = Instant.parse("2026-11-12T20:18:00Z");
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm xxx")
                .withZone(ZoneId.of("Asia/Kolkata"));
        assertEquals("2026-11-13 01:48 +05:30", f.format(instant));
    }
}
