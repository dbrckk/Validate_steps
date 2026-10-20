package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class AutonomousModePolicyTest {
    @Test public void neverClaimsProviderCreditFromNotificationDispatch() {
        String message = AutonomousModePolicy.providerStatus(true, false,
            true, true, true, "action_envoyee_credit_non_confirme",
            "action_directe_compatible", false);
        assertTrue(message.contains("non vérifiée"));
        assertFalse(message.contains("récompense créditée"));
    }

    @Test public void providersWithoutDirectActionsStayExplicitlyUnavailable() {
        assertTrue(AutonomousModePolicy.providerStatus(true, false,
            true, true, true, "", "action_ouvrant_interface", false)
            .contains("indisponible"));
        assertTrue(AutonomousModePolicy.providerStatus(true, false,
            true, true, true, "", "", false)
            .contains("Aucune action directe"));
        assertTrue(AutonomousModePolicy.providerStatus(true, false,
            true, true, true, "", "libelle_de_recompense_ambigu", false)
            .contains("ignorée"));
    }

    @Test public void noAccessibilityOrClickPermissionRequiredForAutonomousSetup() {
        assertTrue(AutonomousModePolicy.autonomousReady(true, true, true, true));
        assertFalse(AutonomousModePolicy.autonomousReady(true, false, true, true));
        assertFalse(AutonomousModePolicy.autonomousReady(true, true, false, true));
        assertFalse(AutonomousModePolicy.autonomousReady(true, true, true, false));
    }

    @Test public void sweatcoinUsesNativeBackgroundConversion() {
        assertTrue(AutonomousModePolicy.providerStatus(true, true,
            true, false, false, "", "", false).contains("Sweatcoin"));
        assertTrue(AutonomousModePolicy.providerStatus(false, true,
            true, true, true, "", "", false).contains("non installée"));
    }

    @Test public void creditsMarkedByUserRemainOnlyUserStatements() {
        assertTrue(AutonomousModePolicy.providerStatus(true, false,
            true, true, true, "", "", true).contains("déclarée"));
    }
}
