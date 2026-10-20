package fr.validatesteps;

import android.content.Context;
import android.content.pm.PackageManager;
import java.time.LocalDate;

/** Read-only snapshot of autonomous capability and limits for the dashboard. */
public final class AutonomousSnapshot {
    private AutonomousSnapshot() {}

    public static String describe(Context context, boolean listenerConnected) {
        LocalDate day = LocalDate.now();
        RunStore run = new RunStore(context);
        ActionBridgeState bridge = new ActionBridgeState(context);
        ProviderCreditStore credits = new ProviderCreditStore(context);
        boolean health = HealthReader.backgroundSupported() &&
            context.checkSelfPermission(HealthReader.READ_STEPS) ==
                PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(HealthReader.READ_BACKGROUND) ==
                PackageManager.PERMISSION_GRANTED;
        boolean permission = RewardActionListener.hasPermission(context);
        boolean ready = AutonomousModePolicy.autonomousReady(
            run.enabled(), bridge.enabled(), permission, health);

        StringBuilder text = new StringBuilder(500);
        text.append("Mode autonome 22 h : ")
            .append(ready ? "configuré (sans Accessibilité)" : "configuration incomplète");
        if (!run.enabled()) text.append("\n• Relevé à 22 h désactivé");
        if (!health) text.append("\n• Autorisation Santé Connect à configurer");
        if (!bridge.enabled()) text.append("\n• Actions directes désactivées");
        if (!permission) text.append("\n• Accès Android aux notifications manquant");
        else if (!listenerConnected) text.append("\n• Service de notifications non connecté");

        addProvider(text, context, "WeWard", ProviderPackages.WEWARD,
            false, run.enabled() && bridge.enabled(), permission,
            listenerConnected, day, bridge, credits);
        addProvider(text, context, "Macadam", ProviderPackages.MACADAM,
            false, run.enabled() && bridge.enabled(), permission,
            listenerConnected, day, bridge, credits);
        addProvider(text, context, "Winwalk", ProviderPackages.WINWALK,
            false, run.enabled() && bridge.enabled(), permission,
            listenerConnected, day, bridge, credits);
        addProvider(text, context, "Sweatcoin", ProviderPackages.SWEATCOIN,
            true, false, permission, listenerConnected, day, bridge, credits);

        return text.append(
            "\nLes commandes ne prouvent pas que les récompenses ont été créditées.")
            .toString();
    }

    private static void addProvider(StringBuilder text, Context context, String name,
            String packageName, boolean nativeConversion, boolean enabled,
            boolean permission, boolean listenerConnected, LocalDate day,
            ActionBridgeState direct, ProviderCreditStore credits) {
        text.append("\n").append(name).append(" : ")
            .append(AutonomousModePolicy.providerStatus(
                ProviderPackages.installed(context, packageName), nativeConversion,
                enabled, permission, listenerConnected,
                direct.actionStatusOn(packageName, day),
                direct.observationOn(packageName, day),
                credits.userReportedReceivedToday(packageName, day)));
    }
}
