package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class GuidedClickSafetyPolicyTest {
    @Test public void exactProviderButtonAndNearWrapperCanBeUsed() {
        assertTrue(GuidedClickSafetyPolicy.permitted(
            "com.weward", "com.weward", 0, true, true, true));
        assertTrue(GuidedClickSafetyPolicy.permitted(
            "com.macadamapp", "com.macadamapp", 2, true, true, true));
    }

    @Test public void rejectsCrossPackageOverlayAndUnknownOwnership() {
        assertFalse(GuidedClickSafetyPolicy.permitted(
            "com.weward", "com.android.chrome", 0, true, true, true));
        assertFalse(GuidedClickSafetyPolicy.permitted(
            "com.weward", null, 0, true, true, true));
        assertFalse(GuidedClickSafetyPolicy.permitted(
            null, "com.weward", 0, true, true, true));
    }

    @Test public void refusesBroadAncestorsHiddenOrDisabledViews() {
        assertFalse(GuidedClickSafetyPolicy.permitted(
            "com.weward", "com.weward", 3, true, true, true));
        assertFalse(GuidedClickSafetyPolicy.permitted(
            "com.weward", "com.weward", -1, true, true, true));
        assertFalse(GuidedClickSafetyPolicy.permitted(
            "com.weward", "com.weward", 0, false, true, true));
        assertFalse(GuidedClickSafetyPolicy.permitted(
            "com.weward", "com.weward", 0, true, false, true));
        assertFalse(GuidedClickSafetyPolicy.permitted(
            "com.weward", "com.weward", 0, true, true, false));
    }
}
