package fr.validatesteps;

import java.time.LocalDate;

/**
 * A canceled PendingIntent definitely did not launch, so it can be retried at
 * most twice more that day. Sent or ambiguous attempts are never repeated.
 */
public final class ClaimRetryPolicy {
    private static final int MAX_ATTEMPTS = 3;
    private ClaimRetryPolicy() {}

    public static boolean allowed(LocalDate day, String lastDay,
            String status, int attempts) {
        if (day == null) return false;
        if (!day.toString().equals(lastDay)) return true;
        return "action_annulee".equals(status) && attempts < MAX_ATTEMPTS;
    }
}
