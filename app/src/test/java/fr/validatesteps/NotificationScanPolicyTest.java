package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class NotificationScanPolicyTest {
    @Test public void negativeAndEmptyListsAreSafe() {
        assertEquals(0, NotificationScanPolicy.limit(-1));
        assertEquals(0, NotificationScanPolicy.limit(0));
    }
    @Test public void ordinaryListsAreNotTruncated() {
        assertEquals(14, NotificationScanPolicy.limit(14));
    }
    @Test public void excessiveListsHaveAFixedCeiling() {
        assertEquals(256, NotificationScanPolicy.limit(30000));
    }
}
