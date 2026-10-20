package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class StepValuePolicyTest {
    @Test public void zeroAndPositiveCountsAreValid() {
        assertTrue(StepValuePolicy.valid(0L));
        assertTrue(StepValuePolicy.valid(37000L));
        assertEquals("steps_read", StepValuePolicy.status(0L));
        assertEquals(37000L, StepValuePolicy.storedCount(37000L, "steps_read"));
    }

    @Test public void negativeAndMissingStepsNeverProduceSuccess() {
        assertFalse(StepValuePolicy.valid(null));
        assertFalse(StepValuePolicy.valid(-1L));
        assertEquals("invalid_steps_data", StepValuePolicy.status(-1L));
        assertEquals("invalid_steps_data",
            StepValuePolicy.storedStatus(null, "steps_read"));
        assertEquals("invalid_steps_data",
            StepValuePolicy.storedStatus(-1L, "steps_read"));
        assertEquals(-1L, StepValuePolicy.storedCount(null, "steps_read"));
        assertEquals(-1L, StepValuePolicy.storedCount(-1L, "steps_read"));
    }

    @Test public void errorsNeverPersistCounts() {
        assertEquals(-1L, StepValuePolicy.storedCount(123L, "permission_missing"));
        assertEquals("permission_missing",
            StepValuePolicy.storedStatus(123L, "permission_missing"));
    }
}
