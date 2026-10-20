package fr.validatesteps;

import android.content.Context;
import android.content.SharedPreferences;
import java.time.LocalDate;

/**
 * Private user-reported per-provider daily evidence. No balances, identifiers
 * or provider notification content; nothing is transmitted over a network.
 */
public final class ProviderCreditStore {
    private final SharedPreferences prefs;

    public ProviderCreditStore(Context context) {
        prefs = context.getSharedPreferences("provider_credit_evidence", Context.MODE_PRIVATE);
    }

    public boolean report(String packageName, LocalDate day, String status) {
        if (!GuidedClaimPolicy.isTarget(packageName) || day == null ||
                !CreditEvidencePolicy.validStatus(status)) return false;
        return prefs.edit()
            .putString("date_" + packageName, day.toString())
            .putString("status_" + packageName, status)
            .putLong("time_" + packageName, System.currentTimeMillis())
            .commit();
    }

    public String visibleStatus(String packageName, LocalDate day) {
        if (!GuidedClaimPolicy.isTarget(packageName))
            return "Service non pris en charge";
        return CreditEvidencePolicy.visibleStatus(
            prefs.getString("date_" + packageName, ""), day,
            prefs.getString("status_" + packageName, CreditEvidencePolicy.UNKNOWN));
    }

    public boolean userReportedReceivedToday(String packageName, LocalDate day) {
        return GuidedClaimPolicy.isTarget(packageName)
            && CreditEvidencePolicy.userReportedReceivedToday(
                prefs.getString("date_" + packageName, ""), day,
                prefs.getString("status_" + packageName, CreditEvidencePolicy.UNKNOWN));
    }

    /** Number of providers for which the user has checked and reported a credit today. */
    public int userReportedReceivedCount(LocalDate day) {
        int count = 0;
        for (String packageName : GuidedClaimPolicy.ORDER) {
            if (userReportedReceivedToday(packageName, day)) count++;
        }
        return count;
    }

    public boolean clear(String packageName) {
        if (!GuidedClaimPolicy.isTarget(packageName)) return false;
        return prefs.edit()
            .remove("date_" + packageName)
            .remove("status_" + packageName)
            .remove("time_" + packageName).commit();
    }
}
