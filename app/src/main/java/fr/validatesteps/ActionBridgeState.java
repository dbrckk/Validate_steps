package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

/**
 * Private, bounded, non-sensitive action metadata.
 * Notification text, user health data and tokens are never stored here.
 */
public final class ActionBridgeState {
    private final SharedPreferences prefs;

    public ActionBridgeState(Context context) {
        prefs = context.getSharedPreferences("notification_action_bridge", Context.MODE_PRIVATE);
    }

    public boolean enabled() { return prefs.getBoolean("enabled", false); }

    public void enable(boolean value) {
        prefs.edit().putBoolean("enabled", value).commit();
    }

    public boolean alreadyAttempted(String packageName, LocalDate day) {
        return day.toString().equals(prefs.getString("date_" + packageName, ""));
    }

    public boolean reserve(String packageName, LocalDate day) {
        if (alreadyAttempted(packageName, day)) return false;
        return prefs.edit()
            .putString("date_" + packageName, day.toString())
            .putString("status_" + packageName, "dispatching")
            .commit();
    }

    public void record(String packageName, String status) {
        prefs.edit().putString("status_" + packageName, status).commit();
    }

    public void inspected() {
        prefs.edit().putString("last_inspection", LocalDate.now().toString())
            .putLong("last_inspection_time", System.currentTimeMillis()).apply();
    }

    public String summary() {
        String day = prefs.getString("last_inspection", "jamais");
        return "Actions de notification : " + (enabled() ? "activées" : "désactivées") +
            "\nDernière inspection : " + day +
            "\nWeWard : " + status("com.weward") +
            "\nMacadam : " + status("com.macadamapp") +
            "\nWinwalk : " + status("com.winwalk.android") +
            "\nAucune action envoyée ne prouve un crédit de récompense.";
    }

    private String status(String packageName) {
        String date = prefs.getString("date_" + packageName, "");
        String value = prefs.getString("status_" + packageName, "aucune action éligible observée");
        return (date.isEmpty() ? "" : date + " : ") + value;
    }
}
