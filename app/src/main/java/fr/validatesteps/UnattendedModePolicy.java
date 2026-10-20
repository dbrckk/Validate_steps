package fr.validatesteps;

/**
 * Honest capability labels for the nightly, no-Accessibility mode.
 * These codes are local observations, NOT a provider authorization or a
 * cryptographic proof that rewards were credited in a third-party account.
 */
public final class UnattendedModePolicy {
    private UnattendedModePolicy() {}

    public static boolean canArmDirectActions(boolean userOptIn, boolean permissionGranted) {
        return userOptIn && permissionGranted;
    }

    public static String providerState(String packageName, boolean enabled,
            String observation, String sentStatus) {
        if (ProviderPackages.SWEATCOIN.equals(packageName))
            return "Conversion en arrière-plan par Sweatcoin · crédit non vérifié";
        if (!ClaimActionPolicy.isProvider(packageName))
            return "Service non pris en charge";
        if (!enabled)
            return "Mode sans appui désactivé";
        if ("action_envoyee_credit_non_confirme".equals(sentStatus))
            return "Commande Android envoyée · crédit à vérifier dans l'application";
        if ("action_annulee".equals(sentStatus))
            return "Commande Android annulée · validation non démontrée";
        if ("erreur_declenchement".equals(sentStatus))
            return "Erreur de commande directe · validation non démontrée";
        if ("dispatching".equals(sentStatus))
            return "Envoi initié · résultat inconnu";
        if ("action_directe_compatible".equals(observation))
            return "Bouton direct détecté · exécution autorisée à 22 h, crédit non garanti";
        if ("action_ouvrant_interface".equals(observation))
            return "La notification ouvre seulement l'application · validation manuelle requise";
        if ("origine_action_non_verifiee".equals(observation))
            return "Action Android d'origine non vérifiée · ignorée";
        if ("libelle_de_recompense_ambigu".equals(observation))
            return "Bouton de récompense ambigu · non exécuté";
        if ("notification_sans_bouton".equals(observation))
            return "Notification sans commande directe · validation manuelle requise";
        return "Aucune commande directe compatible observée · validation non garantie";
    }
}
