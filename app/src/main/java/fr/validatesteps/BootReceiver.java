package fr.validatesteps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-arm on reboot, time changes and exact-alarm permission grants. */
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        DailyScheduler.schedule(context);
        RewardActionListener.ensureConnected(context);
        // Recover missed 22:00 audit after a late restart, before the date changes.
        DailyScheduler.scheduleRecovery(context, true);
    }
}
