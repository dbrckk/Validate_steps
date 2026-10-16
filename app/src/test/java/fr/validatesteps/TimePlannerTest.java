package fr.validatesteps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public final class TimePlannerTest {
    private final ZoneId paris = ZoneId.of("Europe/Paris");

    @Test public void before22SchedulesToday() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 16, 20, 30, 0, 0, paris);
        assertEquals("2026-10-16T22:00+02:00[Europe/Paris]",
            TimePlanner.next22(now).toString());
    }

    @Test public void at22SchedulesTomorrow() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 16, 22, 0, 0, 0, paris);
        assertEquals(17, TimePlanner.next22(now).getDayOfMonth());
    }

    @Test public void after22SchedulesTomorrow() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 16, 23, 15, 0, 0, paris);
        assertEquals(17, TimePlanner.next22(now).getDayOfMonth());
    }

    @Test public void daylightSavingTransitionKeepsLocalHour() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 24, 23, 0, 0, 0, paris);
        ZonedDateTime next = TimePlanner.next22(now);
        assertEquals(25, next.getDayOfMonth());
        assertEquals(22, next.getHour());
        assertTrue(next.isAfter(now));
    }
}
