package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Private bounded timeline of Android scheduling, worker and action events.
 * Codes are compile-time constants: no health counts, notification messages,
 * provider account identifiers, URLs or credentials are ever logged.
 */
public final class NightTimeline {
    public enum Event {
        ALARM_RECEIVED, ALARM_REJECTED, RECOVERY_RECEIVED,
        WATCHDOG_RECEIVED, WATCHDOG_IGNORED, WATCHDOG_ENQUEUED,
        SCHEDULED_ACTION_IGNORED,
        REARMED_AFTER_BOOT, ALARM_SCHEDULE_FAILED, NOTIFICATION_BLOCKED,
        AUDIT_ENQUEUED, AUDIT_STARTED,
        AUDIT_OK, AUDIT_BLOCKED, AUDIT_RETRY_NEEDED, AUDIT_OLD_DAY,
        AUDIT_PERMISSION_RECOVERED,
        CLAIM_ACTION_DISPATCHED, CLAIM_ACTION_CANCELED, CLAIM_ACTION_ERROR,
        MANUAL_DIRECT_REQUESTED,
        GUIDED_STARTED, GUIDED_CLICK_ATTEMPTED, GUIDED_CLICK_REFUSED,
        GUIDED_BUTTON_MISSING, GUIDED_APP_ABSENT, GUIDED_OPEN_BLOCKED,
        GUIDED_FINISHED, GUIDED_CANCELED,
        USER_CREDIT_REPORTED_RECEIVED, USER_CREDIT_REPORTED_MISSING,
        USER_CREDIT_REPORT_CLEARED
    }

    private static final Object LOCK = new Object();
    private static final String KEY = "events";
    private final SharedPreferences prefs;

    public NightTimeline(Context context) {
        prefs = context.getSharedPreferences("night_timeline", Context.MODE_PRIVATE);
    }

    public void add(Event event, LocalDate auditDate) {
        appendLine(event, auditDate, "");
    }

    /** Include only a fixed, allowlisted app identifier; no account data. */
    public void addProvider(Event event, LocalDate auditDate, String packageName) {
        String provider = ProviderEventCode.code(packageName);
        if (provider.isEmpty()) return;
        appendLine(event, auditDate, provider);
    }

    private void appendLine(Event event, LocalDate auditDate, String provider) {
        String line = OffsetDateTime.now().toString() + " | " + event.name()
            + " | " + (auditDate == null ? "-" : auditDate.toString())
            + (provider.isEmpty() ? "" : " | " + provider);
        synchronized (LOCK) {
            String old = prefs.getString(KEY, "");
            prefs.edit().putString(KEY, TimelinePolicy.append(old, line)).commit();
        }
    }

    public String recent() {
        return TimelinePolicy.latest(prefs.getString(KEY, ""), 18);
    }

    public void clear() {
        synchronized (LOCK) {
            prefs.edit().remove(KEY).commit();
        }
    }
}
