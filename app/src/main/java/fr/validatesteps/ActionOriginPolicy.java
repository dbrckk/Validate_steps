package fr.validatesteps;

/**
 * Pure fail-closed policy for Android's system-reported PendingIntent creator.
 *
 * A notification can carry a PendingIntent originating in another app.
 * Only execute when the notification app and the action's CREATOR are the
 * same allowed provider, and both belong to the same Android UID.
 */
public final class ActionOriginPolicy {
    private ActionOriginPolicy() {}

    public static boolean trusted(String notificationPackage, int notificationUid,
            String creatorPackage, int creatorUid) {
        return ClaimActionPolicy.isProvider(notificationPackage)
            && notificationPackage.equals(creatorPackage)
            && notificationUid >= 0
            && creatorUid >= 0
            && notificationUid == creatorUid;
    }
}
