package fr.validatesteps;

/**
 * A fully independent mode: no AccessibilityService and no UI automation.
 * Only provider-created direct Android notification actions are eligible.
 * Reporting never assumes that a submitted action caused a provider credit.
 */
public final class HandsFreeModePolicy {
    private HandsFreeModePolicy() {}

    /** Whether unattended direct-action attempts have been configured. */
    public static boolean configured(boolean alarmEnabled, boolean actionsEnabled,
            boolean notificationAccessGranted) {
        return alarmEnabled && actionsEnabled && notificationAccessGranted;
    }

    /** Explain what can be done without any Accessibility interactions. */
    public static String setupDescription(boolean alarmEnabled, boolean actionsEnabled,
            boolean notificationAccessGranted, boolean listenerConnected) {
        if (!notificationAccessGranted)
            return "Accès aux notifications requis · activation Android nécessaire";
        if (!alarmEnabled || !actionsEnabled)
            return "Mode autonome désactivé · aucune tentative nocturne configurée";
        if (!listenerConnected)
            return "Mode programmé · attente de connexion du service de notifications";
        return "Mode programmé à 22 h · seules les actions directes exposées seront tentées";
    }

    public static String providerDescription(boolean installed, boolean userReportedReceived,
            String directStatus, String observedCategory, boolean sweatcoin) {
        if (!installed) return "Application non installée";
        if (userReportedReceived) return "Réception déclarée par vous · non vérifiée auprès du service";
        if (sweatcoin)
            return "Conversion gérée par Sweatcoin · non vérifiable ici";
        if ("action_envoyee_credit_non_confirme".equals(directStatus))
            return "Commande directe envoyée · crédit non vérifié";
        if ("dispatching".equals(directStatus))
            return "Commande réservée · résultat incertain";
        if ("action_annulee".equals(directStatus))
            return "Commande directe annulée par Android";
        if ("erreur_declenchement".equals(directStatus))
            return "Erreur lors de l'envoi de la commande";
        if ("action_directe_compatible".equals(observedCategory))
            return "Bouton de pas direct détecté · pas encore envoyé";
        if ("action_ouvrant_interface".equals(observedCategory))
            return "Notification ouvrant une interface · validation non automatique";
        if ("libelle_de_recompense_ambigu".equals(observedCategory))
            return "Bouton ambigu ignoré · aucune validation prouvée";
        if ("origine_action_non_verifiee".equals(observedCategory))
            return "Origine de l'action non vérifiée · commande refusée";
        return "Aucune validation directe confirmée aujourd'hui";
    }
}
