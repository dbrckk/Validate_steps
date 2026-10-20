package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class HandsFreeModePolicyTest {
    @Test public void requiresExplicitPermissionAndBothBackgroundSwitches() {
        assertTrue(HandsFreeModePolicy.configured(true, true, true));
        assertFalse(HandsFreeModePolicy.configured(true, true, false));
        assertFalse(HandsFreeModePolicy.configured(true, false, true));
        assertFalse(HandsFreeModePolicy.configured(false, true, true));
    }

    @Test public void configuredModeNeverPromisesProviderCredit() {
        String status = HandsFreeModePolicy.setupDescription(true, true, true, true);
        assertTrue(status.contains("tentées"));
        assertFalse(status.toLowerCase().contains("récompenses validées"));
    }

    @Test public void clearlyShowsMissingListenerAndPermission() {
        assertTrue(HandsFreeModePolicy.setupDescription(true, true, false, false)
            .contains("requis"));
        assertTrue(HandsFreeModePolicy.setupDescription(true, true, true, false)
            .contains("attente de connexion"));
    }

    @Test public void pendingIntentDoesNotProveCredit() {
        String result = HandsFreeModePolicy.providerDescription(true, false,
            "action_envoyee_credit_non_confirme", "", false);
        assertTrue(result.contains("non vérifié"));
        assertFalse(result.contains("Crédit confirmé"));
    }

    @Test public void noDirectActionIsReportedAsNotConfirmed() {
        String result = HandsFreeModePolicy.providerDescription(true, false,
            "", "action_ouvrant_interface", false);
        assertTrue(result.contains("non automatique"));
        assertFalse(result.contains("crédité"));
    }

    @Test public void sweatcoinIsSeparateFromDirectNotifications() {
        String result = HandsFreeModePolicy.providerDescription(true, false,
            "", "", true);
        assertTrue(result.contains("Sweatcoin"));
        assertTrue(result.contains("non vérifiable"));
    }

    @Test public void unknownStatusFailsClosed() {
        String result = HandsFreeModePolicy.providerDescription(true, false,
            "provider_confirmed", "provider_confirmed", false);
        assertTrue(result.contains("Aucune validation directe confirmée"));
    }

    @Test public void userDeclarationIsNotThirdPartyVerification() {
        String result = HandsFreeModePolicy.providerDescription(true, true,
            "", "", false);
        assertTrue(result.contains("déclarée par vous"));
        assertTrue(result.contains("non vérifiée"));
    }
}
