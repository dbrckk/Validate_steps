package fr.validatesteps;

/**
 * Whether an explicit user-granted Health Connect permission merits one
 * same-evening retry after an earlier permission failure.
 *
 * RunStore's normal 22h retries deliberately stop after a permission error.
 * A later grant is new information and can safely justify a single retry.
 */
public final class HealthPermissionRecoveryPolicy {
    private HealthPermissionRecoveryPolicy() {}

    public static boolean shouldRetry(boolean nightlyEnabled,
            String previousStatus, boolean permissionsGranted,
            boolean withinNightlyWindow, boolean alreadyQueuedToday) {
        return nightlyEnabled && "permission_missing".equals(previousStatus)
            && permissionsGranted && withinNightlyWindow && !alreadyQueuedToday;
    }
}
