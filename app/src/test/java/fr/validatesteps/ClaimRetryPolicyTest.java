package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalDate;
import org.junit.Test;

public final class ClaimRetryPolicyTest {
    private final LocalDate date = LocalDate.of(2026, 10, 17);

    @Test public void newDayAllowsOneAttempt() {
        assertTrue(ClaimRetryPolicy.allowed(date, "2026-10-16",
            "action_envoyee_credit_non_confirme", 1));
    }

    @Test public void failedCanceledIntentCanBeRetriedTwice() {
        assertTrue(ClaimRetryPolicy.allowed(date, date.toString(), "action_annulee", 1));
        assertTrue(ClaimRetryPolicy.allowed(date, date.toString(), "action_annulee", 2));
        assertFalse(ClaimRetryPolicy.allowed(date, date.toString(), "action_annulee", 3));
    }

    @Test public void ambiguousOrDispatchedAttemptNeverRetried() {
        for (String status : new String[] {
                "dispatching", "action_envoyee_credit_non_confirme", "erreur_declenchement"}) {
            assertFalse(ClaimRetryPolicy.allowed(date, date.toString(), status, 1));
        }
    }

    @Test public void noDayNeverAllowed() {
        assertFalse(ClaimRetryPolicy.allowed(null, date.toString(), "action_annulee", 1));
    }
}
