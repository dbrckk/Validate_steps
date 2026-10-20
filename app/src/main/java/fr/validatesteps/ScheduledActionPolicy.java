package fr.validatesteps;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Guards notification actions triggered by a dated Android alarm or watchdog.
 *
 * An Android job can arrive hours or days late due to Doze or OEM battery
 * policies. A *stale* trigger must not cause a reward action for another day,
 * even if the current local time happens to be within today's claim window.
 *
 * This does not affect a direct user-initiated action or notifications posted
 * naturally by an enabled provider listener.
 */
public final class ScheduledActionPolicy {
    private ScheduledActionPolicy() {}

    public static boolean canInspect(LocalDate scheduledDay, ZonedDateTime now,
            boolean nightlyEnabled) {
        return nightlyEnabled && scheduledDay != null && now != null
            && scheduledDay.equals(now.toLocalDate())
            && RetryPolicy.canRecover(now);
    }
}
