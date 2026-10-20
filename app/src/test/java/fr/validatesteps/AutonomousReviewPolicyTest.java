package fr.validatesteps;

import static org.junit.Assert.*;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.Test;

public final class AutonomousReviewPolicyTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    @Test public void onlyOneOptedInReviewWithinNightDeadline() {
        assertTrue(AutonomousReviewPolicy.shouldNotify(DAY, DAY,
            LocalTime.of(23, 15), true, true, false, 2));
        assertTrue(AutonomousReviewPolicy.shouldNotify(DAY, DAY,
            LocalTime.of(23, 39), true, true, false, 1));
        assertFalse(AutonomousReviewPolicy.shouldNotify(DAY, DAY,
            LocalTime.of(23, 40), true, true, false, 2));
    }

    @Test public void neverNagsWithoutExplicitOptInOrWhenEverythingChecked() {
        assertFalse(AutonomousReviewPolicy.shouldNotify(DAY, DAY,
            LocalTime.of(23, 20), true, false, false, 3));
        assertFalse(AutonomousReviewPolicy.shouldNotify(DAY, DAY,
            LocalTime.of(23, 20), true, true, false, 0));
        assertFalse(AutonomousReviewPolicy.shouldNotify(DAY, DAY,
            LocalTime.of(23, 20), false, true, false, 3));
        assertFalse(AutonomousReviewPolicy.shouldNotify(DAY, DAY,
            LocalTime.of(23, 20), true, true, true, 3));
    }

    @Test public void staleAndEarlyJobsCannotGenerateReminders() {
        assertFalse(AutonomousReviewPolicy.shouldNotify(DAY.minusDays(1), DAY,
            LocalTime.of(23, 20), true, true, false, 3));
        assertFalse(AutonomousReviewPolicy.shouldNotify(DAY, DAY.plusDays(1),
            LocalTime.of(23, 20), true, true, false, 3));
        assertFalse(AutonomousReviewPolicy.shouldNotify(DAY, DAY,
            LocalTime.of(22, 20), true, true, false, 3));
        assertFalse(AutonomousReviewPolicy.shouldNotify(null, DAY,
            LocalTime.of(23, 20), true, true, false, 3));
    }
}
