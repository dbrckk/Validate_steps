package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalDate;
import org.junit.Test;

public final class CreditEvidencePolicyTest {
    private final LocalDate today = LocalDate.of(2026, 10, 20);

    @Test public void noProviderCreditIsAutomaticallyConfirmed() {
        assertEquals("Crédit non vérifié aujourd'hui",
            CreditEvidencePolicy.visibleStatus("", today, CreditEvidencePolicy.UNKNOWN));
        assertFalse(CreditEvidencePolicy.userReportedReceivedToday(
            "", today, CreditEvidencePolicy.UNKNOWN));
    }

    @Test public void explicitUserConfirmationIsNotMisrepresentedAsServerProof() {
        String text = CreditEvidencePolicy.visibleStatus(
            today.toString(), today, CreditEvidencePolicy.USER_REPORTED_RECEIVED);
        assertTrue(text.contains("Vous avez déclaré"));
        assertTrue(CreditEvidencePolicy.userReportedReceivedToday(today.toString(), today,
            CreditEvidencePolicy.USER_REPORTED_RECEIVED));
    }

    @Test public void missingCreditCanBeRecordedWithoutFalseSuccess() {
        assertTrue(CreditEvidencePolicy.visibleStatus(today.toString(), today,
            CreditEvidencePolicy.USER_REPORTED_MISSING).contains("ne pas avoir reçu"));
        assertFalse(CreditEvidencePolicy.userReportedReceivedToday(today.toString(), today,
            CreditEvidencePolicy.USER_REPORTED_MISSING));
    }

    @Test public void yesterdayAndArbitraryStatusAreNeverCreditedToday() {
        assertFalse(CreditEvidencePolicy.userReportedReceivedToday(
            today.minusDays(1).toString(), today, CreditEvidencePolicy.USER_REPORTED_RECEIVED));
        assertEquals("Crédit non vérifié aujourd'hui",
            CreditEvidencePolicy.visibleStatus(today.minusDays(1).toString(),
                today, CreditEvidencePolicy.USER_REPORTED_RECEIVED));
        assertFalse(CreditEvidencePolicy.validStatus("provider_confirmed"));
        assertFalse(CreditEvidencePolicy.validStatus(null));
        assertFalse(CreditEvidencePolicy.userReportedReceivedToday(
            today.toString(), today, "provider_confirmed"));
    }
}
