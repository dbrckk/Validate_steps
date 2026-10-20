package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Base64;

/**
 * A one-use nonce binds a notification button to a legitimate user gesture.
 * Never start an Accessibility session solely from a forgeable exported intent.
 */
public final class GuidedNotificationToken {
    public static final String EXTRA = "fr.validatesteps.USER_STARTED_GUIDED_FLOW";
    private static final String STORE = "notification_action_nonce";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Object LOCK = new Object();

    private GuidedNotificationToken() {}

    public static String create(Context context, LocalDate day) {
        byte[] bits = new byte[24];
        RANDOM.nextBytes(bits);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bits);
        synchronized (LOCK) {
            context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
                .putString("nonce", token)
                .putString("day", day.toString())
                .putLong("issued", System.currentTimeMillis())
                .commit();
        }
        return token;
    }

    public static boolean consume(Context context, String candidate) {
        if (!NotificationNoncePolicy.validFormat(candidate)) return false;
        synchronized (LOCK) {
            SharedPreferences p = context.getSharedPreferences(STORE, Context.MODE_PRIVATE);
            String nonce = p.getString("nonce", "");
            long issued = p.getLong("issued", 0);
            long elapsed = System.currentTimeMillis() - issued;
            LocalDate date;
            try { date = LocalDate.parse(p.getString("day", "")); }
            catch (RuntimeException badStoredDate) { return false; }
            boolean valid = NotificationNoncePolicy.validFormat(nonce)
                && NotificationActionWindow.canStart(
                    date, ZonedDateTime.now(), elapsed)
                && MessageDigest.isEqual(
                    candidate.getBytes(StandardCharsets.UTF_8),
                    nonce.getBytes(StandardCharsets.UTF_8));
            if (valid) p.edit().clear().commit(); // Single-use.
            return valid;
        }
    }
}
