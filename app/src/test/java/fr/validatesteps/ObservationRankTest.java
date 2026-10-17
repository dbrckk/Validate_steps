package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class ObservationRankTest {
    @Test public void untrustedActionOriginIsVisibleButNeverOverridesCompatibleAction() {
        assertEquals(5, ObservationRank.score("origine_action_non_verifiee"));
        assertEquals("origine_action_non_verifiee",
            ObservationRank.strongest("notification_sans_bouton", "origine_action_non_verifiee"));
        assertEquals("action_directe_compatible",
            ObservationRank.strongest("action_directe_compatible", "origine_action_non_verifiee"));
    }

    @Test public void noButtonCannotHidePreviouslyEligibleAction() {
        assertEquals("action_directe_compatible",
            ObservationRank.strongest("action_directe_compatible",
                "notification_sans_bouton"));
    }

    @Test public void openingUiIsMoreInformativeThanNoButton() {
        assertEquals("action_ouvrant_interface",
            ObservationRank.strongest("notification_sans_bouton",
                "action_ouvrant_interface"));
    }

    @Test public void directActionAlwaysWins() {
        assertEquals("action_directe_compatible",
            ObservationRank.strongest("action_avec_saisie",
                "action_directe_compatible"));
    }

    @Test public void noObservationTakesNewClassification() {
        assertEquals("notification_ancienne",
            ObservationRank.strongest("", "notification_ancienne"));
    }
}
