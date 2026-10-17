package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class TimelinePolicyTest {
    @Test public void firstEventIsRecorded() {
        assertEquals("22:00 | ALARM_RECEIVED",
            TimelinePolicy.append("", "22:00 | ALARM_RECEIVED"));
    }

    @Test public void historyIsBoundedToEightyEvents() {
        String history = "";
        for (int i = 0; i < 110; i++)
            history = TimelinePolicy.append(history, "EV_" + i);
        String[] lines = history.split("\n");
        assertEquals(TimelinePolicy.MAX_EVENTS, lines.length);
        assertEquals("EV_30", lines[0]);
        assertEquals("EV_109", lines[lines.length - 1]);
    }

    @Test public void latestReportsOnlyRecentEvents() {
        String history = "first\nsecond\nthird";
        assertEquals("second\nthird", TimelinePolicy.latest(history, 2));
        assertEquals("first\nsecond\nthird", TimelinePolicy.latest(history, 10));
    }

    @Test public void noEventsHasClearDiagnostic() {
        assertEquals("aucun événement", TimelinePolicy.latest(null, 18));
        assertEquals("aucun événement", TimelinePolicy.latest("", 18));
        assertEquals("aucun événement", TimelinePolicy.latest("some", 0));
    }

    @Test public void cannotInjectNewlinesIntoEvent() {
        try {
            TimelinePolicy.append("original", "BAD\ninjected");
            fail("Expected rejection of a multi-line event");
        } catch (IllegalArgumentException expected) {
            assertEquals("An event must be one line", expected.getMessage());
        }
    }
}
