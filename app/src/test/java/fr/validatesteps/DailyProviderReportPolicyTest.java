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

    @Test public void notificationObservationsExplainWhyNoAutomaticActionWasSent() {
        String text = DailyProviderReportPolicy.render(day, "",
            new DailyProviderReportPolicy.Entry[] {
                new DailyProviderReportPolicy.Entry("WeWard", true, "",
                    "action_ouvrant_interface", GuidedOutcome.NONE,
                    CreditEvidencePolicy.UNKNOWN),
                new DailyProviderReportPolicy.Entry("Macadam", true, "",
                    "libelle_de_recompense_ambigu", GuidedOutcome.BUTTON_MISSING,
                    CreditEvidencePolicy.UNKNOWN),
                new DailyProviderReportPolicy.Entry("Winwalk", true, "",
                    "origine_action_non_verifiee", GuidedOutcome.NONE,
                    CreditEvidencePolicy.UNKNOWN),
                new DailyProviderReportPolicy.Entry("Sweatcoin", true, "",
                    "", GuidedOutcome.SWEATCOIN_OPENED, CreditEvidencePolicy.UNKNOWN)
            });
        assertTrue(text.contains("Action ouvrant une interface"));
        assertTrue(text.contains("Libellé de récompense ambigu"));
        assertTrue(text.contains("origine Android non vérifiée"));
        assertTrue(text.contains("Sweatcoin ouvert pour synchronisation"));
        assertTrue(text.contains("4 non vérifiée(s)"));
        assertFalse(text.contains("Déclarations : 4 reçue(s)"));
    }

    @Test public void laterCancellationCannotEraseEarlierRealClickAttempt() {
        String report = DailyProviderReportPolicy.render(day, "steps_read",
            new DailyProviderReportPolicy.Entry[] {
                new DailyProviderReportPolicy.Entry("WeWard", true,
                    "", "notification_sans_bouton", GuidedOutcome.CANCELED,
                    CreditEvidencePolicy.UNKNOWN, true)
            });
        assertTrue(report.contains("Clics assistés tentés : 1"));
        assertTrue(report.contains("clic avait aussi été tenté plus tôt"));
        assertTrue(report.contains("Non vérifié aujourd'hui"));
    }

    @Test public void missingButtonNeverCountsAsGuidedClick() {
        String report = DailyProviderReportPolicy.render(day, "",
            new DailyProviderReportPolicy.Entry[] {
                new DailyProviderReportPolicy.Entry("Macadam", true, "",
                    "notification_sans_bouton", GuidedOutcome.BUTTON_MISSING,
                    CreditEvidencePolicy.UNKNOWN, false)
            });
        assertTrue(report.contains("Clics assistés tentés : 0"));
        assertFalse(report.contains("clic avait aussi été tenté"));
    }

    @Test public void unexpectedObservationCodesDoNotLeakTextIntoDailyReport() {
        String hostile = "CUSTOM_PASSWORD_OR_NOTIFICATION_BODY_123";
        String text = DailyProviderReportPolicy.render(day, "",
            new DailyProviderReportPolicy.Entry[] {
                new DailyProviderReportPolicy.Entry("WeWard", true, "", hostile,
                    GuidedOutcome.NONE, CreditEvidencePolicy.UNKNOWN)
            });
        assertFalse(text.contains(hostile));
        assertTrue(text.contains("Aucun bouton analysé"));
    }

    @Test public void historicReportDoesNotClaimOldInstallationState() {
        String old = DailyProviderReportPolicy.renderArchive(day,
            "steps_read", new DailyProviderReportPolicy.Entry[] {
                new DailyProviderReportPolicy.Entry(
                    "WeWard", false, "action_envoyee_credit_non_confirme",
                    "notification_sans_bouton", GuidedOutcome.CANCELED,
                    CreditEvidencePolicy.USER_REPORTED_RECEIVED, true)
            });
        assertTrue(old.contains("BILAN DU 20/10/2026"));
        assertTrue(old.contains("Réception déclarée par vous"));
        assertTrue(old.contains("Clics assistés tentés : 1"));
        assertFalse(old.contains("non installée"));
        assertFalse(old.contains("installée"));
        assertTrue(old.contains("aucune preuve de crédit côté serveur"));
    }

    @Test public void archiveDoesNotInferRewardSettlementFromCommandsAlone() {
        String old = DailyProviderReportPolicy.renderArchive(day, "",
            new DailyProviderReportPolicy.Entry[] {
                new DailyProviderReportPolicy.Entry(
                    "Macadam", false, "action_envoyee_credit_non_confirme",
                    GuidedOutcome.CLICK_ATTEMPTED, CreditEvidencePolicy.UNKNOWN)
            });
        assertTrue(old.contains("Commandes Android envoyées : 1"));
        assertTrue(old.contains("Clics assistés tentés : 1"));
        assertTrue(old.contains("Non vérifié aujourd'hui"));
        assertTrue(old.contains("aucune preuve de crédit côté serveur"));
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
