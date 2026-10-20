package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalDate;
import org.junit.Test;

public final class DailyProviderReportPolicyTest {
    private final LocalDate day = LocalDate.of(2026, 10, 20);

    @Test public void sentCommandAndClickAreNotServerCreditEvidence() {
        String text = DailyProviderReportPolicy.render(day, "steps_read",
            new DailyProviderReportPolicy.Entry[] {
                new DailyProviderReportPolicy.Entry("WeWard", true,
                    "action_envoyee_credit_non_confirme",
                    GuidedOutcome.CLICK_ATTEMPTED, CreditEvidencePolicy.UNKNOWN)
            });
        assertTrue(text.contains("Commandes Android envoyées : 1"));
        assertTrue(text.contains("Clics assistés tentés : 1"));
        assertTrue(text.contains("Non vérifié aujourd'hui"));
        assertTrue(text.contains("aucune preuve de crédit côté serveur"));
    }

    @Test public void userDeclarationsRemainExplicitAndIndependent() {
        String text = DailyProviderReportPolicy.render(day, "permission_missing",
            new DailyProviderReportPolicy.Entry[] {
                new DailyProviderReportPolicy.Entry("WeWard", true, "",
                    GuidedOutcome.NONE, CreditEvidencePolicy.USER_REPORTED_RECEIVED),
                new DailyProviderReportPolicy.Entry("Winwalk", true,
                    "action_annulee", GuidedOutcome.CLICK_REFUSED,
                    CreditEvidencePolicy.USER_REPORTED_MISSING),
                new DailyProviderReportPolicy.Entry("Sweatcoin", false, "",
                    GuidedOutcome.NONE, CreditEvidencePolicy.UNKNOWN)
            });
        assertTrue(text.contains("1 reçue(s) · 1 non reçue(s) · 1 non vérifiée(s)"));
        assertTrue(text.contains("Réception déclarée par vous"));
        assertTrue(text.contains("Non-réception déclarée par vous"));
        assertTrue(text.contains("non installée"));
        assertTrue(text.contains("autorisation Santé Connect manquante"));
    }

    @Test public void neverCountsCancelledOrReservedCommandsAsSent() {
        assertFalse(DailyProviderReportPolicy.directSent("action_annulee"));
        assertFalse(DailyProviderReportPolicy.directSent("dispatching"));
        assertFalse(DailyProviderReportPolicy.directSent("erreur_declenchement"));
        assertFalse(DailyProviderReportPolicy.directSent(null));
        assertTrue(DailyProviderReportPolicy.directSent(
            "action_envoyee_credit_non_confirme"));
    }

    @Test public void unknownCodesAreSafeAndNullDateIsHandled() {
        assertTrue(DailyProviderReportPolicy.directDescription("provider_confirmed")
            .contains("Aucune commande"));
        assertTrue(DailyProviderReportPolicy.creditDescription("provider_confirmed")
            .contains("Non vérifié"));
        assertTrue(DailyProviderReportPolicy.render(null, "", null)
            .contains("indisponible"));
        assertTrue(DailyProviderReportPolicy.render(day, "", null)
            .contains("Aucun relevé"));
    }

    @Test public void reportDoesNotIncludeIndividualStepCountOrAccountData() {
        String text = DailyProviderReportPolicy.render(day, "steps_read",
            new DailyProviderReportPolicy.Entry[] {
                new DailyProviderReportPolicy.Entry("Macadam", true, "",
                    GuidedOutcome.BUTTON_MISSING, CreditEvidencePolicy.UNKNOWN)
            });
        assertFalse(text.contains("123456789"));
        assertFalse(text.contains("solde "));
        assertTrue(text.contains("20/10/2026"));
    }
}
