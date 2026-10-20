package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Small offline, immutable-date ledger of last 30 days' safe diagnostic codes.
 * Writes occur when events actually happen, before today's single-value ledgers
 * are overwritten tomorrow. Unknown or arbitrary provider text is never saved.
 */
public final class DailyHistoryStore {
    private static final Object LOCK = new Object();
    private static final String ROOT = "daily_history_v1";
    private final SharedPreferences prefs;

    public DailyHistoryStore(Context context) {
        prefs = context.getApplicationContext()
            .getSharedPreferences(ROOT, Context.MODE_PRIVATE);
    }

    private static String key(LocalDate day, String provider, String field) {
        return day + "|" + provider + "|" + field;
    }

    private static String provider(String packageName) {
        return ProviderEventCode.code(packageName);
    }

    private static boolean valid(LocalDate day) {
        return HistoryRecordPolicy.withinRetention(day, LocalDate.now());
    }

    /** Prunes on each new event; never stores older, future or free-form data. */
    private void pruneLocked(LocalDate today) {
        Map<String, ?> all = prefs.getAll();
        SharedPreferences.Editor edits = prefs.edit();
        boolean changed = false;
        for (String item : all.keySet()) {
            if (item.length() < 12) {
                edits.remove(item);
                changed = true;
                continue;
            }
            LocalDate recorded;
            try {
                if (item.charAt(10) != '|') throw new DateTimeParseException(
                    "Bad history key", item, 10);
                recorded = LocalDate.parse(item.substring(0, 10));
            } catch (RuntimeException invalid) {
                edits.remove(item);
                changed = true;
                continue;
            }
            if (!HistoryRecordPolicy.withinRetention(recorded, today)) {
                edits.remove(item);
                changed = true;
            }
        }
        if (changed) edits.commit();
    }

    public boolean recordAudit(LocalDate day, String rawStatus) {
        if (!valid(day)) return false;
        String code = HistoryRecordPolicy.audit(rawStatus);
        if (code.isEmpty()) return false;
        synchronized (LOCK) {
            pruneLocked(LocalDate.now());
            return prefs.edit().putString(key(day, "AUDIT", "status"), code).commit();
        }
    }

    public boolean recordDirect(String packageName, LocalDate day, String rawStatus) {
        String provider = provider(packageName);
        String code = HistoryRecordPolicy.direct(rawStatus);
        if (provider.isEmpty() || code.isEmpty() || !valid(day)) return false;
        synchronized (LOCK) {
            pruneLocked(LocalDate.now());
            return prefs.edit().putString(key(day, provider, "direct"), code).commit();
        }
    }

    public boolean recordObservation(String packageName, LocalDate day, String rawCode) {
        String provider = provider(packageName);
        String code = HistoryRecordPolicy.observation(rawCode);
        if (provider.isEmpty() || code.isEmpty() || !valid(day)) return false;
        synchronized (LOCK) {
            pruneLocked(LocalDate.now());
            String field = key(day, provider, "observation");
            String previous = prefs.getString(field, "");
            String strongest = ObservationRank.strongest(previous, code);
            // Provider apps may publish numerous identical notifications.
            // Don't write the same code to flash storage more than once.
            if (strongest.equals(previous)) return true;
            return prefs.edit().putString(field, strongest).commit();
        }
    }

    public boolean recordGuided(String packageName, LocalDate day, GuidedOutcome outcome) {
        String provider = provider(packageName);
        if (provider.isEmpty() || outcome == null || !valid(day)) return false;
        synchronized (LOCK) {
            pruneLocked(LocalDate.now());
            SharedPreferences.Editor editor = prefs.edit()
                .putString(key(day, provider, "guided"), outcome.name());
            // A later cancellation must not erase a click attempted today.
            if (outcome.clickAttempted())
                editor.putBoolean(key(day, provider, "clicked"), true);
            return editor.commit();
        }
    }

