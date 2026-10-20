package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public final class ScheduledActionPolicyTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    private ZonedDateTime at(LocalDate day, int hour, int minute) {
        return day.atTime(hour, minute).atZone(PARIS);
    }

    @Test public void acceptsOnlyThisDaysAuthorizedNightWindow() {
        assertTrue(ScheduledActionPolicy.canInspect(DAY, at(DAY, 22, 0), true));
        assertTrue(ScheduledActionPolicy.canInspect(DAY, at(DAY, 23, 39), true));
        assertFalse(ScheduledActionPolicy.canInspect(DAY, at(DAY, 21, 59), true));
        assertFalse(ScheduledActionPolicy.canInspect(DAY, at(DAY, 23, 40), true));
    }

    @Test public void staleWorkerCannotRunTodaysClaimEvenAt22() {
        assertFalse(ScheduledActionPolicy.canInspect(
            DAY.minusDays(1), at(DAY, 22, 20), true));
        assertFalse(ScheduledActionPolicy.canInspect(
            DAY, at(DAY.plusDays(1), 22, 20), true));
    }

    @Test public void tomorrowScheduledTriggerCannotRunEarly() {
        assertFalse(ScheduledActionPolicy.canInspect(
            DAY.plusDays(1), at(DAY, 22, 20), true));
    }

    @Test public void neverRunsWithoutConsentOrValidDate() {
        assertFalse(ScheduledActionPolicy.canInspect(DAY, at(DAY, 22, 10), false));
        assertFalse(ScheduledActionPolicy.canInspect(null, at(DAY, 22, 10), true));
        assertFalse(ScheduledActionPolicy.canInspect(DAY, null, true));
    }

    @Test public void cannotReplayAcrossMidnight() {
        assertFalse(ScheduledActionPolicy.canInspect(
            DAY, at(DAY.plusDays(1), 0, 1), true));
    }
}
