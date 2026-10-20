package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalDate;
import org.junit.Test;

public final class HistoryRecordPolicyTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Test public void retainsExactlyThirtyLocalDatesIncludingToday() {
        assertTrue(HistoryRecordPolicy.withinRetention(TODAY, TODAY));
        assertTrue(HistoryRecordPolicy.withinRetention(TODAY.minusDays(29), TODAY));
        assertFalse(HistoryRecordPolicy.withinRetention(TODAY.minusDays(30), TODAY));
        assertFalse(HistoryRecordPolicy.withinRetention(TODAY.plusDays(1), TODAY));
        assertFalse(HistoryRecordPolicy.withinRetention(null, TODAY));
        assertFalse(HistoryRecordPolicy.withinRetention(TODAY, null));
    }

    @Test public void providerCreditCanOnlyOriginateFromUserStatements() {
        assertEquals(CreditEvidencePolicy.USER_REPORTED_RECEIVED,
            HistoryRecordPolicy.credit(CreditEvidencePolicy.USER_REPORTED_RECEIVED));
        assertEquals(CreditEvidencePolicy.USER_REPORTED_MISSING,
            HistoryRecordPolicy.credit(CreditEvidencePolicy.USER_REPORTED_MISSING));
        assertEquals(CreditEvidencePolicy.UNKNOWN,
            HistoryRecordPolicy.credit("provider_confirmed"));
        assertEquals(CreditEvidencePolicy.UNKNOWN, HistoryRecordPolicy.credit(null));
    }

    @Test public void rejectUntrustedArbitraryStatusAndNotificationContent() {
        assertEquals("", HistoryRecordPolicy.direct("secret123"));
        assertEquals("", HistoryRecordPolicy.observation("notification private text"));
        assertEquals("audit_failed", HistoryRecordPolicy.audit("secret value"));
        assertEquals("steps_read", HistoryRecordPolicy.audit("steps_read"));
        assertEquals("action_ouvrant_interface",
            HistoryRecordPolicy.observation("action_ouvrant_interface"));
    }

    @Test public void statusesNeverImplyServerConfirmedCredits() {
        assertEquals("", HistoryRecordPolicy.direct("provider_confirmed"));
        assertEquals("action_envoyee_credit_non_confirme",
            HistoryRecordPolicy.direct("action_envoyee_credit_non_confirme"));
        assertEquals("action_annulee", HistoryRecordPolicy.direct("action_annulee"));
    }
}
