package fr.validatesteps;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.time.ZonedDateTime;

/** Only plans a daily local on-device read; never launches provider apps. */
public final class DailyScheduler {
    private static final int REQUEST = 2200;
    private DailyScheduler() {}

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, NightlyReceiver.class);
        return PendingIntent.getBroadcast(context, REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean exactAllowed(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        return Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
    }

    public static void schedule(Context context) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        PendingIntent intent = alarmIntent(context);
        manager.cancel(intent);
        if (!new RunStore(context).enabled()) return;

        long next = TimePlanner.next22(ZonedDateTime.now()).toInstant().toEpochMilli();
        if (exactAllowed(context)) {
            try {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent);
                return;
            } catch (SecurityException ignored) {
                // Revocation between permission check and use.
            }
        }
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent);
    }
}
