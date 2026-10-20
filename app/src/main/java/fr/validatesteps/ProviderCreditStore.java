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
    private final Context appContext;

    public ProviderCreditStore(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences("provider_credit_evidence", Context.MODE_PRIVATE);
    }

    public boolean report(String packageName, LocalDate day, String status) {
        if (!GuidedClaimPolicy.isTarget(packageName) || day == null ||
                !CreditEvidencePolicy.validStatus(status)) return false;
        boolean committed = prefs.edit()
            .putString("date_" + packageName, day.toString())
            .putString("status_" + packageName, status)
            .putLong("time_" + packageName, System.currentTimeMillis())
            .commit();
        if (committed)
            new DailyHistoryStore(appContext).recordCredit(packageName, day, status);
        return committed;
    }

    public String visibleStatus(String packageName, LocalDate day) {
        if (!GuidedClaimPolicy.isTarget(packageName))
            return "Service non pris en charge";
        return CreditEvidencePolicy.visibleStatus(
            prefs.getString("date_" + packageName, ""), day,
            prefs.getString("status_" + packageName, CreditEvidencePolicy.UNKNOWN));
    }

    /** Fixed user statement for the requested day; NOT provider verification. */
    public String statusOn(String packageName, LocalDate day) {
        if (!GuidedClaimPolicy.isTarget(packageName) || day == null ||
                !day.toString().equals(prefs.getString("date_" + packageName, "")))
            return CreditEvidencePolicy.UNKNOWN;
        String stored = prefs.getString("status_" + packageName,
            CreditEvidencePolicy.UNKNOWN);
        return CreditEvidencePolicy.validStatus(stored) ? stored :
            CreditEvidencePolicy.UNKNOWN;
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
        LocalDate day;
        try { day = LocalDate.parse(prefs.getString("date_" + packageName, "")); }
        catch (RuntimeException invalid) { day = null; }
        boolean committed = prefs.edit()
            .remove("date_" + packageName)
            .remove("status_" + packageName)
            .remove("time_" + packageName).commit();
        if (committed && day != null)
            new DailyHistoryStore(appContext).recordCredit(
                packageName, day, CreditEvidencePolicy.UNKNOWN);
        return committed;
    }
}
