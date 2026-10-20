package fr.validatesteps;

/**
 * Pure, conservative explanation of what can execute without Accessibility.
 * A valid Health Connect step count is NOT a third-party reward credit.
 */
public final class AutonomousModePolicy {
    private AutonomousModePolicy() {}

    public static String providerStatus(boolean installed, boolean nativeConversion,
            boolean enabled, boolean permission, boolean listenerConnected,
            String directStatus, String observation, boolean userReceived) {
        if (!installed) return "Application non installée";
        if (nativeConversion)
            return "Conversion gérée par Sweatcoin ; crédit non contrôlable ici";
        if (userReceived)
            return "Réception déclarée par vous ; aucune preuve serveur accessible";
        if (DailyProviderReportPolicy.directSent(directStatus))
            return "Commande directe envoyée ; récompense non vérifiée";
        if (!enabled) return "Actions autonomes désactivées";
        if (!permission) return "Autorisation Android des notifications manquante";
        if (!listenerConnected) return "Service de notifications non connecté";
        if ("action_directe_compatible".equals(observation))
            return "Action directe observée ; aucune commande envoyée";
        if ("action_ouvrant_interface".equals(observation))
            return "La notification ouvre l'application : validation autonome indisponible";
        if ("origine_action_non_verifiee".equals(observation))
            return "Action ignorée : origine Android non vérifiée";
        if ("libelle_de_recompense_ambigu".equals(observation))
            return "Action ignorée : récompense non explicitement liée aux pas";
        if ("action_avec_saisie".equals(observation))
            return "Action avec saisie : impossible sans interface";
        return "Aucune action directe de validation des pas disponible aujourd'hui";
    }

    public static boolean autonomousReady(boolean auditEnabled, boolean bridgeEnabled,
            boolean notificationGranted, boolean healthGranted) {
        return auditEnabled && bridgeEnabled && notificationGranted && healthGranted;
    }
}
