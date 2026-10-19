package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class HealthPermissionRecoveryPolicyTest {
    @Test public void grantAfterPermissionFailureCanRecoverSameEvening() {
        assertTrue(HealthPermissionRecoveryPolicy.shouldRetry(
            true, "permission_missing", true, true, false));
    }

    @Test public void neverRunsWhenDisabledOrOutsideNightlyWindow() {
        assertFalse(HealthPermissionRecoveryPolicy.shouldRetry(
            false, "permission_missing", true, true, false));
        assertFalse(HealthPermissionRecoveryPolicy.shouldRetry(
            true, "permission_missing", true, false, false));
    }

    @Test public void neverRetriesBeforeThePermissionIsActuallyGranted() {
        assertFalse(HealthPermissionRecoveryPolicy.shouldRetry(
            true, "permission_missing", false, true, false));
    }

    @Test public void oneRecoveryOnlyPerDay() {
        assertFalse(HealthPermissionRecoveryPolicy.shouldRetry(
            true, "permission_missing", true, true, true));
    }

    @Test public void avoidsRepeatingSuccessfulAndUnrelatedFailures() {
        for (String status : new String[] {
                null, "steps_read", "read_timeout", "health_connect_unavailable",
                "background_health_unsupported", "reading"}) {
            assertFalse(HealthPermissionRecoveryPolicy.shouldRetry(
                true, status, true, true, false));
        }
    }
}
