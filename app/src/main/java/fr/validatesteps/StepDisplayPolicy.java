package fr.validatesteps;

import java.time.LocalDate;

/**
 * Only show real, successful measurements from the CURRENT local day.
 *
 * A Health Connect callback can arrive late (after midnight), out of order,
 * without permission, or without records. In all those cases the UI must
 * show a missing value, never yesterday's number under today's date.
 */
public final class StepDisplayPolicy {
    private StepDisplayPolicy() {}

    public static boolean isCurrentReading(LocalDate requestedDay,
            LocalDate currentDay, Long steps, String status) {
        return requestedDay != null && currentDay != null
            && requestedDay.equals(currentDay)
            && "steps_read".equals(status)
            && steps != null && steps >= 0;
    }

    /** Suppress stale asynchronous callbacks from a superseded request. */
    public static boolean isLatestRequest(int callbackId, int latestRequestId) {
        return callbackId == latestRequestId;
    }
}
