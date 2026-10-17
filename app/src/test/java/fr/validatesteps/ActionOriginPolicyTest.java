package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class ActionOriginPolicyTest {
    @Test public void acceptsActionCreatedByTheNotificationApp() {
        assertTrue(ActionOriginPolicy.trusted("com.weward", 10051, "com.weward", 10051));
        assertTrue(ActionOriginPolicy.trusted("com.macadamapp", 10999,
            "com.macadamapp", 10999));
        assertTrue(ActionOriginPolicy.trusted("com.winwalk.android", 10201,
            "com.winwalk.android", 10201));
    }

    @Test public void rejectsAnotherCreatorsPendingIntent() {
        assertFalse(ActionOriginPolicy.trusted("com.weward", 10051,
            "com.example.broker", 10051));
        assertFalse(ActionOriginPolicy.trusted("com.weward", 10051,
            "com.macadamapp", 10099));
    }

    @Test public void rejectsCreatorUidMismatchIncludingAndroidUserProfiles() {
        assertFalse(ActionOriginPolicy.trusted("com.weward", 10051, "com.weward", 10123));
        assertFalse(ActionOriginPolicy.trusted("com.weward", 10051, "com.weward", 1010051));
    }

    @Test public void rejectsUnknownOrMissingIdentity() {
        assertFalse(ActionOriginPolicy.trusted("com.weward", 10051, null, 10051));
        assertFalse(ActionOriginPolicy.trusted("com.weward", -1, "com.weward", 10051));
        assertFalse(ActionOriginPolicy.trusted("com.weward", 10051, "com.weward", -1));
    }

    @Test public void rejectsUnapprovedNotificationPackage() {
        assertFalse(ActionOriginPolicy.trusted("com.random.app", 10051,
            "com.random.app", 10051));
        assertFalse(ActionOriginPolicy.trusted(null, 10051, "com.weward", 10051));
    }
}
