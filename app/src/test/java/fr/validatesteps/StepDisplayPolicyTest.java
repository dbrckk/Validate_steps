package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalDate;
import org.junit.Test;

public final class StepDisplayPolicyTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);

    @Test public void acceptsLegitimateZeroAndPositiveSteps() {
        assertTrue(StepDisplayPolicy.isCurrentReading(MONDAY, MONDAY, 0L, "steps_read"));
        assertTrue(StepDisplayPolicy.isCurrentReading(MONDAY, MONDAY, 9852L, "steps_read"));
    }

    @Test public void neverLabelsYesterdaysStepsAsTodays() {
        assertFalse(StepDisplayPolicy.isCurrentReading(
            MONDAY, TUESDAY, 9852L, "steps_read"));
    }

    @Test public void rejectsInvalidOrMissingMeasurements() {
        assertFalse(StepDisplayPolicy.isCurrentReading(MONDAY, MONDAY, null, "steps_read"));
        assertFalse(StepDisplayPolicy.isCurrentReading(MONDAY, MONDAY, -1L, "steps_read"));
        assertFalse(StepDisplayPolicy.isCurrentReading(MONDAY, MONDAY, 100L, "permission_missing"));
        assertFalse(StepDisplayPolicy.isCurrentReading(MONDAY, MONDAY, 100L, "read_timeout"));
        assertFalse(StepDisplayPolicy.isCurrentReading(null, MONDAY, 100L, "steps_read"));
        assertFalse(StepDisplayPolicy.isCurrentReading(MONDAY, null, 100L, "steps_read"));
    }

    @Test public void oldCallbacksCannotOverwriteMoreRecentReads() {
        assertTrue(StepDisplayPolicy.isLatestRequest(4, 4));
        assertFalse(StepDisplayPolicy.isLatestRequest(4, 5));
    }
}
