package fr.validatesteps;

import java.time.ZonedDateTime;

/** Pure time rules: retries never spill into tomorrow's step counter. */
public final class RetryPolicy {
    private RetryPolicy() {}

    /** Initial/late-boot recovery is allowed until 23:40 local time. */
    public static boolean canRecover(ZonedDateTime now) {
        ZonedDateTime start = now.withHour(22).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime cutoff = now.withHour(23).withMinute(40).withSecond(0).withNano(0);
        return !now.isBefore(start) && now.isBefore(cutoff);
    }

    public static ZonedDateTime recovery(ZonedDateTime now) {
        return canRecover(now) ? now.plusMinutes(2) : null;
    }

    /** At most one retry alarm is scheduled; repeat only after a failed read. */
    public static ZonedDateTime retry(ZonedDateTime now) {
        if (!canRecover(now)) return null;
        ZonedDateTime next = now.plusMinutes(12);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.getZone());
        return next.isBefore(midnight.minusMinutes(1)) ? next : null;
    }
}
