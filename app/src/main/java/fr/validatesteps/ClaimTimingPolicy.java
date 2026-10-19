package fr.validatesteps;

import java.time.LocalTime;

/**
 * A provider's first daily claim may lock its step tally for the day.
 * Warn before an intentionally early manual claim, not at night.
 */
public final class ClaimTimingPolicy {
    private static final LocalTime NIGHTLY = LocalTime.of(22, 0);
    private ClaimTimingPolicy() {}

    public static boolean risksPartialDayClaim(LocalTime time) {
        return time != null && time.isBefore(NIGHTLY);
    }
}
