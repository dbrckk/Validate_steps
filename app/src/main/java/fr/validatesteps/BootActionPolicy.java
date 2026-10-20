package fr.validatesteps;

import java.util.Set;

/** Accept only system broadcasts that can invalidate the nightly schedule. */
public final class BootActionPolicy {
    private static final Set<String> REARM = Set.of(
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.TIME_SET",
        "android.intent.action.TIMEZONE_CHANGED",
        "android.intent.action.MY_PACKAGE_REPLACED",
        "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
    );

    private BootActionPolicy() {}

    public static boolean shouldRearm(String action) {
        return action != null && REARM.contains(action);
    }
}
