package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

/** Private local step audit only; never treats measurements as reward credits. */
public final class RunStore {
    private final SharedPreferences prefs;
    public RunStore(Context context) {
        prefs = context.getSharedPreferences("validate_steps", Context.MODE_PRIVATE);
    }

    public boolean enabled() { return prefs.getBoolean("enabled", false); }

    public void enable(boolean enabled) {
        prefs.edit().putBoolean("enabled", enabled).commit();
    }

    public boolean successfulOn(LocalDate day) {
        return day.toString().equals(prefs.getString("last_date", "")) &&
            "steps_read".equals(prefs.getString("last_status", ""));
    }

    /** Prevent old delayed requests from overwriting later days or completed reads. */
    public boolean begin(LocalDate day) {
        if (AuditDay.isOlderThanRecorded(day, prefs.getString("last_date", "")) ||
            successfulOn(day)) return false;
        boolean sameDay = day.toString().equals(prefs.getString("last_date", ""));
        int attempts = sameDay ? prefs.getInt("attempts", 0) + 1 : 1;
        return prefs.edit()
            .putString("last_date", day.toString())
            .putLong("last_executed_at", System.currentTimeMillis())
            .putLong("last_steps", -1)
            .putInt("attempts", attempts)
            .putString("last_status", "reading")
            .commit();
    }

    public boolean finish(LocalDate day, Long steps, String status) {
        if (!day.toString().equals(prefs.getString("last_date", "")) ||
            successfulOn(day)) return false;
        return prefs.edit()
            .putLong("last_steps", steps == null ? -1 : steps)
            .putString("last_status", status).commit();
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
