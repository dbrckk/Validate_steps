package fr.validatesteps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Tiny no-UI broadcast; worker executes the read, with a safety retry alarm. */
public final class NightlyReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!new RunStore(context).enabled()) return;
        // Never depend on this short-lived receiver surviving Health Connect's async call.
        DailyScheduler.schedule(context);
        // Arm fallback before starting a worker, including if Android kills the worker.
        // The worker cancels this alarm after a successful or permanently unavailable read.
        DailyScheduler.scheduleRecovery(context, false);
        AuditWorker.enqueue(context.getApplicationContext());
    }
}
