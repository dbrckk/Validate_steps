package fr.validatesteps;

/**
 * Bounds the amount of third-party notification data examined per scan.
 * Notifications are inspected locally; content is not copied or persisted.
 */
public final class NotificationScanPolicy {
    public static final int MAX_NOTIFICATIONS = 256;
    private NotificationScanPolicy() {}

    public static int limit(int available) {
        return Math.max(0, Math.min(available, MAX_NOTIFICATIONS));
    }
}
