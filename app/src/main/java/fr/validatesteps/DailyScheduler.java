package fr.validatesteps;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/** Schedules reads for an explicit local date, even if Android delivers late. */
public final class DailyScheduler {
    public static final String EXTRA_AUDIT_DATE = "fr.validatesteps.AUDIT_DATE";
    private static final int DAILY = 2200;
    private static final int RECOVERY = 2212;
    private DailyScheduler() {}

    private static PendingIntent intent(Context context, int request, LocalDate day) {
        Intent trigger = new Intent(context, NightlyReceiver.class);
        trigger.setAction(request == DAILY ? "fr.validatesteps.DAILY" : "fr.validatesteps.RECOVERY");
        if (day != null) trigger.putExtra(EXTRA_AUDIT_DATE, day.toString());
        return PendingIntent.getBroadcast(context, request, trigger,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean exactAllowed(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        return Build.VERSION.SDK_INT < 31 || manager.canScheduleExactAlarms();
    }

    private static void set(Context context, int request, ZonedDateTime when) {
        if (when == null) return;
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        PendingIntent pending = intent(context, request, when.toLocalDate());
        long timestamp = when.toInstant().toEpochMilli();
        if (exactAllowed(context)) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pending);
                return;
            } catch (SecurityException ignored) {
                // Exact alarm permission was revoked between inspection and scheduling.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timestamp, pending);
    }

    public static void cancelRecovery(Context context) {
        context.getSystemService(AlarmManager.class)
            .cancel(intent(context, RECOVERY, null));
    }

    /** Cancel & re-arm only the next scheduled daily read. */
    public static void schedule(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        manager.cancel(intent(context, DAILY, null));
        boolean enabled = new RunStore(context).enabled();
        WatchdogWorker.configure(context, enabled);
        if (!enabled) {
            cancelRecovery(context);
            return;
        }
        set(context, DAILY, TimePlanner.next22(ZonedDateTime.now()));
    }

    public static void scheduleRecovery(Context context, boolean rebootCatchup) {
        cancelRecovery(context);
        RunStore store = new RunStore(context);
        LocalDate today = LocalDate.now();
        if (!store.enabled() || store.successfulOn(today)) return;
        ZonedDateTime now = ZonedDateTime.now();
        set(context, RECOVERY, rebootCatchup
            ? RetryPolicy.recovery(now) : RetryPolicy.retry(now));
    }
}
