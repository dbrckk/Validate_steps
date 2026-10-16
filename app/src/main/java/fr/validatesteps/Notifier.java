package fr.validatesteps;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/** A truthful optional result notice. No provider rewards are asserted. */
public final class Notifier {
    private static final String CHANNEL = "step_audit";
    private Notifier() {}

    public static void show(Context context, Long steps, String status) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
            CHANNEL, "Suivi autonome des pas", NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(channel);

        String value = steps == null ? "indisponible (" + status + ")" : steps + " pas";
        Notification notification = new Notification.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Relevé de 22 h : " + value)
            .setContentText("Récompenses WeWard, Macadam et Winwalk non confirmées")
            .setStyle(new Notification.BigTextStyle().bigText(
                "Pas réels Santé Connect : " + value + ". Ceci ne valide pas les récompenses " +
                "dans les applications tierces, qui peuvent exiger une action manuelle."))
            .setAutoCancel(true)
            .build();
        manager.notify(2200, notification);
    }
}
