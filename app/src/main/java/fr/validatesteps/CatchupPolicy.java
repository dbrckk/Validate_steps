package fr.validatesteps;

import java.time.ZonedDateTime;

/** Pure rule for the periodic safety-net, independent of Android scheduling. */
public final class CatchupPolicy {
    private CatchupPolicy() {}

    /**
     * The watchdog is only useful within the same-day recovery window.
     * It must not re-open yesterday's reward deadline or run before 22:00.
     */
    public static boolean shouldAttempt(ZonedDateTime now,
            boolean enabled, boolean alreadyRead, boolean permanentlyUnavailable) {
        return enabled && !alreadyRead && !permanentlyUnavailable
            && RetryPolicy.canRecover(now);
    }
}
