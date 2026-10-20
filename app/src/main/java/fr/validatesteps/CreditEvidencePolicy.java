package fr.validatesteps;

import java.time.LocalDate;

/**
 * Model for LOCAL USER STATEMENTS about third-party credit.
 *
 * Never claim provider verification: there is no authorized account balance
 * endpoint, transaction receipt or authenticated provider callback.
 */
public final class CreditEvidencePolicy {
    public static final String USER_REPORTED_RECEIVED = "user_reported_received";
    public static final String USER_REPORTED_MISSING = "user_reported_missing";
    public static final String UNKNOWN = "not_verified";

    private CreditEvidencePolicy() {}

    public static boolean validStatus(String value) {
        return USER_REPORTED_RECEIVED.equals(value)
            || USER_REPORTED_MISSING.equals(value);
    }

    public static String visibleStatus(String storedDay, LocalDate today, String status) {
        if (today == null || !today.toString().equals(storedDay) ||
                !validStatus(status)) return "Crédit non vérifié aujourd'hui";
        return USER_REPORTED_RECEIVED.equals(status)
            ? "Vous avez déclaré avoir reçu les récompenses aujourd'hui"
            : "Vous avez indiqué ne pas avoir reçu les récompenses aujourd'hui";
    }

    public static boolean userReportedReceivedToday(
            String storedDay, LocalDate today, String status) {
        return today != null && today.toString().equals(storedDay)
            && USER_REPORTED_RECEIVED.equals(status);
    }
}
