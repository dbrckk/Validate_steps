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

    /** Whether the framework service is currently bound (not merely permitted). */
    public static boolean connected() { return active != null; }

    public static boolean hasPermission(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager != null && manager.isNotificationListenerAccessGranted(
            new ComponentName(context, RewardActionListener.class));
    }

    /**
     * Android may unbind the notification listener when the device restarts
     * or when it reclaims the process. Rebind only with explicit user consent.
     */
    public static void ensureConnected(Context context) {
        if (!new ActionBridgeState(context).enabled() ||
            !hasPermission(context) || active != null) return;
        try {
            requestRebind(new ComponentName(context, RewardActionListener.class));
        } catch (RuntimeException ignored) {
            // Will try again on another scheduled event or system rebind.
        }
    }

    /** Called by alarm and fallback; actions are gated by time and consent. */
    public static void inspectConnectedNow() {
        RewardActionListener instance = active;
        if (instance != null) instance.inspectActiveNotifications(true, false);
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
        instance.inspectActiveNotifications(false, false);
        return true;
    }

    /**
     * Only called synchronously by MainActivity's explicit "Tout valider" tap.
     * Does not enable unattended automation and never opens an Activity.
     * Returns -1 when Android's listener is unavailable, otherwise the number
     * of dispatched provider actions (NOT the number of credited rewards).
     */
    public static int claimDirectNowFromUserTap(Context context) {
        if (!hasPermission(context)) return -1;
        RewardActionListener instance = active;
        if (instance == null) {
            try {
                requestRebind(new ComponentName(context, RewardActionListener.class));
            } catch (RuntimeException ignored) {
                // The user can open the system Notification Access settings.
            }
            return -1;
        }
        new NightTimeline(context).add(NightTimeline.Event.MANUAL_DIRECT_REQUESTED,
            LocalDate.now());
        return instance.inspectActiveNotifications(true, true);
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        active = this;
        inspectActiveNotifications(true, false);
    }

    @Override public void onListenerDisconnected() {
        if (active == this) active = null;
        new ActionBridgeState(this).inspected(false);
        super.onListenerDisconnected();
        ensureConnected(this);
    }

    @Override public void onDestroy() {
        if (active == this) active = null;
        super.onDestroy();
    }

    @Override public void onNotificationPosted(StatusBarNotification notification) {
        if (!new ActionBridgeState(this).enabled()) return;
        inspect(notification, true, false);
    }

    private int inspectActiveNotifications(boolean allowDispatch, boolean onDemand) {
        ActionBridgeState store = new ActionBridgeState(this);
        // Unattended execution requires explicit opt-in. The separate onDemand
        // path is only entered from a real foreground button tap.
        if (allowDispatch && !onDemand && !store.enabled()) return 0;
        int dispatched = 0;
        try {
            StatusBarNotification[] notifications = getActiveNotifications();
            if (notifications != null) {
                int inspectedProviders = 0;
                for (StatusBarNotification item : notifications) {
                    // Filter first: unrelated notification contents are never inspected.
                    if (item == null ||
                            !ClaimActionPolicy.isProvider(item.getPackageName())) continue;
                    if (inspectedProviders >= NotificationScanPolicy.MAX_NOTIFICATIONS) break;
                    inspectedProviders++;
                    if (inspect(item, allowDispatch, onDemand)) dispatched++;
                }
            }
            store.inspected(true);
        } catch (RuntimeException disconnected) {
            store.inspected(false);
        }
        return dispatched;
    }

    private synchronized boolean inspect(StatusBarNotification sbn, boolean allowDispatch, boolean onDemand) {
        if (sbn == null || !ClaimActionPolicy.isProvider(sbn.getPackageName())) return false;
        ActionBridgeState store = new ActionBridgeState(this);
        String packageName = sbn.getPackageName();

        // Do not replay an old notification into a new daily reward window.
        LocalDate day = LocalDate.now();
        LocalDate postedDay = Instant.ofEpochMilli(sbn.getPostTime())
            .atZone(ZoneId.systemDefault()).toLocalDate();
        if (!day.equals(postedDay)) {
            store.noteSeen(packageName, "notification_ancienne");
            return false;
        }

        Notification notification = sbn.getNotification();
        if (notification == null || notification.actions == null ||
            notification.actions.length == 0) {
            store.noteSeen(packageName, "notification_sans_bouton");
            return false;
        }

        String observed = "notification_sans_bouton";
        for (Notification.Action action : notification.actions) {
            if (action == null || action.actionIntent == null) continue;
            PendingIntent pending = action.actionIntent;
            // Android guarantees these creator fields originate from the system.
            // Notifications may embed a PendingIntent delegated by another app:
            // never invoke it unless both package AND UID match the notifier.
            if (!ActionOriginPolicy.trusted(packageName, sbn.getUid(),
                    pending.getCreatorPackage(), pending.getCreatorUid())) {
                observed = ObservationRank.strongest(observed,
                    "origine_action_non_verifiee");
                continue;
            }
            boolean opensActivity = pending.isActivity();
            boolean isBackgroundType = pending.isBroadcast() ||
                pending.isService() || pending.isForegroundService();
            boolean requiresInput = action.getRemoteInputs() != null &&
                action.getRemoteInputs().length > 0;
            String label = StepLabelNormalizer.reasonableLength(action.title)
                ? action.title.toString() : "";
            String diagnostic = ClaimActionPolicy.classify(packageName,
                label, opensActivity, isBackgroundType, requiresInput);
            observed = ObservationRank.strongest(observed, diagnostic);

            if (!"action_directe_compatible".equals(diagnostic)) continue;
            store.noteSeen(packageName, diagnostic);
            if (!allowDispatch || store.alreadyAttempted(packageName, day) ||
                    new ProviderCreditStore(this).userReportedReceivedToday(packageName, day))
                return false;
            if (!onDemand && (!new RunStore(this).enabled() ||
                    !store.enabled() || !ClaimActionPolicy.inTimeWindow(ZonedDateTime.now())))
                return false;
            // Both modes retain the EXACT same security filter. Only the
            // explicitly user-initiated mode relaxes the 22:00 time restriction.
            boolean allowed = onDemand
                ? ClaimActionPolicy.eligibleOnDemand(packageName, label,
                    opensActivity, isBackgroundType, requiresInput)
                : ClaimActionPolicy.eligible(packageName, label,
                    opensActivity, isBackgroundType, requiresInput, ZonedDateTime.now());
            if (!allowed) return false;
            if (!store.reserve(packageName, day)) return false;
            boolean sent = false;
            try {
                pending.send();
                sent = true;
                store.record(packageName, "action_envoyee_credit_non_confirme");
                new NightTimeline(this).addProvider(NightTimeline.Event.CLAIM_ACTION_DISPATCHED, day, packageName);
            } catch (PendingIntent.CanceledException cancelled) {
                store.record(packageName, "action_annulee");
                new NightTimeline(this).addProvider(NightTimeline.Event.CLAIM_ACTION_CANCELED, day, packageName);
            } catch (RuntimeException error) {
                store.record(packageName, "erreur_declenchement");
                new NightTimeline(this).addProvider(NightTimeline.Event.CLAIM_ACTION_ERROR, day, packageName);
            }
            return sent;
        }
        store.noteSeen(packageName, observed);
        return false;
    }
}
