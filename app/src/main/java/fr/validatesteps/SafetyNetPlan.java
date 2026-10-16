package fr.validatesteps;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Two fallback instants for the local day of a scheduled 22:00 audit.
 * Calculations use instants to handle DST jumps and time-zone offsets safely.
 */
public final class SafetyNetPlan {
    private SafetyNetPlan() {}

    public static ZonedDateTime early(ZonedDateTime daily22) {
        return daily22.plusMinutes(20);
    }

    public static ZonedDateTime late(ZonedDateTime daily22) {
        return daily22.plusMinutes(75);
    }

    public static long delayMillis(ZonedDateTime now, ZonedDateTime target) {
        return Math.max(0L, Duration.between(now.toInstant(),
            target.toInstant()).toMillis());
    }
}
