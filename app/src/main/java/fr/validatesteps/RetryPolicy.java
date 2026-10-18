package fr.validatesteps;

import java.time.ZonedDateTime;

/** Time-bounded retries: never schedule work outside the nightly recovery window. */
public final class RetryPolicy {
    private RetryPolicy() {}

    /** Initial/late-boot recovery is allowed from 22:00 until 23:40 local time. */
    public static boolean canRecover(ZonedDateTime now) {
        ZonedDateTime start = now.withHour(22).withMinute(0).withSecond(0).withNano(0);
        return !now.isBefore(start) && now.isBefore(cutoff(now));
    }

    /** The exclusive end of the recovery window on the same local day. */
    private static ZonedDateTime cutoff(ZonedDateTime time) {
        return time.withHour(23).withMinute(40).withSecond(0).withNano(0);
    }

    /** Initial recovery after a delayed reboot; never schedule past 23:40. */
    public static ZonedDateTime recovery(ZonedDateTime now) {
        if (!canRecover(now)) return null;
        ZonedDateTime planned = now.plusMinutes(2);
        return planned.isBefore(cutoff(now)) ? planned : null;
    }

    /** Retry only after failure; no alarm scheduled past the claim window. */
    public static ZonedDateTime retry(ZonedDateTime now) {
        if (!canRecover(now)) return null;
        ZonedDateTime planned = now.plusMinutes(12);
        return planned.isBefore(cutoff(now)) ? planned : null;
    }
}
