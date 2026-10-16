package fr.validatesteps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Tiny broadcast: durable work goes into WorkManager, never an Activity. */
public final class NightlyReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!new RunStore(context).enabled()) return;
        DailyScheduler.schedule(context);
        AuditWorker.enqueue(context.getApplicationContext());
    }
}
