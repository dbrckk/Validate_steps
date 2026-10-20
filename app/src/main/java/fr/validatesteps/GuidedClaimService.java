package fr.validatesteps;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Explicitly user-started, deterministic UI assistance.
 *
 * It opens the real provider apps; it is NOT a headless/background claim.
 * Only exact, step-related labels are clicked. No coordinates, ads, login,
 * captchas, account operations, network requests or screenshots.
 *
 * A CLICK_ATTEMPT is not a reward settlement confirmation.
 */
public final class GuidedClaimService extends AccessibilityService {
    private static volatile GuidedClaimService current;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean waitingForNext;
    // In-memory only: a process restart must NEVER resurrect a user-initiated
    // Accessibility workflow from the persisted state of an earlier session.
    private boolean sessionArmed;
    private long previousScanMs;

    public static boolean connected() { return current != null; }

    public static boolean begin(Context context) {
        GuidedClaimService instance = current;
        if (instance == null || new ClaimFlowState(context).active()) return false;
        instance.handler.removeCallbacksAndMessages(null);
        instance.waitingForNext = false;
        instance.sessionArmed = true;
        new ClaimFlowState(context).start();
        new NightTimeline(context).add(NightTimeline.Event.GUIDED_STARTED,
            java.time.LocalDate.now());
        instance.handler.post(instance::launchNext);
        return true;
    }

    public static void cancel(Context context) {
        ClaimFlowState state = new ClaimFlowState(context);
        String target = state.target();
        if (target != null) {
            state.report(target, "Parcours interrompu · validation non confirmée");
            new NightTimeline(context).add(NightTimeline.Event.GUIDED_CANCELED,
                java.time.LocalDate.now());
        }
        state.stop();
        GuidedClaimService instance = current;
        if (instance != null) {
            instance.sessionArmed = false;
            instance.waitingForNext = true;
            instance.handler.removeCallbacksAndMessages(null);
        }
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        // An older SharedPreferences session does not prove a *current*
        // user gesture after Android reconnects or restarts this service.
        handler.removeCallbacksAndMessages(null);
        waitingForNext = false;
        sessionArmed = false;
        new ClaimFlowState(this).stop();
        current = this;
    }

    @Override public void onDestroy() {
        sessionArmed = false;
        if (current == this) current = null;
        handler.removeCallbacksAndMessages(null);
        new ClaimFlowState(this).stop();
        super.onDestroy();
    }

    @Override public void onInterrupt() {
        sessionArmed = false;
        new ClaimFlowState(this).stop();
        handler.removeCallbacksAndMessages(null);
    }

