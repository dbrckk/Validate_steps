package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class UnattendedModePolicyTest {
    @Test public void directActionsRequireBothOptInAndAndroidPermission() {
        assertTrue(UnattendedModePolicy.canArmDirectActions(true, true));
        assertFalse(UnattendedModePolicy.canArmDirectActions(true, false));
        assertFalse(UnattendedModePolicy.canArmDirectActions(false, true));
        assertFalse(UnattendedModePolicy.canArmDirectActions(false, false));
    }

    @Test public void noGenericOrActivityActionCanPretendToValidate() {
        assertTrue(UnattendedModePolicy.providerState(
            "com.weward", true, "action_ouvrant_interface", "")
            .contains("manuelle requise"));
        assertTrue(UnattendedModePolicy.providerState(
            "com.macadamapp", true, "libelle_de_recompense_ambigu", "")
            .contains("non exécuté"));
        assertTrue(UnattendedModePolicy.providerState(
            "com.winwalk.android", true, "notification_sans_bouton", "")
            .contains("manuelle requise"));
    }

    @Test public void directIntentDoesNotEqualCreditedRewards() {
        String status = UnattendedModePolicy.providerState("com.weward", true,
            "action_directe_compatible", "action_envoyee_credit_non_confirme");
        assertTrue(status.contains("crédit à vérifier"));
        assertFalse(status.contains("crédit confirmé"));
    }

    @Test public void observedButtonIsNotConfirmedFutureCredit() {
        assertTrue(UnattendedModePolicy.providerState("com.weward", true,
            "action_directe_compatible", "").contains("crédit non garanti"));
    }

    @Test public void sweatcoinRunsIndependentlyOfThisCompanion() {
        assertTrue(UnattendedModePolicy.providerState("in.sweatco.app", false,
            "", "").contains("par Sweatcoin"));
    }

    @Test public void failsClosedForUnrecognizedProvidersAndCodes() {
        assertTrue(UnattendedModePolicy.providerState("com.fake", true,
            "action_directe_compatible", "").contains("non pris en charge"));
        assertTrue(UnattendedModePolicy.providerState("com.weward", true,
            "nonsense", "provider_confirmed").contains("non garantie"));
    }
}
