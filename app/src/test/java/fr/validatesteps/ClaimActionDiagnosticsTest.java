package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class ClaimActionDiagnosticsTest {
    @Test public void recognizesRealDirectButton() {
        assertEquals("action_directe_compatible",
            ClaimActionPolicy.classify("com.weward", "Valider mes pas",
                false, true, false));
    }

    @Test public void anActivityIsNotBackground() {
        assertEquals("action_ouvrant_interface",
            ClaimActionPolicy.classify("com.weward", "Valider mes pas",
                true, false, false));
    }

    @Test public void noBlindlyAcceptedTextReplies() {
        assertEquals("action_avec_saisie",
            ClaimActionPolicy.classify("com.macadamapp", "Valider les pas",
                false, true, true));
    }

    @Test public void unknownActionTypeIsRejected() {
        assertEquals("type_action_non_pris_en_charge",
            ClaimActionPolicy.classify("com.weward", "Valider mes pas",
                false, false, false));
    }

    @Test public void openingNotificationsDoNotPretendToClaim() {
        assertEquals("libelle_action_non_reconnu",
            ClaimActionPolicy.classify("com.weward", "Ouvrir",
                false, true, false));
    }

    @Test public void genericCoinLabelIsExplicitlyFlaggedAsAmbiguous() {
        assertEquals("libelle_de_recompense_ambigu",
            ClaimActionPolicy.classify("com.macadamapp", "RÉCUPÉRER MES PIÈCES",
                false, true, false));
        assertEquals("action_directe_compatible",
            ClaimActionPolicy.classify("com.winwalk.android", "Collect step coins",
                false, true, false));
    }

    @Test public void unknownPackageIsNotInspected() {
        assertEquals("service_non_pris_en_charge",
            ClaimActionPolicy.classify("com.example.finance", "Valider mes pas",
                false, true, false));
    }
}
