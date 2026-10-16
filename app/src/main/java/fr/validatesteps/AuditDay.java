package fr.validatesteps;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Audit identity is the date of the scheduled alarm, NOT the worker execution
 * date. Android may launch a queued worker after midnight.
 */
public final class AuditDay {
    private AuditDay() {}

    /** Accepts an ISO date at most one day old; excludes stale/future alarms. */
    public static LocalDate resolve(String isoDate, LocalDate today) {
        if (isoDate == null) return null;
        try {
            LocalDate requested = LocalDate.parse(isoDate);
            return requested.isAfter(today) || requested.isBefore(today.minusDays(1))
                ? null : requested;
        } catch (DateTimeParseException invalid) {
            return null;
        }
    }

    public static boolean isOlderThanRecorded(LocalDate requested, String lastDate) {
        if (lastDate == null || lastDate.isEmpty()) return false;
        try { return requested.isBefore(LocalDate.parse(lastDate)); }
        catch (DateTimeParseException invalid) { return false; }
    }
}
