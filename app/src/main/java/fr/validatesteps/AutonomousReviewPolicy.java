package fr.validatesteps;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Optional one-per-night informational reminder, never a claim attempt.
 * Run only from the late scheduled watchdog before the 23:40 cutoff.
 */
public final class AutonomousReviewPolicy {
    private AutonomousReviewPolicy() {}

    public static boolean shouldNotify(LocalDate scheduledDay, LocalDate today,
            LocalTime now, boolean nightlyEnabled, boolean optedIn,
            boolean alreadyNotified, int unverifiedInstalledProviders) {
        if (scheduledDay == null || today == null || now == null) return false;
        return scheduledDay.equals(today) && nightlyEnabled && optedIn
            && !alreadyNotified && unverifiedInstalledProviders > 0
            && !now.isBefore(LocalTime.of(23, 15))
            && now.isBefore(LocalTime.of(23, 40));
    }
}
