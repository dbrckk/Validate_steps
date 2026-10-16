package fr.validatesteps;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/** Exact 22:00 trigger where allowed, bounded background retries otherwise. */
public final class DailyScheduler {
    private static final int DAILY = 2200;
    private static final int RECOVERY = 2212;
    private DailyScheduler() {}

    private static PendingIntent intent(Context context, int request) {
        Intent trigger = new Intent(context, NightlyReceiver.class);
        trigger.setAction(request == DAILY ? "fr.validatesteps.DAILY" : "fr.validatesteps.RECOVERY");
        return PendingIntent.getBroadcast(context, request, trigger,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean exactAllowed(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
    }

    private static void set(Context context, int request, ZonedDateTime when) {
        if (when == null) return;
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        PendingIntent pending = intent(context, request);
        long at = when.toInstant().toEpochMilli();
        if (exactAllowed(context)) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
                return;
            } catch (SecurityException ignored) {
                // Permission can be revoked between checks.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending);
    }

    public static void cancelRecovery(Context context) {
        context.getSystemService(AlarmManager.class).cancel(intent(context, RECOVERY));
    }

    /** Idempotently arms the next 22:00 local alarm; clears retries on disable. */
    public static void schedule(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        manager.cancel(intent(context, DAILY));
        if (!new RunStore(context).enabled()) {
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
