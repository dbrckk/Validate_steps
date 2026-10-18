package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public final class ClaimActionPolicyTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private ZonedDateTime at(int h, int m) {
        return ZonedDateTime.of(2026, 10, 16, h, m, 0, 0, PARIS);
    }

    @Test public void exactStepValidationButtonCanBeDispatchedInBackground() {
        assertTrue(ClaimActionPolicy.eligible(
            "com.weward", "Valider mes pas", false, true, false, at(22, 0)));
        assertTrue(ClaimActionPolicy.eligible(
            "com.macadamapp", "Récupérer mes pas", false, true, false, at(23, 10)));
    }

    @Test public void genericPointsOrCoinsAreNeverClaimedBlindly() {
        for (String action : new String[] {
                "Récupérer mes pièces", "Récupérer mes points",
                "Collecter mes pièces", "Collect coins",
                "Get points", "Collect rewards"}) {
            assertFalse(action,
                ClaimActionPolicy.eligible("com.weward", action,
                    false, true, false, at(22, 10)));
        }
    }

    @Test public void actionMustNotLaunchAnActivity() {
        assertFalse(ClaimActionPolicy.eligible(
            "com.weward", "Valider mes pas", true, false, false, at(22, 0)));
    }

    @Test public void inputActionsAreNotEligible() {
        assertFalse(ClaimActionPolicy.eligible(
            "com.winwalk.android", "Collect step coins", false, true, true, at(22, 0)));
    }

    @Test public void unrelatedButtonNeverSent() {
        assertFalse(ClaimActionPolicy.eligible(
            "com.weward", "Open the app", false, true, false, at(22, 0)));
        assertFalse(ClaimActionPolicy.eligible(
            "com.weward", "Watch ad", false, true, false, at(22, 0)));
    }

    @Test public void unrelatedApplicationNeverSent() {
        assertFalse(ClaimActionPolicy.eligible(
            "com.bank.app", "Valider mes pas", false, true, false, at(22, 0)));
    }

    @Test public void oneTimeWindowOnly() {
        assertFalse(ClaimActionPolicy.eligible(
            "com.weward", "Valider mes pas", false, true, false, at(21, 59)));
        assertFalse(ClaimActionPolicy.eligible(
            "com.weward", "Valider mes pas", false, true, false, at(23, 40)));
    }

    @Test public void normalizedAccentAndWhitespace() {
        assertTrue(ClaimActionPolicy.eligible(
            "com.macadamapp", "  RÉCUPÉRER    MES PAS ", false, true, false, at(22, 5)));
    }

    @Test public void unknownPendingIntentTypeMustNeverDispatch() {
        assertFalse(ClaimActionPolicy.eligible(
            "com.weward", "Valider mes pas", false, false, false, at(22, 0)));
    }

    @Test public void missingProviderPackageFailsClosedWithoutException() {
        assertFalse(ClaimActionPolicy.isProvider(null));
        assertFalse(ClaimActionPolicy.eligible(null, "Valider mes pas", false,
            true, false, at(22, 0)));
    }

    @Test public void sweatcoinNotTouchedByNotificationBridge() {
        assertFalse(ClaimActionPolicy.isProvider("in.sweatco.app"));
    }
}
