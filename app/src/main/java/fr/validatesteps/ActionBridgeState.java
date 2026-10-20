package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

/**
 * Local evidence ledger. Stores only whitelisted package identifiers, predefined
 * classification codes and dates. No notification message text or account tokens.
 */
public final class ActionBridgeState {
    // A synchronized reservation is essential if Android delivers notification
    // callbacks and a manual "Tout valider" request in close succession.
    // One process owns this ledger; SharedPreferences alone does not make
    // a read/check/write sequence atomic.
    private static final Object RESERVATION_LOCK = new Object();
    private final SharedPreferences prefs;
    public ActionBridgeState(Context context) {
        prefs = context.getSharedPreferences("notification_action_bridge", Context.MODE_PRIVATE);
    }

    public boolean enabled() { return prefs.getBoolean("enabled", false); }

    public void enable(boolean value) {
        prefs.edit().putBoolean("enabled", value).commit();
    }

    /**
     * A dispatched PendingIntent does not prove provider settlement.
     * Avoid an immediate second UI tap in the same user-started workflow.
     */
    public boolean dispatchedToday(String packageName, LocalDate day) {
        return ClaimActionPolicy.isProvider(packageName) && day != null
            && day.toString().equals(prefs.getString("date_" + packageName, ""))
            && "action_envoyee_credit_non_confirme".equals(
                prefs.getString("status_" + packageName, ""));
    }

    /** Fixed-category notification observation for the requested local day. */
    public String observationOn(String packageName, LocalDate day) {
        if (!ClaimActionPolicy.isProvider(packageName) || day == null ||
                !day.toString().equals(prefs.getString("seen_" + packageName, "")))
            return "";
        return prefs.getString("category_" + packageName, "");
    }

    /** Today's fixed internal result only; stale attempts never leak to today's report. */
    public String actionStatusOn(String packageName, LocalDate day) {
        if (!ClaimActionPolicy.isProvider(packageName) || day == null ||
                !day.toString().equals(prefs.getString("date_" + packageName, "")))
            return "";
        return prefs.getString("status_" + packageName, "");
    }

    public boolean alreadyAttempted(String packageName, LocalDate day) {
        return !ClaimRetryPolicy.allowed(day,
            prefs.getString("date_" + packageName, ""),
            prefs.getString("status_" + packageName, ""),
            prefs.getInt("attempts_" + packageName, 0));
    }

    /** A claimed dispatch is reserved before sending to prevent double execution. */
    public boolean reserve(String packageName, LocalDate day) {
        if (!ClaimActionPolicy.isProvider(packageName) || day == null) return false;
        synchronized (RESERVATION_LOCK) {
            if (alreadyAttempted(packageName, day)) return false;
            int tries = day.toString().equals(prefs.getString("date_" + packageName, ""))
                ? prefs.getInt("attempts_" + packageName, 0) + 1 : 1;
            return prefs.edit()
                .putString("date_" + packageName, day.toString())
                .putString("status_" + packageName, "dispatching")
                .putInt("attempts_" + packageName, tries)
                .commit();
        }
    }

    public void record(String packageName, String status) {
        if (!ClaimActionPolicy.isProvider(packageName)) return;
        prefs.edit().putString("status_" + packageName, status).commit();
    }

    /** Diagnostic metadata only; never persist text from a third-party notification. */
    public void noteSeen(String packageName, String category) {
        if (!ClaimActionPolicy.isProvider(packageName)) return;
        String day = LocalDate.now().toString();
        String previousDate = prefs.getString("seen_" + packageName, "");
        String previous = day.equals(previousDate)
            ? prefs.getString("category_" + packageName, "") : "";
        String strongest = ObservationRank.strongest(previous, category);
        prefs.edit().putString("seen_" + packageName, day)
            .putString("category_" + packageName, strongest)
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

    /** A compact, honest, provider-specific notification result for the dashboard. */
    public String brief(String packageName) {
        if (!ClaimActionPolicy.isProvider(packageName)) return "Aucune action observée";
        String seen = prefs.getString("seen_" + packageName, "");
        if (!LocalDate.now().toString().equals(seen)) return "Aucune action observée";
        String attempted = prefs.getString("date_" + packageName, "");
        if (LocalDate.now().toString().equals(attempted))
            return "Action Android : " +
                prefs.getString("status_" + packageName, "tentative non confirmée");
        return "Notification : " +
            prefs.getString("category_" + packageName, "non reconnue");
    }

    private String status(String packageName) {
        String seen = prefs.getString("seen_" + packageName, "");
        String diagnostic = seen.isEmpty() ? "notification non observée" :
            "notification " + seen + ", " +
                prefs.getString("category_" + packageName, "inconnue");
        String attempted = prefs.getString("date_" + packageName, "");
        if (!attempted.isEmpty()) diagnostic += ", tentatives " + attempted + " : " +
            prefs.getInt("attempts_" + packageName, 1) + ", " +
            prefs.getString("status_" + packageName, "inconnue");
        return diagnostic;
    }
}
