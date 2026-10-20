package fr.validatesteps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/** No UI. Each scheduled alarm carries the date it belongs to. */
public final class NightlyReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        RunStore store = new RunStore(context);
        NightTimeline timeline = new NightTimeline(context);
        if (!store.enabled() || intent == null) return;

        ZonedDateTime now = ZonedDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate auditedDay = AuditDay.resolve(
            intent.getStringExtra(DailyScheduler.EXTRA_AUDIT_DATE), today);
        if (auditedDay == null) {
            timeline.add(NightTimeline.Event.ALARM_REJECTED, null);
            return; // Never attribute a stale alarm to today.
        }
        timeline.add("fr.validatesteps.RECOVERY".equals(intent.getAction())
            ? NightTimeline.Event.RECOVERY_RECEIVED
            : NightTimeline.Event.ALARM_RECEIVED, auditedDay);

        DailyScheduler.schedule(context);
        // An old alarm may be delivered after midnight or even 22h on a
        // later day. Keep its audit date, but never let it trigger today's
        // reward notifications or a listener reconnection.
        if (ScheduledActionPolicy.canInspect(auditedDay, now, store.enabled())) {
            RewardActionListener.ensureConnected(context);
            RewardActionListener.inspectConnectedNow();
        } else {
            timeline.add(NightTimeline.Event.SCHEDULED_ACTION_IGNORED, auditedDay);
        }
        if (auditedDay.equals(today)) {
            // A safety retry remains scheduled even if Android kills the worker.
            DailyScheduler.scheduleRecovery(context, false);
        }
        AuditWorker.enqueue(context.getApplicationContext(), auditedDay);
        timeline.add(NightTimeline.Event.AUDIT_ENQUEUED, auditedDay);
    }
}
