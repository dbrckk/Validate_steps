package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public final class SafetyNetPlanTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    private ZonedDateTime time(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, PARIS);
    }

    @Test public void twoFallbacksStayBeforeDeadline() {
        ZonedDateTime daily = time(2026, 10, 16, 22, 0);
        assertEquals(time(2026, 10, 16, 22, 20), SafetyNetPlan.early(daily));
        assertEquals(time(2026, 10, 16, 23, 15), SafetyNetPlan.late(daily));
        assertTrue(RetryPolicy.canRecover(SafetyNetPlan.early(daily)));
        assertTrue(RetryPolicy.canRecover(SafetyNetPlan.late(daily)));
    }

    @Test public void calculatesAbsoluteDelayNotWallClockSubtraction() {
        ZonedDateTime now = time(2026, 10, 16, 21, 50);
        assertEquals(30 * 60_000L, SafetyNetPlan.delayMillis(now,
            SafetyNetPlan.early(time(2026, 10, 16, 22, 0))));
    }

    @Test public void noNegativeDelayForDelayedScheduling() {
        assertEquals(0L, SafetyNetPlan.delayMillis(
            time(2026, 10, 16, 23, 20),
            time(2026, 10, 16, 22, 20)));
    }

    @Test public void dstSwitchPreservesLocalTime() {
        ZonedDateTime scheduled = time(2026, 10, 25, 22, 0);
        assertEquals(22, SafetyNetPlan.early(scheduled).getHour());
        assertEquals(23, SafetyNetPlan.late(scheduled).getHour());
        assertEquals(20, SafetyNetPlan.early(scheduled).getMinute());
        assertEquals(15, SafetyNetPlan.late(scheduled).getMinute());
    }
}