    @Override public boolean onUnbind(Intent intent) {
        sessionArmed = false;
        if (current == this) current = null;
        handler.removeCallbacksAndMessages(null);
        new ClaimFlowState(this).stop();
        return super.onUnbind(intent);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        ClaimFlowState state = new ClaimFlowState(this);
        String target = state.target();
        if (!sessionArmed || target == null || waitingForNext || event == null ||
                event.getPackageName() == null ||
                !target.contentEquals(event.getPackageName())) return;
        if (ProviderPackages.SWEATCOIN.equals(target)) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (now - previousScanMs < 300) return;
        previousScanMs = now;
        // A third-party UI can mutate or detach during inspection. Never let
        // its dynamic accessibility tree crash our service on the main thread.
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null || root.getPackageName() == null ||
                    !target.contentEquals(root.getPackageName())) return;
            AccessibilityNodeInfo button = findExplicitAction(root, target, 0,
                new AccessibilityScanBudget());
            if (button != null) {
                // At most one UI click attempt for each app in a single manual run.
                waitingForNext = true;
                boolean performed;
                try {
                    performed = button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                } catch (RuntimeException detachedNode) {
                    performed = false;
                }
                state.report(target, performed
                    ? "Clic de validation tenté · crédit à vérifier"
                    : "Bouton trouvé mais clic refusé");
                new NightTimeline(this).addProvider(performed
                    ? NightTimeline.Event.GUIDED_CLICK_ATTEMPTED
                    : NightTimeline.Event.GUIDED_CLICK_REFUSED, java.time.LocalDate.now(), target);
                handler.postDelayed(() -> finishCurrent(target), 2500L);
            }
        } catch (RuntimeException staleUiTree) {
            // Ignore transient third-party view changes. The bounded retry
            // window will report a missing button if no stable view appears.
        }
    }

    /** Both depth and TOTAL visited node count are capped per event. */
    private AccessibilityNodeInfo findExplicitAction(AccessibilityNodeInfo node,
            String pkg, int depth, AccessibilityScanBudget budget) {
        if (node == null || !budget.visit(depth)) return null;
        if (!node.isVisibleToUser() || !node.isEnabled()) return null;
        CharSequence sourcePackage = node.getPackageName();
        if (sourcePackage != null && !pkg.contentEquals(sourcePackage)) return null;
        CharSequence txt = node.getText();
        CharSequence desc = node.getContentDescription();
        if ((StepLabelNormalizer.reasonableLength(txt)
                && GuidedClaimPolicy.canClick(pkg, txt.toString())) ||
            (StepLabelNormalizer.reasonableLength(desc)
                && GuidedClaimPolicy.canClick(pkg, desc.toString()))) {
            AccessibilityNodeInfo clickable = node;
            for (int i = 0;
                    i <= GuidedClickSafetyPolicy.MAX_PARENT_HOPS && clickable != null; i++) {
                CharSequence owner = clickable.getPackageName();
                if (GuidedClickSafetyPolicy.permitted(pkg,
                        owner == null ? null : owner.toString(),
                        i, clickable.isClickable(), clickable.isEnabled(),
                        clickable.isVisibleToUser())) return clickable;
                // Do not cross into a view owned by Android or another app.
                if (owner == null || !pkg.contentEquals(owner)) return null;
                clickable = clickable.getParent();
            }
        }
        int count = Math.min(node.getChildCount(), AccessibilityScanBudget.MAX_CHILDREN);
        for (int i = 0; i < count && !budget.exhausted(); i++) {
            AccessibilityNodeInfo found =
                findExplicitAction(node.getChild(i), pkg, depth + 1, budget);
            if (found != null) return found;
        }
        return null;
    }

    private void launchNext() {
        ClaimFlowState state = new ClaimFlowState(this);
        if (!sessionArmed || !state.active()) {
            endOnDashboard();
            return;
        }
        waitingForNext = false;
        String target = state.target();
        if (target == null) {
            state.stop();
            endOnDashboard();
            return;
        }
        if (new ProviderCreditStore(this).userReportedReceivedToday(
                target, java.time.LocalDate.now())) {
            // A user already checked the actual provider account and said
            // the rewards were received: do not click them again today.
            state.report(target, "Déjà déclaré reçu par vous · aucun nouveau clic");
            finishCurrent(target);
            return;
        }
        if (new ActionBridgeState(this).dispatchedToday(
                target, java.time.LocalDate.now())) {
            // Avoid a second claim attempt when an exact provider notification
            // command was already sent earlier today (not yet settlement proof).
            state.report(target, "Action directe déjà envoyée · crédit à vérifier");
            finishCurrent(target);
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(target);
        if (launch == null) {
            state.report(target, "Application absente · à installer");
            new NightTimeline(this).addProvider(NightTimeline.Event.GUIDED_APP_ABSENT,
                java.time.LocalDate.now(), target);
            finishCurrent(target);
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(launch);
        } catch (RuntimeException blocked) {
            state.report(target, "Android a bloqué l'ouverture");
            new NightTimeline(this).addProvider(NightTimeline.Event.GUIDED_OPEN_BLOCKED,
                java.time.LocalDate.now(), target);
            finishCurrent(target);
            return;
        }
        if (ProviderPackages.SWEATCOIN.equals(target)) {
            state.report(target, "Ouverte pour synchronisation · crédit non confirmé");
            handler.postDelayed(() -> finishCurrent(target), 4200L);
        } else {
            state.report(target, "Recherche du bouton de pas en cours…");
            handler.postDelayed(() -> {
                if (target.equals(new ClaimFlowState(this).target()) && !waitingForNext) {
                    new ClaimFlowState(this).report(target,
                        "Bouton direct non trouvé · validation manuelle requise");
                    new NightTimeline(this).addProvider(NightTimeline.Event.GUIDED_BUTTON_MISSING,
                        java.time.LocalDate.now(), target);
                    finishCurrent(target);
                }
            }, 14000L);
        }
    }

    private void finishCurrent(String expectedTarget) {
        ClaimFlowState state = new ClaimFlowState(this);
        if (!sessionArmed || !expectedTarget.equals(state.target())) return;
        waitingForNext = true;
        boolean more = state.advance(expectedTarget);
        if (more) handler.postDelayed(this::launchNext, 650L);
        else {
            sessionArmed = false;
            state.stop();
            new NightTimeline(this).add(NightTimeline.Event.GUIDED_FINISHED,
                java.time.LocalDate.now());
            handler.postDelayed(this::endOnDashboard, 600L);
        }
    }

    private void endOnDashboard() {
        // This mode was explicitly initiated by the user; bring back the results.
        try {
            Intent home = new Intent(this, MainActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
        } catch (RuntimeException ignored) {
            // Android can prevent background activity launches; recorded results remain.
        }
    }
}
