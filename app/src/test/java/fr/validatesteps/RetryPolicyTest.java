package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public final class RetryPolicyTest {
    private final ZoneId paris = ZoneId.of("Europe/Paris");
    private ZonedDateTime at(int h, int m) {
        return ZonedDateTime.of(2026, 10, 16, h, m, 0, 0, paris);
    }

    @Test public void morningDoesNotScheduleRecovery() {
        assertNull(RetryPolicy.recovery(at(9, 0)));
        assertNull(RetryPolicy.retry(at(9, 0)));
    }
    @Test public void after22RecoveryOccursWithoutOpeningApp() {
        assertEquals(at(22, 12), RetryPolicy.recovery(at(22, 10)));
    }
    @Test public void failureRetriesInTwelveMinutes() {
        assertEquals(at(22, 27), RetryPolicy.retry(at(22, 15)));
    }
    @Test public void neverSchedulesAfterDeadline() {
        assertNull(RetryPolicy.retry(at(23, 40)));
        assertNull(RetryPolicy.recovery(at(23, 40)));
    }
    @Test public void beforeDeadlineRetryNeverRunsAfterRecoveryWindow() {
        assertEquals(at(23, 39), RetryPolicy.retry(at(23, 27)));
        assertNull(RetryPolicy.retry(at(23, 28)));
        assertNull(RetryPolicy.retry(at(23, 39)));
    }

    @Test public void rebootRecoveryNeverRunsAtOrAfterCutoff() {
        assertEquals(at(23, 39), RetryPolicy.recovery(at(23, 37)));
        assertNull(RetryPolicy.recovery(at(23, 38)));
        assertNull(RetryPolicy.recovery(at(23, 39)));
    }
    @Test public void daylightSavingDayIsValid() {
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 25, 22, 4, 0, 0, paris);
        assertEquals(22, RetryPolicy.retry(now).getHour());
        assertEquals(16, RetryPolicy.retry(now).getMinute());
    }
}
