package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class NotificationNoncePolicyTest {
    @Test public void acceptsOnlyExpectedRandomUrlSafeTokens() {
        assertTrue(NotificationNoncePolicy.validFormat(
            "abcdefghijklmnopqrstuvwxyz012345"));
        assertTrue(NotificationNoncePolicy.validFormat(
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123-_"));
    }

    @Test public void rejectsEmptyUnexpectedAndOversizedIntents() {
        assertFalse(NotificationNoncePolicy.validFormat(null));
        assertFalse(NotificationNoncePolicy.validFormat(""));
        assertFalse(NotificationNoncePolicy.validFormat("abc"));
        assertFalse(NotificationNoncePolicy.validFormat("a".repeat(10000)));
        assertFalse(NotificationNoncePolicy.validFormat(
            "abcdefghijklmnopqrstuvwxyz01234="));
        assertFalse(NotificationNoncePolicy.validFormat(
            "abcdefghijklmnopqrstuvwxyz01234/"));
    }
}
