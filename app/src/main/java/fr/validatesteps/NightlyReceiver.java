package fr.validatesteps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.time.LocalDate;

/** No UI. Each scheduled alarm carries the date it belongs to. */
public final class NightlyReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        RunStore store = new RunStore(context);
        if (!store.enabled() || intent == null) return;

        LocalDate today = LocalDate.now();
        LocalDate auditedDay = AuditDay.resolve(
            intent.getStringExtra(DailyScheduler.EXTRA_AUDIT_DATE), today);
        if (auditedDay == null) return; // Never attribute a stale alarm to today.

        DailyScheduler.schedule(context);
        RewardActionListener.inspectConnectedNow();
        if (auditedDay.equals(today)) {
            // A safety retry remains scheduled even if Android kills the worker.
            DailyScheduler.scheduleRecovery(context, false);
        }
        AuditWorker.enqueue(context.getApplicationContext(), auditedDay);
    }
}
