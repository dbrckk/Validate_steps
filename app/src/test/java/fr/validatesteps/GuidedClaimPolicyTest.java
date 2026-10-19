package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class GuidedClaimPolicyTest {
    @Test public void exactStepActionsAreRecognized() {
        assertTrue(GuidedClaimPolicy.canClick("com.weward", "Valider mes pas"));
        assertTrue(GuidedClaimPolicy.canClick("com.macadamapp", " VALIDER  MES PAS DU JOUR "));
        assertTrue(GuidedClaimPolicy.canClick("com.winwalk.android", "Collect step coins"));
    }

    @Test public void rejectsGenericCoinsAdsAndAccountActions() {
        for (String label : new String[] {
            "Collect coins", "Récupérer mes pièces", "Valider",
            "Watch ad", "Confirmer", "Connexion", "Continuer", "Collect bonus",
            "Acheter", "Réclamer"
        }) {
            assertFalse(label, GuidedClaimPolicy.canClick("com.weward", label));
            assertFalse(label, GuidedClaimPolicy.canClick("com.macadamapp", label));
            assertFalse(label, GuidedClaimPolicy.canClick("com.winwalk.android", label));
        }
    }

    @Test public void onlyInstalledRewardAppsAreTargets() {
        assertTrue(GuidedClaimPolicy.isTarget("in.sweatco.app"));
        assertFalse(GuidedClaimPolicy.isTarget("com.bank.app"));
        assertFalse(GuidedClaimPolicy.isTarget(null));
    }

    @Test public void sweatcoinNeverClicksUnknownButtons() {
        assertFalse(GuidedClaimPolicy.canClick("in.sweatco.app", "Claim steps"));
    }

    @Test public void noUnknownOrNullLabelIsClickable() {
        assertFalse(GuidedClaimPolicy.canClick(null, "Valider mes pas"));
        assertFalse(GuidedClaimPolicy.canClick("com.weward", null));
        assertFalse(GuidedClaimPolicy.canClick("com.weward", "Valider mes pas et voir publicité"));
    }
}
