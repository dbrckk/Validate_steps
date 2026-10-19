package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public final class NotificationActionWindowTest {
    private final ZoneId paris = ZoneId.of("Europe/Paris");
    private ZonedDateTime at(int hour, int minute) {
        return ZonedDateTime.of(2026, 10, 19, hour, minute, 0, 0, paris);
    }

    @Test public void permitsSameNightUserGestureInsideWindow() {
        assertTrue(NotificationActionWindow.canStart(LocalDate.of(2026, 10, 19),
            at(22, 1), 2000));
        assertTrue(NotificationActionWindow.canStart(LocalDate.of(2026, 10, 19),
            at(23, 39), 99 * 60 * 1000L));
    }

    @Test public void refusesOldAndFutureNotifications() {
        assertFalse(NotificationActionWindow.canStart(LocalDate.of(2026, 10, 18),
            at(22, 3), 2000));
        assertFalse(NotificationActionWindow.canStart(LocalDate.of(2026, 10, 20),
            at(22, 3), 2000));
    }

    @Test public void refusesOutOfWindowOrExpiredTokens() {
        assertFalse(NotificationActionWindow.canStart(LocalDate.of(2026, 10, 19),
            at(21, 59), 2000));
        assertFalse(NotificationActionWindow.canStart(LocalDate.of(2026, 10, 19),
            at(23, 40), 2000));
        assertFalse(NotificationActionWindow.canStart(LocalDate.of(2026, 10, 19),
            at(22, 10), -1));
        assertFalse(NotificationActionWindow.canStart(LocalDate.of(2026, 10, 19),
            at(22, 10), 120 * 60 * 1000L));
    }
}
