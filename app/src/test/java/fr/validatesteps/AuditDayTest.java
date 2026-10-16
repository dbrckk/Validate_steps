package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalDate;
import org.junit.Test;

public final class AuditDayTest {
    private static final LocalDate OCT17 = LocalDate.of(2026, 10, 17);

    @Test public void delayed22AlarmStillReadsYesterday() {
        assertEquals(LocalDate.of(2026, 10, 16),
            AuditDay.resolve("2026-10-16", OCT17));
    }
    @Test public void todays22AlarmReadsToday() {
        assertEquals(OCT17, AuditDay.resolve("2026-10-17", OCT17));
    }
    @Test public void doesNotReadAnArbitraryOldDay() {
        assertNull(AuditDay.resolve("2026-10-15", OCT17));
    }
    @Test public void doesNotAcceptFutureAlarms() {
        assertNull(AuditDay.resolve("2026-10-18", OCT17));
    }
    @Test public void invalidExtraDoesNotSilentlyUseTodaysSteps() {
        assertNull(AuditDay.resolve(null, OCT17));
        assertNull(AuditDay.resolve("broken", OCT17));
    }
    @Test public void delayedOlderWorkCannotReplaceTodaysResult() {
        assertTrue(AuditDay.isOlderThanRecorded(
            LocalDate.of(2026, 10, 16), "2026-10-17"));
        assertFalse(AuditDay.isOlderThanRecorded(OCT17, "2026-10-16"));
    }
}
