package fr.validatesteps;

import java.text.Normalizer;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * Conservative gate: only provider-supplied, explicitly step-related
 * notification actions can be sent. Never clicks activities or ads.
 */
public final class ClaimActionPolicy {
    private static final Set<String> PACKAGES = Set.of(
        "com.weward", "com.macadamapp", "com.winwalk.android");
    // Exact, step-specific labels only. Generic "collect coins" / "get points"
    // actions are too ambiguous: they may trigger unrelated redemptions.
    private static final Set<String> ACTIONS = Set.of(
        "valider mes pas", "valider les pas", "convertir mes pas",
        "reclamer mes pas", "recuperer mes pas", "collecter mes pas",
        "claim steps", "validate steps", "convert steps",
        "collect step coins", "collect coins from steps");
    private static final Set<String> AMBIGUOUS_ACTIONS = Set.of(
        "recuperer mes pieces", "recuperer mes points",
        "collecter mes pieces", "collecter mes points",
        "collect coins", "claim coins", "get points", "collect rewards");

    private ClaimActionPolicy() {}

    public static boolean isProvider(String packageName) {
        return packageName != null && PACKAGES.contains(packageName);
    }

    public static boolean inTimeWindow(ZonedDateTime now) {
        return RetryPolicy.canRecover(now);
    }

    /**
     * Diagnostic classifications contain no notification text or account data.
     * Detection is independent of 22:00; execution still requires the window.
     */
    public static String classify(String packageName, String button,
            boolean isActivity, boolean isBackgroundType, boolean needsInput) {
        if (!isProvider(packageName)) return "service_non_pris_en_charge";
        if (needsInput) return "action_avec_saisie";
        if (isActivity) return "action_ouvrant_interface";
        if (!isBackgroundType) return "type_action_non_pris_en_charge";
        String normalized = normalize(button);
        if (ACTIONS.contains(normalized)) return "action_directe_compatible";
        if (AMBIGUOUS_ACTIONS.contains(normalized))
            return "libelle_de_recompense_ambigu";
        return "libelle_action_non_reconnu";
    }

    /** Automatic dispatch is strictly limited to the nightly local time window. */
    public static boolean eligible(String packageName, String button,
            boolean isActivity, boolean isBackgroundType, boolean needsInput, ZonedDateTime time) {
        return time != null && inTimeWindow(time) &&
            eligibleOnDemand(packageName, button, isActivity, isBackgroundType, needsInput);
    }

    /**
     * Only an explicit foreground user gesture may call this alternative.
     * It relaxes the CLOCK constraint, not package, content or action security.
     */
    public static boolean eligibleOnDemand(String packageName, String button,
            boolean isActivity, boolean isBackgroundType, boolean needsInput) {
        return isProvider(packageName) && !isActivity && isBackgroundType &&
            !needsInput && ACTIONS.contains(normalize(button));
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String plain = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return plain.trim().replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }
}
