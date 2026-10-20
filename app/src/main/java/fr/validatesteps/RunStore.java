package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

/** Private local step audit only; never treats measurements as reward credits. */
public final class RunStore {
    private final SharedPreferences prefs;
    private final Context appContext;
    public RunStore(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences("validate_steps", Context.MODE_PRIVATE);
    }

    public boolean enabled() { return prefs.getBoolean("enabled", false); }

    public void enable(boolean enabled) {
        prefs.edit().putBoolean("enabled", enabled).commit();
    }

    /** Read-only status for exact day; does not expose step counts. */
    public String statusOn(LocalDate day) {
        if (day == null || !day.toString().equals(prefs.getString("last_date", "")))
            return "";
        return prefs.getString("last_status", "");
    }

    public boolean successfulOn(LocalDate day) {
        return day.toString().equals(prefs.getString("last_date", "")) &&
            "steps_read".equals(prefs.getString("last_status", ""));
    }

    /** No pointless background reads after a permission/platform failure that needs user action. */
    public boolean permanentlyUnavailableOn(LocalDate day) {
        if (!day.toString().equals(prefs.getString("last_date", ""))) return false;
        String status = prefs.getString("last_status", "");
        return "permission_missing".equals(status) ||
            "background_health_unsupported".equals(status) ||
            "health_connect_unavailable".equals(status);
    }

    /** Prevent old delayed requests from overwriting later days or completed reads. */
    public boolean begin(LocalDate day) {
        if (AuditDay.isOlderThanRecorded(day, prefs.getString("last_date", "")) ||
            successfulOn(day)) return false;
        boolean sameDay = day.toString().equals(prefs.getString("last_date", ""));
        int attempts = sameDay ? prefs.getInt("attempts", 0) + 1 : 1;
        boolean committed = prefs.edit()
            .putString("last_date", day.toString())
            .putLong("last_executed_at", System.currentTimeMillis())
            .putLong("last_steps", -1)
            .putInt("attempts", attempts)
            .putString("last_status", "reading")
            .commit();
        if (committed) new DailyHistoryStore(appContext).recordAudit(day, "reading");
        return committed;
    }

    public boolean finish(LocalDate day, Long steps, String status) {
        if (!day.toString().equals(prefs.getString("last_date", "")) ||
            successfulOn(day)) return false;
        String storedStatus = StepValuePolicy.storedStatus(steps, status);
        boolean committed = prefs.edit()
            .putLong("last_steps", StepValuePolicy.storedCount(steps, status))
            .putString("last_status", storedStatus)
            .commit();
        if (committed) new DailyHistoryStore(appContext).recordAudit(day, storedStatus);
        return committed;
    }

    public String describe() {
        String date = prefs.getString("last_date", "jamais");
        String state = prefs.getString("last_status", "pas encore exécuté");
        long steps = prefs.getLong("last_steps", -1);
        return "Dernier passage : " + date +
            "\nPas : " + (steps < 0 ? "non disponibles" : Long.toString(steps)) +
            "\nTentatives : " + prefs.getInt("attempts", 0) +
            "\nLecture : " + state +
            "\nRécompenses WeWard/Macadam/Winwalk : non confirmées";
    }
}