    public boolean recordCredit(String packageName, LocalDate day, String rawStatus) {
        String provider = provider(packageName);
        if (provider.isEmpty() || !valid(day)) return false;
        synchronized (LOCK) {
            pruneLocked(LocalDate.now());
            return prefs.edit().putString(key(day, provider, "credit"),
                HistoryRecordPolicy.credit(rawStatus)).commit();
        }
    }

    /** Explicit user-requested archive reset; leaves live permissions and alarms alone. */
    public boolean clear() {
        synchronized (LOCK) {
            return prefs.edit().clear().commit();
        }
    }

    public List<LocalDate> recentDays() {
        LocalDate today = LocalDate.now();
        synchronized (LOCK) {
            pruneLocked(today);
            Set<LocalDate> found = new HashSet<>();
            for (String item : prefs.getAll().keySet()) {
                if (item.length() < 12 || item.charAt(10) != '|') continue;
                try {
                    LocalDate day = LocalDate.parse(item.substring(0, 10));
                    if (HistoryRecordPolicy.withinRetention(day, today)) found.add(day);
                } catch (DateTimeParseException ignored) {
                    // Corrupt keys are removed by pruneLocked.
                }
            }
            List<LocalDate> dates = new ArrayList<>(found);
            dates.sort((a, b) -> b.compareTo(a));
            return dates;
        }
    }

    /** Old dates are immutable snapshots; today is rendered by the live report. */
    public String report(Context context, LocalDate day) {
        if (day == null || !valid(day))
            return "Aucune donnée conservée pour cette date (historique limité à 30 jours).";
        if (day.equals(LocalDate.now()))
            return DailyProviderReport.build(context, day);
        synchronized (LOCK) {
            DailyProviderReportPolicy.Entry[] entries = {
                entry(day, ProviderPackages.WEWARD, "WeWard"),
                entry(day, ProviderPackages.MACADAM, "Macadam"),
                entry(day, ProviderPackages.WINWALK, "Winwalk"),
                entry(day, ProviderPackages.SWEATCOIN, "Sweatcoin")
            };
            String audit = prefs.getString(key(day, "AUDIT", "status"), "");
            return DailyProviderReportPolicy.renderArchive(day, audit, entries);
        }
    }

    private DailyProviderReportPolicy.Entry entry(LocalDate day,
            String packageName, String name) {
        String provider = provider(packageName);
        return new DailyProviderReportPolicy.Entry(
            name, false,
            prefs.getString(key(day, provider, "direct"), ""),
            prefs.getString(key(day, provider, "observation"), ""),
            GuidedOutcome.fromStored(prefs.getString(
                key(day, provider, "guided"), "")),
            HistoryRecordPolicy.credit(prefs.getString(
                key(day, provider, "credit"), "")),
            prefs.getBoolean(key(day, provider, "clicked"), false));
    }

    public String recentSummary() {
        List<LocalDate> days = recentDays();
        if (days.isEmpty()) return "Aucun événement encore archivé. L'historique commence avec la v1.0.";
        StringBuilder lines = new StringBuilder();
        int limit = Math.min(days.size(), 7);
        for (int i = 0; i < limit; i++) {
            LocalDate day = days.get(i);
            int reported = 0;
            int sent = 0;
            int clicks = 0;
            for (String packageName : GuidedClaimPolicy.ORDER) {
                String code = provider(packageName);
                if (CreditEvidencePolicy.USER_REPORTED_RECEIVED.equals(
                        prefs.getString(key(day, code, "credit"), ""))) reported++;
                if (DailyProviderReportPolicy.directSent(
                        prefs.getString(key(day, code, "direct"), ""))) sent++;
                if (prefs.getBoolean(key(day, code, "clicked"), false)) clicks++;
            }
            if (i > 0) lines.append("\n");
            lines.append(day).append(" · ").append(reported)
                .append(" déclarés reçus · ").append(sent)
                .append(" commandes · ").append(clicks).append(" clics");
        }
        return lines.toString();
    }
}
