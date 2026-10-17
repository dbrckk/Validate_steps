package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

/**
 * Local evidence ledger. Stores only whitelisted package identifiers, predefined
 * classification codes and dates. No notification message text or account tokens.
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

    /** A claimed dispatch is reserved before sending to prevent double execution. */
    public boolean reserve(String packageName, LocalDate day) {
        if (!ClaimActionPolicy.isProvider(packageName) ||
            alreadyAttempted(packageName, day)) return false;
        return prefs.edit()
            .putString("date_" + packageName, day.toString())
            .putString("status_" + packageName, "dispatching")
            .commit();
    }

    public void record(String packageName, String status) {
        if (!ClaimActionPolicy.isProvider(packageName)) return;
        prefs.edit().putString("status_" + packageName, status).commit();
    }

    /** Diagnostic metadata only; never persist text from a third-party notification. */
    public void noteSeen(String packageName, String category) {
        if (!ClaimActionPolicy.isProvider(packageName)) return;
        prefs.edit().putString("seen_" + packageName, LocalDate.now().toString())
            .putString("category_" + packageName, category)
            .putLong("seen_at_" + packageName, System.currentTimeMillis()).apply();
    }

    public void inspected(boolean connected) {
        prefs.edit().putString("last_inspection", LocalDate.now().toString())
            .putLong("last_inspection_time", System.currentTimeMillis())
            .putString("listener_state", connected ? "connecte" : "non_connecte").apply();
    }

    public String summary() {
        return "Actions de notification : " + (enabled() ? "activées" : "désactivées") +
            "\nService Android : " + prefs.getString("listener_state", "non vérifié") +
            "\nDernière inspection : " + prefs.getString("last_inspection", "jamais") +
            "\nWeWard : " + status("com.weward") +
            "\nMacadam : " + status("com.macadamapp") +
            "\nWinwalk : " + status("com.winwalk.android") +
            "\nACTION ENVOYÉE ≠ RÉCOMPENSE CRÉDITÉE.";
    }

    private String status(String packageName) {
        String seen = prefs.getString("seen_" + packageName, "");
        String diagnostic = seen.isEmpty() ? "notification non observée" :
            "notification " + seen + ", " +
                prefs.getString("category_" + packageName, "inconnue");
        String attempted = prefs.getString("date_" + packageName, "");
        if (!attempted.isEmpty()) diagnostic += ", tentative " + attempted + " : " +
            prefs.getString("status_" + packageName, "inconnue");
        return diagnostic;
    }
}
