package fr.validatesteps;

/**
 * Bounded local outcome of a user-started Accessibility session.
 * A click or an app opening is not evidence that a provider credited rewards.
 */
public enum GuidedOutcome {
    NONE("Aucune tentative assistée", false),
    SEARCHING("Recherche d'un bouton de pas en cours", false),
    CLICK_ATTEMPTED("Clic sur le bouton de pas tenté, crédit non vérifié", true),
    CLICK_REFUSED("Bouton de pas trouvé, clic refusé", false),
    BUTTON_MISSING("Aucun bouton de validation reconnu", false),
    APP_MISSING("Application non installée", false),
    OPEN_BLOCKED("Ouverture bloquée par Android", false),
    SWEATCOIN_OPENED("Sweatcoin ouvert pour synchronisation, crédit non vérifié", false),
    SKIPPED_DIRECT("Commande directe déjà envoyée aujourd'hui, non répétée", false),
    SKIPPED_USER_RECEIVED("Réception déjà déclarée par l'utilisateur, non répétée", false),
    CANCELED("Parcours interrompu avant vérification", false);

    private final String description;
    private final boolean clickAttempted;

    GuidedOutcome(String description, boolean clickAttempted) {
        this.description = description;
        this.clickAttempted = clickAttempted;
    }

    public String description() { return description; }
    public boolean clickAttempted() { return clickAttempted; }

    public static GuidedOutcome fromStored(String code) {
        if (code == null || code.isEmpty()) return NONE;
        try { return GuidedOutcome.valueOf(code); }
        catch (IllegalArgumentException malformed) { return NONE; }
    }
}
