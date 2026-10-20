package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Single opt-in setup for nightly step reads + provider-created notification
 * PendingIntent actions. NO Accessibility, no app launching, no credentials.
 *
 * Android permissions can only be granted by the user in system settings.
 * Remembering the user's explicit request allows Android's notification
 * permission to be reconciled when returning from those settings.
 */
public final class UnattendedModeController {
    private static final String PREFS = "unattended_mode";
    private static final String KEY = "requested";
    private final Context context;
    private final SharedPreferences prefs;

    public UnattendedModeController(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // Carry over an explicit v1.0 opt-in to BOTH daily alarms and direct
        // notification actions. Never auto-opt-in users who enabled only
        // step auditing, or who declined access to notifications.
        if (!prefs.contains(KEY) && new RunStore(this.context).enabled() &&
                new ActionBridgeState(this.context).enabled()) {
            prefs.edit().putBoolean(KEY, true).commit();
        }
    }

    public boolean requested() { return prefs.getBoolean(KEY, false); }

    /** Called only after a clear user opt-in to the no-tap nightly mode. */
    public void enable() {
        if (!prefs.edit().putBoolean(KEY, true).commit()) return;
        new RunStore(context).enable(true);
        DailyScheduler.schedule(context);
        DailyScheduler.scheduleRecovery(context, true);
        reconcile();
    }

    /** Called on return from settings, not a new automatic authorization. */
    public void reconcile() {
        if (!requested()) return;
        ActionBridgeState actions = new ActionBridgeState(context);
        boolean allowed = UnattendedModePolicy.canArmDirectActions(
            true, RewardActionListener.hasPermission(context));
        if (actions.enabled() != allowed) actions.enable(allowed);
        if (allowed) RewardActionListener.ensureConnected(context);
    }

    public void disable() {
        prefs.edit().putBoolean(KEY, false).commit();
        new RunStore(context).enable(false);
        new ActionBridgeState(context).enable(false);
        DailyScheduler.schedule(context); // cancel alarms and WorkManager fallbacks
    }
}
