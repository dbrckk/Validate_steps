package fr.validatesteps;

import android.content.Context;
import java.time.LocalDate;

/**
 * Read-only snapshot across the four existing local ledgers.
 * Never fetches or stores third-party account data and never needs Internet.
 */
public final class DailyProviderReport {
    private DailyProviderReport() {}

    public static String build(Context context, LocalDate day) {
        if (day == null) return "Date du rapport indisponible";
        ActionBridgeState direct = new ActionBridgeState(context);
        ClaimFlowState guided = new ClaimFlowState(context);
        ProviderCreditStore credits = new ProviderCreditStore(context);
        DailyProviderReportPolicy.Entry[] entries = {
            entry(context, ProviderPackages.WEWARD, "WeWard", day, direct, guided, credits),
            entry(context, ProviderPackages.MACADAM, "Macadam", day, direct, guided, credits),
            entry(context, ProviderPackages.WINWALK, "Winwalk", day, direct, guided, credits),
            entry(context, ProviderPackages.SWEATCOIN, "Sweatcoin", day, direct, guided, credits)
        };
        return DailyProviderReportPolicy.render(day,
            new RunStore(context).statusOn(day), entries);
    }

    private static DailyProviderReportPolicy.Entry entry(Context context,
            String packageName, String name, LocalDate day, ActionBridgeState direct,
            ClaimFlowState guided, ProviderCreditStore credits) {
        return new DailyProviderReportPolicy.Entry(
            name,
            ProviderPackages.installed(context, packageName),
            direct.actionStatusOn(packageName, day),
            direct.observationOn(packageName, day),
            guided.outcomeOn(packageName, day),
            credits.statusOn(packageName, day),
            guided.clickAttemptedOn(packageName, day));
    }
}
