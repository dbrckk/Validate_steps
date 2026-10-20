package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class GuidedOutcomeTest {
    @Test public void onlyRealClicksAreCountedAsAttempted() {
        assertTrue(GuidedOutcome.CLICK_ATTEMPTED.clickAttempted());
        for (GuidedOutcome outcome : GuidedOutcome.values())
            if (outcome != GuidedOutcome.CLICK_ATTEMPTED)
                assertFalse(outcome.name(), outcome.clickAttempted());
    }

    @Test public void corruptOrPreviousVersionDataNeverClaimsASuccess() {
        assertEquals(GuidedOutcome.NONE, GuidedOutcome.fromStored(null));
        assertEquals(GuidedOutcome.NONE, GuidedOutcome.fromStored(""));
        assertEquals(GuidedOutcome.NONE, GuidedOutcome.fromStored("PROVIDER_CONFIRMED"));
        assertEquals(GuidedOutcome.CLICK_REFUSED,
            GuidedOutcome.fromStored("CLICK_REFUSED"));
    }

    @Test public void noGuidedOutcomeEverClaimsServerVerifiedRewards() {
        for (GuidedOutcome outcome : GuidedOutcome.values()) {
            assertFalse(outcome.description().toLowerCase().contains("confirmé par"));
            assertFalse(outcome.description().toLowerCase().contains("certifié"));
        }
    }
}
