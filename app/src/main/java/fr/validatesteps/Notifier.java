package fr.validatesteps;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import java.time.LocalDate;

/** Notifies of a real step read for the intended day; never asserts rewards. */
public final class Notifier {
    private static final String CHANNEL = "step_audit";
    private Notifier() {}

    public static void show(Context context, LocalDate day, Long steps, String status) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
            CHANNEL, "Suivi autonome des pas", NotificationManager.IMPORTANCE_DEFAULT);
        try {
            manager.createNotificationChannel(channel);
        } catch (SecurityException permissionsChanged) {
            new NightTimeline(context).add(NightTimeline.Event.NOTIFICATION_BLOCKED, day);
            return;
        }

        String value = steps == null ? "indisponibles (" + status + ")" : steps + " pas";
        String summary = "Pas de " + day + " : " + value;
        Notification.Builder builder = new Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Vos pas du " + day)
            .setContentText(summary + " · Récompenses non confirmées.")
            .setStyle(new Notification.BigTextStyle().bigText(summary +
                ". Aucun crédit WeWard, Macadam ou Winwalk n'a été vérifié."))
            .setAutoCancel(true);
        // Informational notification only. Never prompt a daily manual
        // "Tout valider" action: the opt-in mode operates without assistance.
        PendingIntent viewOnly = PendingIntent.getActivity(context, 1301,
            new Intent(context, MainActivity.class),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(viewOnly);
        Notification notification = builder.build();
        try {
            manager.notify(2200, notification);
        } catch (SecurityException permissionsChanged) {
            // The step audit must still succeed if Android revokes the
            // notification permission while the worker is finishing.
            new NightTimeline(context).add(NightTimeline.Event.NOTIFICATION_BLOCKED, day);
        }
    }
}
