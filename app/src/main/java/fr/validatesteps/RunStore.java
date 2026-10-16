package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

/** Local truthful audit state: reading steps never claims external rewards. */
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

    public void start(LocalDate date) {
        boolean sameDay = date.toString().equals(prefs.getString("last_date", ""));
        int attempts = sameDay ? prefs.getInt("attempts", 0) + 1 : 1;
        prefs.edit().putString("last_date", date.toString())
            .putLong("last_executed_at", System.currentTimeMillis())
            .putLong("last_steps", -1)
            .putInt("attempts", attempts)
            .putString("last_status", "reading").commit();
    }

    public void finish(LocalDate day, Long steps, String status) {
        if (!day.toString().equals(prefs.getString("last_date", ""))) return;
        // Another worker may have already finished a successful read.
        if (successfulOn(day)) return;
        prefs.edit().putLong("last_steps", steps == null ? -1 : steps)
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
