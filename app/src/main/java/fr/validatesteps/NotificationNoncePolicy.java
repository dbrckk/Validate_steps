package fr.validatesteps;

/**
 * Notification actions only accept a 192-bit URL-safe random token.
 * Unexpected payloads from the exported dashboard Activity fail closed.
 */
public final class NotificationNoncePolicy {
    private static final int EXPECTED_LENGTH = 32; // Base64 URL-safe, 24 bytes, no padding

    private NotificationNoncePolicy() {}

    public static boolean validFormat(String token) {
        if (token == null || token.length() != EXPECTED_LENGTH) return false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                    || (c >= '0' && c <= '9') || c == '_' || c == '-'))
                return false;
        }
        return true;
    }
}
