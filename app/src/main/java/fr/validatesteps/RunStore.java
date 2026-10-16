package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

/** Private audit state. A step count is never mistaken for provider wallet credit. */
public final class RunStore {
    private final SharedPreferences prefs;

    public RunStore(Context context) {
        prefs = context.getSharedPreferences("validate_steps", Context.MODE_PRIVATE);
    }

    public boolean enabled() { return prefs.getBoolean("enabled", false); }

    public void enable(boolean enabled) {
        prefs.edit().putBoolean("enabled", enabled).commit();
    }

    public void start(LocalDate date) {
        prefs.edit().putString("last_date", date.toString())
            .putLong("last_executed_at", System.currentTimeMillis())
            .putLong("last_steps", -1)
            .putString("last_status", "reading").commit();
    }

    public void finish(Long steps, String status) {
        SharedPreferences.Editor edit = prefs.edit()
            .putLong("last_steps", steps == null ? -1 : steps)
            .putString("last_status", status);
        edit.commit();
    }

    public String describe() {
        String date = prefs.getString("last_date", "jamais");
        String state = prefs.getString("last_status", "pas encore executé");
        long steps = prefs.getLong("last_steps", -1);
        String stepsText = steps < 0 ? "non disponible" : Long.toString(steps);
        return "Dernier passage : " + date + "\nPas : " + stepsText +
            "\nLecture : " + state + "\nRécompenses externes : non vérifiées";
    }
}
