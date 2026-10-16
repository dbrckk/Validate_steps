package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public final class CatchupPolicyTest {
    private final ZoneId zone = ZoneId.of("Europe/Paris");
    private ZonedDateTime at(int hour, int minute) {
        return ZonedDateTime.of(2026, 10, 16, hour, minute, 0, 0, zone);
    }

    @Test public void retriesLostAlarmAfter22() {
        assertTrue(CatchupPolicy.shouldAttempt(at(22, 25), true, false, false));
    }

    @Test public void cannotRunBefore22OrAfterCutoff() {
        assertFalse(CatchupPolicy.shouldAttempt(at(21, 59), true, false, false));
        assertFalse(CatchupPolicy.shouldAttempt(at(23, 40), true, false, false));
    }

    @Test public void mustNotDoubleCountSuccessfulDay() {
        assertFalse(CatchupPolicy.shouldAttempt(at(22, 30), true, true, false));
    }

    @Test public void noWakeupsForDisabledApplication() {
        assertFalse(CatchupPolicy.shouldAttempt(at(22, 30), false, false, false));
    }

    @Test public void doNotRetryPermanentPermissionFailure() {
        assertFalse(CatchupPolicy.shouldAttempt(at(22, 30), true, false, true));
    }

    @Test public void worksAfterDaylightSavingsChange() {
        ZonedDateTime date = ZonedDateTime.of(2026, 10, 25, 22, 30, 0, 0, zone);
        assertTrue(CatchupPolicy.shouldAttempt(date, true, false, false));
    }
}
