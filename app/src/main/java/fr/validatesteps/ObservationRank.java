package fr.validatesteps;

/** Preserve the most informative provider-action observation on a given day. */
public final class ObservationRank {
    private ObservationRank() {}

    public static int score(String category) {
        if ("action_directe_compatible".equals(category)) return 6;
        if ("action_ouvrant_interface".equals(category)) return 5;
        if ("action_avec_saisie".equals(category)) return 4;
        if ("libelle_action_non_reconnu".equals(category)) return 3;
        if ("type_action_non_pris_en_charge".equals(category)) return 3;
        if ("notification_sans_bouton".equals(category)) return 2;
        if ("notification_ancienne".equals(category)) return 1;
        return 0;
    }

    public static String strongest(String previous, String next) {
        return score(previous) >= score(next) ? previous : next;
    }
}
