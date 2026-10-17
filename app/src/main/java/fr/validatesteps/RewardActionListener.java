package fr.validatesteps;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Opt-in notification diagnostics and direct, provider-created background actions.
 * No message text, passwords, advertising or account data are ever recorded.
 * A sent PendingIntent does NOT prove reward settlement in the provider account.
 */
public final class RewardActionListener extends NotificationListenerService {
    private static volatile RewardActionListener active;

    public static boolean hasPermission(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager != null && manager.isNotificationListenerAccessGranted(
            new ComponentName(context, RewardActionListener.class));
    }

    /** Called by alarm and fallback; actions are gated by time and consent. */
    public static void inspectConnectedNow() {
        RewardActionListener instance = active;
        if (instance != null) instance.inspectActiveNotifications(true);
    }

    /**
     * A manual diagnostics scan NEVER triggers a third-party PendingIntent.
     * Reports whether the listener is connected and collects classifications only.
     */
    public static boolean diagnoseConnectedNow(Context context) {
        RewardActionListener instance = active;
        if (instance == null) {
            new ActionBridgeState(context).inspected(false);
            return false;
        }
        instance.inspectActiveNotifications(false);
        return true;
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        active = this;
        inspectActiveNotifications(true);
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
        inspect(notification, true);
    }

    private void inspectActiveNotifications(boolean allowDispatch) {
        ActionBridgeState store = new ActionBridgeState(this);
        try {
            StatusBarNotification[] notifications = getActiveNotifications();
            if (notifications != null) {
                for (StatusBarNotification item : notifications) {
                    inspect(item, allowDispatch);
                }
            }
            store.inspected(true);
        } catch (RuntimeException disconnected) {
            store.inspected(false);
        }
    }

    private synchronized void inspect(StatusBarNotification sbn, boolean allowDispatch) {
        if (sbn == null || !ClaimActionPolicy.isProvider(sbn.getPackageName())) return;
        ActionBridgeState store = new ActionBridgeState(this);
        String packageName = sbn.getPackageName();

        // Do not replay an old notification into a new daily reward window.
        LocalDate day = LocalDate.now();
        LocalDate postedDay = Instant.ofEpochMilli(sbn.getPostTime())
            .atZone(ZoneId.systemDefault()).toLocalDate();
        if (!day.equals(postedDay)) {
            store.noteSeen(packageName, "notification_ancienne");
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null || notification.actions == null ||
            notification.actions.length == 0) {
            store.noteSeen(packageName, "notification_sans_bouton");
            return;
        }

        String observed = "notification_sans_bouton";
        for (Notification.Action action : notification.actions) {
            if (action == null || action.actionIntent == null) continue;
            PendingIntent pending = action.actionIntent;
            boolean opensActivity = pending.isActivity();
            boolean isBackgroundType = pending.isBroadcast() ||
                pending.isService() || pending.isForegroundService();
            boolean requiresInput = action.getRemoteInputs() != null &&
                action.getRemoteInputs().length > 0;
            String label = action.title == null ? "" : action.title.toString();
            String diagnostic = ClaimActionPolicy.classify(packageName,
                label, opensActivity, isBackgroundType, requiresInput);
            observed = diagnostic;

            if (!"action_directe_compatible".equals(diagnostic)) continue;
            store.noteSeen(packageName, diagnostic);
            if (!allowDispatch || !new RunStore(this).enabled() ||
                !store.enabled() || !ClaimActionPolicy.inTimeWindow(ZonedDateTime.now()) ||
                store.alreadyAttempted(packageName, day)) return;
            // Same strict label test that is exercised in the unit tests.
            if (!ClaimActionPolicy.eligible(packageName, label,
                    opensActivity, requiresInput, ZonedDateTime.now())) return;
            if (!store.reserve(packageName, day)) return;
            try {
                pending.send();
                store.record(packageName, "action_envoyee_credit_non_confirme");
            } catch (PendingIntent.CanceledException cancelled) {
                store.record(packageName, "action_annulee");
            } catch (RuntimeException error) {
                store.record(packageName, "erreur_declenchement");
            }
            return;
        }
        store.noteSeen(packageName, observed);
    }
}
