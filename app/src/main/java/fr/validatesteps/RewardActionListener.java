package fr.validatesteps;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Optional notification-action bridge.
 *
 * Only sends a third-party notification's provider-created, non-Activity,
 * exact step-claim action after the user opts in and grants Notification
 * Access. No credentials, private APIs, UI taps or fake steps.
 *
 * A PendingIntent send merely means DISPATCHED; the provider may reject the
 * claim and no reward is declared credited without provider proof.
 */
public final class RewardActionListener extends NotificationListenerService {
    private static volatile RewardActionListener active;

    public static boolean hasPermission(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager.isNotificationListenerAccessGranted(
            new ComponentName(context, RewardActionListener.class));
    }

    /** Best-effort immediate scan from our 22:00 alarm / fallback workers. */
    public static void inspectConnectedNow() {
        RewardActionListener instance = active;
        if (instance != null) instance.inspectActiveNotifications();
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        active = this;
        inspectActiveNotifications();
    }

    @Override public void onListenerDisconnected() {
        if (active == this) active = null;
        super.onListenerDisconnected();
    }

    @Override public void onDestroy() {
        if (active == this) active = null;
        super.onDestroy();
    }

    @Override public void onNotificationPosted(StatusBarNotification notification) {
        inspect(notification);
    }

    private void inspectActiveNotifications() {
        if (!new ActionBridgeState(this).enabled()) return;
        if (!ClaimActionPolicy.inTimeWindow(ZonedDateTime.now())) return;
        try {
            StatusBarNotification[] notifications = getActiveNotifications();
            if (notifications != null) {
                for (StatusBarNotification notification : notifications) {
                    inspect(notification);
                }
            }
            new ActionBridgeState(this).inspected();
        } catch (RuntimeException ignored) {
            // Listener disconnected during a scan or system temporarily unavailable.
        }
    }

    private synchronized void inspect(StatusBarNotification sbn) {
        if (sbn == null || !ClaimActionPolicy.isProvider(sbn.getPackageName()) ||
            !ClaimActionPolicy.inTimeWindow(ZonedDateTime.now())) return;

        // Never trigger an old notification re-shown after a day boundary.
        LocalDate postedDay = Instant.ofEpochMilli(sbn.getPostTime())
            .atZone(ZoneId.systemDefault()).toLocalDate();
        if (!LocalDate.now().equals(postedDay)) return;

        ActionBridgeState store = new ActionBridgeState(this);
        if (!store.enabled() || store.alreadyAttempted(
                sbn.getPackageName(), LocalDate.now())) return;

        Notification notification = sbn.getNotification();
        if (notification == null || notification.actions == null) return;
        for (Notification.Action action : notification.actions) {
            if (action == null || action.actionIntent == null) continue;
            boolean activity = action.actionIntent.isActivity();
            boolean backgroundType = action.actionIntent.isBroadcast() ||
                action.actionIntent.isService() ||
                action.actionIntent.isForegroundService();
            if (!backgroundType) continue;
            boolean input = action.getRemoteInputs() != null &&
                action.getRemoteInputs().length > 0;
            if (!ClaimActionPolicy.eligible(sbn.getPackageName(),
                    action.title == null ? "" : action.title.toString(),
                    activity, input, ZonedDateTime.now())) continue;

            LocalDate day = LocalDate.now();
            if (!store.reserve(sbn.getPackageName(), day)) return;
            try {
                action.actionIntent.send();
                store.record(sbn.getPackageName(), "action_envoyee_non_confirmee");
            } catch (PendingIntent.CanceledException cancelled) {
                store.record(sbn.getPackageName(), "action_annulee");
            } catch (RuntimeException problem) {
                store.record(sbn.getPackageName(), "erreur_envoi");
            }
            return; // At most ONE attempt per provider per day.
        }
    }
}
