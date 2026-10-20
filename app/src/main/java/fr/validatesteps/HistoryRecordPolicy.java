package fr.validatesteps;

import java.time.LocalDate;

/**
 * Bounded, allowlisted, date-scoped diagnostic persistence.
 *
 * Contains no account credentials, notification body, step count or balance.
 * A system action is NEVER interpreted as a provider credit.
 */
public final class HistoryRecordPolicy {
    public static final int RETENTION_DAYS = 30;

    private HistoryRecordPolicy() {}

    public static boolean withinRetention(LocalDate day, LocalDate today) {
        return day != null && today != null
            && !day.isAfter(today)
            && !day.isBefore(today.minusDays(RETENTION_DAYS - 1));
    }

    public static String audit(String status) {
        if ("steps_read".equals(status)) return "steps_read";
        if ("reading".equals(status)) return "reading";
        if ("permission_missing".equals(status)) return "permission_missing";
        if ("background_health_unsupported".equals(status))
            return "background_health_unsupported";
        if ("health_connect_unavailable".equals(status))
            return "health_connect_unavailable";
        if (status == null || status.isEmpty()) return "";
        return "audit_failed";
    }

    public static String direct(String status) {
        if ("dispatching".equals(status)) return "dispatching";
        if ("action_envoyee_credit_non_confirme".equals(status))
            return "action_envoyee_credit_non_confirme";
        if ("action_annulee".equals(status)) return "action_annulee";
        if ("erreur_declenchement".equals(status)) return "erreur_declenchement";
        return "";
    }

    public static String observation(String code) {
        return ObservationRank.score(code) > 0 ? code : "";
    }

    public static String credit(String code) {
        return CreditEvidencePolicy.validStatus(code)
            ? code : CreditEvidencePolicy.UNKNOWN;
    }
}
