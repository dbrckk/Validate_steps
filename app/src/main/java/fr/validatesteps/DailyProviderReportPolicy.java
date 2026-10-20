package fr.validatesteps;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Pure reporting model: no Android dependencies or third-party data queries.
 * Only fixed internal status codes are accepted; unknown codes fail closed.
 *
 * "Credit received" always means that the USER declared seeing a reward.
 * Provider-verified settlement is unsupported and must not be inferred.
 */
public final class DailyProviderReportPolicy {
    private static final DateTimeFormatter FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRANCE);

    private DailyProviderReportPolicy() {}

    public static final class Entry {
        public final String name;
        public final boolean installed;
        public final String direct;
        public final String observation;
        public final GuidedOutcome guided;
        public final String credit;

        public Entry(String name, boolean installed, String direct,
                GuidedOutcome guided, String credit) {
            this(name, installed, direct, "", guided, credit);
        }

        public Entry(String name, boolean installed, String direct,
                String observation, GuidedOutcome guided, String credit) {
            this.name = name;
            this.installed = installed;
            this.direct = direct;
            this.observation = observation;
            this.guided = guided == null ? GuidedOutcome.NONE : guided;
            this.credit = credit;
        }
    }

    public static boolean directSent(String status) {
        return "action_envoyee_credit_non_confirme".equals(status);
    }

    /** Interpret fixed category codes without copying notification content. */
    public static String observationDescription(String category) {
        if ("action_directe_compatible".equals(category))
            return "Bouton de pas direct compatible observé";
        if ("origine_action_non_verifiee".equals(category))
            return "Action ignorée : origine Android non vérifiée";
        if ("action_ouvrant_interface".equals(category))
            return "Action ouvrant une interface ; non envoyée en arrière-plan";
        if ("action_avec_saisie".equals(category))
            return "Action nécessitant une saisie ; ignorée";
        if ("libelle_de_recompense_ambigu".equals(category))
            return "Libellé de récompense ambigu ; ignoré";
        if ("libelle_action_non_reconnu".equals(category))
            return "Libellé de validation non reconnu";
        if ("type_action_non_pris_en_charge".equals(category))
            return "Type d'action Android incompatible";
        if ("notification_sans_bouton".equals(category))
            return "Notification reçue sans bouton";
        if ("notification_ancienne".equals(category))
            return "Notification périmée ; ignorée";
        return "Aucun bouton analysé aujourd'hui";
    }

    public static String directDescription(String status) {
        if (directSent(status))
            return "Commande Android envoyée ; crédit non vérifié";
        if ("action_annulee".equals(status))
            return "Commande Android annulée ; crédit non vérifié";
        if ("erreur_declenchement".equals(status))
            return "Erreur d'envoi ; crédit non vérifié";
        if ("dispatching".equals(status))
            return "Tentative réservée ; résultat d'envoi inconnu";
        return "Aucune commande directe enregistrée";
    }

    public static String creditDescription(String credit) {
        if (CreditEvidencePolicy.USER_REPORTED_RECEIVED.equals(credit))
            return "Réception déclarée par vous, non vérifiée auprès du fournisseur";
        if (CreditEvidencePolicy.USER_REPORTED_MISSING.equals(credit))
            return "Non-réception déclarée par vous";
        return "Non vérifié aujourd'hui";
    }

    public static String auditDescription(String status) {
        if ("steps_read".equals(status)) return "Pas lus dans Santé Connect";
        if ("reading".equals(status)) return "Lecture des pas en cours";
        if ("permission_missing".equals(status))
            return "Lecture bloquée : autorisation Santé Connect manquante";
        if ("background_health_unsupported".equals(status) ||
                "health_connect_unavailable".equals(status))
            return "Lecture indisponible sur cet appareil";
        if (status == null || status.isEmpty()) return "Aucun relevé enregistré ce jour";
        return "Relevé non abouti ; consulter l'historique";
    }

    public static String render(LocalDate day, String auditStatus, Entry[] entries) {
        if (day == null) return "Date du rapport indisponible";
        Entry[] safe = entries == null ? new Entry[0] : entries;
        int declaredReceived = 0;
        int declaredMissing = 0;
        int sent = 0;
        int clicked = 0;
        for (Entry item : safe) {
            if (item == null) continue;
            if (CreditEvidencePolicy.USER_REPORTED_RECEIVED.equals(item.credit))
                declaredReceived++;
            if (CreditEvidencePolicy.USER_REPORTED_MISSING.equals(item.credit))
                declaredMissing++;
            if (directSent(item.direct)) sent++;
            if (item.guided.clickAttempted()) clicked++;
        }

        StringBuilder result = new StringBuilder(900);
        result.append("BILAN DU ").append(day.format(FORMAT))
            .append("\nRelevé à 22 h : ").append(auditDescription(auditStatus))
            .append("\nDéclarations : ").append(declaredReceived)
            .append(" reçue(s) · ").append(declaredMissing)
            .append(" non reçue(s) · ")
            .append(Math.max(0, safe.length - declaredReceived - declaredMissing))
            .append(" non vérifiée(s)")
            .append("\nCommandes Android envoyées : ").append(sent)
            .append(" · Clics assistés tentés : ").append(clicked);

        for (Entry item : safe) {
            if (item == null) continue;
            result.append("\n\n").append(item.name == null ? "Service" : item.name)
                .append(item.installed ? " · installée" : " · non installée")
                .append("\n  Signal détecté : ").append(observationDescription(item.observation))
                .append("\n  Commande directe : ").append(directDescription(item.direct))
                .append("\n  Assistance : ").append(item.guided.description())
                .append("\n  Récompenses : ").append(creditDescription(item.credit));
        }

        return result.append(
            "\n\nAttention : aucune preuve de crédit côté serveur. " +
            "Les déclarations sont saisies par vous ; une commande ou un clic " +
            "ne prouve jamais une récompense reçue.").toString();
    }
}
