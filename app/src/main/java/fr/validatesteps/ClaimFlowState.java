package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Private bounded state machine for one explicitly user-started guided run.
 * Records attempted Android interactions, NEVER confirmed reward credits.
 */
public final class ClaimFlowState {
    private static final long MAX_AGE_MS = 120_000L;
    private final SharedPreferences prefs;

    public ClaimFlowState(Context context) {
        prefs = context.getSharedPreferences("guided_claim_flow", Context.MODE_PRIVATE);
    }

    public void start() {
        // Beginning a new run must not present earlier same-day attempts as if
        // they were part of the new sequence. Explicit user credit reports
        // live in a separate store and are never reset by this operation.
        SharedPreferences.Editor editor = prefs.edit()
            .putBoolean("active", true)
            .putLong("started", System.currentTimeMillis())
            .putInt("index", 0)
            .putString("summary", "Démarrage du parcours assisté");
        String day = java.time.LocalDate.now().toString();
        for (String provider : GuidedClaimPolicy.ORDER) {
            editor.putString("result_day_" + provider, day);
            editor.putString("result_" + provider, "En attente de traitement");
        }
        editor.commit();
    }

    public boolean active() {
        return prefs.getBoolean("active", false)
            && System.currentTimeMillis() - prefs.getLong("started", 0) < MAX_AGE_MS
            && System.currentTimeMillis() >= prefs.getLong("started", 0);
    }

    public int index() { return prefs.getInt("index", 0); }

    public String target() {
        int i = index();
        return active() && i >= 0 && i < GuidedClaimPolicy.ORDER.length
            ? GuidedClaimPolicy.ORDER[i] : null;
    }

    public void report(String pkg, String result, GuidedOutcome outcome) {
        if (!GuidedClaimPolicy.isTarget(pkg) || outcome == null) return;
        String today = java.time.LocalDate.now().toString();
        prefs.edit().putString("result_" + pkg, result)
            .putString("result_day_" + pkg, today)
            .putString("outcome_" + pkg, outcome.name())
            .putString("outcome_day_" + pkg, today)
            .putString("summary", result).commit();
    }

    /** Compatibility with older in-process callers: never invent a click. */
    public void report(String pkg, String result) {
        report(pkg, result, GuidedOutcome.NONE);
    }

    /** Date-bound structured status, independent of localized display text. */
    public GuidedOutcome outcomeOn(String pkg, java.time.LocalDate day) {
        if (!GuidedClaimPolicy.isTarget(pkg) || day == null ||
                !day.toString().equals(prefs.getString("outcome_day_" + pkg, "")))
            return GuidedOutcome.NONE;
        return GuidedOutcome.fromStored(prefs.getString("outcome_" + pkg, ""));
    }

    public boolean advance(String expectedPkg) {
        if (!active() || !expectedPkg.equals(target())) return false;
        int next = index() + 1;
        prefs.edit().putInt("index", next)
            .putBoolean("active", next < GuidedClaimPolicy.ORDER.length).commit();
        return next < GuidedClaimPolicy.ORDER.length;
    }

    public void stop() {
        prefs.edit().putBoolean("active", false).putString("summary",
            "Parcours arrêté ou terminé").commit();
    }

    public String result(String pkg) {
        if (!java.time.LocalDate.now().toString().equals(
                prefs.getString("result_day_" + pkg, "")))
            return "Non vérifié aujourd'hui";
        return prefs.getString("result_" + pkg, "Non vérifié aujourd'hui");
    }

    public String summary() {
        return (active() ? "Parcours en cours" : "Parcours inactif") +
            " · " + prefs.getString("summary", "Prêt");
    }
}
