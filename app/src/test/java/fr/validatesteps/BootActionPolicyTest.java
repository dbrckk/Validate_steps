package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class BootActionPolicyTest {
    @Test public void allRelevantSystemChangesRearm() {
        for (String action : new String[]{
                "android.intent.action.BOOT_COMPLETED",
                "android.intent.action.TIME_SET",
                "android.intent.action.TIMEZONE_CHANGED",
                "android.intent.action.MY_PACKAGE_REPLACED",
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"}) {
            assertTrue(action, BootActionPolicy.shouldRearm(action));
        }
    }

    @Test public void randomIntentsDoNotTriggerRescheduling() {
        assertFalse(BootActionPolicy.shouldRearm(null));
        assertFalse(BootActionPolicy.shouldRearm(""));
        assertFalse(BootActionPolicy.shouldRearm("com.other.application.TRIGGER"));
    }
}
