package fr.validatesteps;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Optional private warning about reward status, not automatic settlement.
 * It never opens an app, never reads a balance and never clicks a provider UI.
 */
public final class AutonomousReviewReminder {
    private static final String PREFS = "autonomous_review";
    private static final String CHANNEL = "rewards_review";
    private static final int NOTIFICATION_ID = 2315;
    private static final Object LOCK = new Object();
    private AutonomousReviewReminder() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(
            PREFS, Context.MODE_PRIVATE);
    }

    public static boolean enabled(Context context) {
        return prefs(context).getBoolean("enabled", false);
    }

    public static void enable(Context context, boolean value) {
        prefs(context).edit().putBoolean("enabled", value).commit();
    }

    public static boolean maybeShow(Context context, LocalDate scheduledDay,
            ZonedDateTime now) {
        if (context == null || now == null || scheduledDay == null) return false;
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false;
        RunStore schedule = new RunStore(context);
        ProviderCreditStore credits = new ProviderCreditStore(context);
        int unverified = 0;
        StringBuilder names = new StringBuilder();
        for (String pkg : new String[]{
                ProviderPackages.WEWARD,
                ProviderPackages.MACADAM,
                ProviderPackages.WINWALK}) {
            if (!ProviderPackages.installed(context, pkg) ||
                    credits.userReportedReceivedToday(pkg, now.toLocalDate())) continue;
            if (names.length() > 0) names.append(", ");
            names.append(ProviderEventCode.code(pkg));
            unverified++;
        }
        synchronized (LOCK) {
            SharedPreferences prefs = prefs(context);
            boolean already = scheduledDay.toString().equals(
                prefs.getString("last_review_day", ""));
            if (!AutonomousReviewPolicy.shouldNotify(scheduledDay, now.toLocalDate(),
                    now.toLocalTime(), schedule.enabled(), enabled(context),
                    already, unverified)) return false;
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return false;
            try {
                manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL, "Bilan facultatif des récompenses",
                    NotificationManager.IMPORTANCE_DEFAULT));
                Intent open = new Intent(context, MainActivity.class);
                PendingIntent view = PendingIntent.getActivity(context, 2315, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                Notification note = new Notification.Builder(context, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("Récompenses non vérifiées")
                    .setContentText(names + " : vérification dans les applications nécessaire")
                    .setStyle(new Notification.BigTextStyle().bigText(
                        names + " : aucune réception confirmée par les fournisseurs. " +
                        "Le relevé des pas et les éventuelles commandes Android " +
                        "ne prouvent pas un crédit."))
                    .setContentIntent(view)
                    .setAutoCancel(true)
                    .setVisibility(Notification.VISIBILITY_PRIVATE)
                    .build();
                if (!prefs.edit()
                        .putString("last_review_day", scheduledDay.toString()).commit())
                    return false;
                manager.notify(NOTIFICATION_ID, note);
                new NightTimeline(context).add(
                    NightTimeline.Event.AUTONOMOUS_REVIEW_REMINDER, scheduledDay);
                return true;
            } catch (SecurityException permissionRevoked) {
                return false;
            }
        }
    }
}
