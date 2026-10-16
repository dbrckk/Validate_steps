package fr.validatesteps;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
        NotificationChannel channel = new NotificationChannel(
            CHANNEL, "Suivi autonome des pas", NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(channel);

        String value = steps == null ? "indisponibles (" + status + ")" : steps + " pas";
        String summary = "Pas de " + day + " : " + value;
        Notification notification = new Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Relevé Santé Connect : " + day)
            .setContentText(summary + ". Récompenses non confirmées.")
            .setStyle(new Notification.BigTextStyle().bigText(summary +
                ". Aucun crédit de pièces WeWard, Macadam ou Winwalk n'a été vérifié."))
            .setAutoCancel(true)
            .build();
        manager.notify(2200, notification);
    }
}
