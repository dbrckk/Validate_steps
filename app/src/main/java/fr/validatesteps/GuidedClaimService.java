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

    public static boolean connected() { return current != null; }

    public static boolean begin(Context context) {
        GuidedClaimService instance = current;
        if (instance == null) return false;
        new ClaimFlowState(context).start();
        instance.handler.post(() -> instance.launchNext());
        return true;
    }

    public static void cancel(Context context) {
        new ClaimFlowState(context).stop();
        GuidedClaimService instance = current;
        if (instance != null) instance.handler.removeCallbacksAndMessages(null);
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        current = this;
    }

    @Override public void onDestroy() {
        if (current == this) current = null;
        handler.removeCallbacksAndMessages(null);
        new ClaimFlowState(this).stop();
        super.onDestroy();
    }

    @Override public void onInterrupt() {
        new ClaimFlowState(this).stop();
        handler.removeCallbacksAndMessages(null);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        ClaimFlowState state = new ClaimFlowState(this);
        String target = state.target();
        if (target == null || waitingForNext || event == null ||
                event.getPackageName() == null ||
                !target.contentEquals(event.getPackageName())) return;
        if (ProviderPackages.SWEATCOIN.equals(target)) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null ||
                !target.contentEquals(root.getPackageName())) return;

        AccessibilityNodeInfo button = findExplicitAction(root, target, 0);
        if (button != null) {
            // At most one UI click attempt for each app in a single manual run.
            waitingForNext = true;
            boolean performed = button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            state.report(target, performed
                ? "Clic de validation tenté · crédit à vérifier"
                : "Bouton trouvé mais clic refusé");
            handler.postDelayed(() -> finishCurrent(target), 2500L);
        }
    }

    /** Depth is capped, and exact provider-specific labels are mandatory. */
    private AccessibilityNodeInfo findExplicitAction(AccessibilityNodeInfo node,
            String pkg, int depth) {
        if (node == null || depth > 24) return null;
        if (!node.isVisibleToUser() || !node.isEnabled()) return null;
        CharSequence txt = node.getText();
        CharSequence desc = node.getContentDescription();
        if ((txt != null && GuidedClaimPolicy.canClick(pkg, txt.toString())) ||
            (desc != null && GuidedClaimPolicy.canClick(pkg, desc.toString()))) {
            AccessibilityNodeInfo clickable = node;
            for (int i = 0; i < 4 && clickable != null; i++) {
                if (clickable.isClickable() && clickable.isEnabled()
                    && clickable.isVisibleToUser()) return clickable;
                clickable = clickable.getParent();
            }
        }
        int count = Math.min(node.getChildCount(), 80);
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo found =
                findExplicitAction(node.getChild(i), pkg, depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    private void launchNext() {
        ClaimFlowState state = new ClaimFlowState(this);
        if (!state.active()) {
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
        Intent launch = getPackageManager().getLaunchIntentForPackage(target);
        if (launch == null) {
            state.report(target, "Application absente · à installer");
            finishCurrent(target);
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            startActivity(launch);
        } catch (RuntimeException blocked) {
            state.report(target, "Android a bloqué l'ouverture");
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
                    finishCurrent(target);
                }
            }, 14000L);
        }
    }

    private void finishCurrent(String expectedTarget) {
        ClaimFlowState state = new ClaimFlowState(this);
        if (!expectedTarget.equals(state.target())) return;
        waitingForNext = true;
        boolean more = state.advance(expectedTarget);
        if (more) handler.postDelayed(this::launchNext, 650L);
        else {
            state.stop();
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
