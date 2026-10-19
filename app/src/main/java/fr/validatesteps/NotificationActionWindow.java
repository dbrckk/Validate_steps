package fr.validatesteps;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/** Deterministic eligibility for user-tapped nightly notification actions. */
public final class NotificationActionWindow {
    private static final long TWO_HOURS = 120 * 60 * 1000L;

    private NotificationActionWindow() {}

    public static boolean canStart(LocalDate issuedDay, ZonedDateTime now,
            long ageMillis) {
        return issuedDay != null && issuedDay.equals(now.toLocalDate())
            && ageMillis >= 0 && ageMillis < TWO_HOURS
            && RetryPolicy.canRecover(now);
    }
}
