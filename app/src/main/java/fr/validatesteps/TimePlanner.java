package fr.validatesteps;

import java.time.ZonedDateTime;

/** Pure calculation, including past-occurrence rollover. */
public final class TimePlanner {
    private TimePlanner() {}

    public static ZonedDateTime next22(ZonedDateTime now) {
        ZonedDateTime next = now.withHour(22).withMinute(0).withSecond(0).withNano(0);
        return next.isAfter(now) ? next : next.plusDays(1);
    }
}
