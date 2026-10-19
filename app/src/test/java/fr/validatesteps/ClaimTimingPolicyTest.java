package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalTime;
import org.junit.Test;

public final class ClaimTimingPolicyTest {
    @Test public void manualClaimsBefore22MayExcludeLaterSteps() {
        assertTrue(ClaimTimingPolicy.risksPartialDayClaim(LocalTime.of(8, 0)));
        assertTrue(ClaimTimingPolicy.risksPartialDayClaim(LocalTime.of(21, 59)));
    }

    @Test public void nightlyClaimDoesNotRequireEarlyWarning() {
        assertFalse(ClaimTimingPolicy.risksPartialDayClaim(LocalTime.of(22, 0)));
        assertFalse(ClaimTimingPolicy.risksPartialDayClaim(LocalTime.of(23, 59)));
    }

    @Test public void missingClockCannotTriggerEarlyClaimAssumption() {
        assertFalse(ClaimTimingPolicy.risksPartialDayClaim(null));
    }
}
