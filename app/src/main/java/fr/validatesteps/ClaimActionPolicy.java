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
    private static final Set<String> ACTIONS = Set.of(
        "valider mes pas", "valider les pas", "convertir mes pas",
        "recuperer mes pieces", "recuperer mes points",
        "reclamer mes pas", "collecter mes pieces",
        "claim steps", "validate steps", "convert steps",
        "collect step coins", "collect coins from steps");

    private ClaimActionPolicy() {}

    public static boolean isProvider(String packageName) {
        return PACKAGES.contains(packageName);
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
        return ACTIONS.contains(normalize(button))
            ? "action_directe_compatible" : "libelle_action_non_reconnu";
    }

    public static boolean eligible(String packageName, String button,
            boolean isActivity, boolean isBackgroundType, boolean needsInput, ZonedDateTime time) {
        return isProvider(packageName) && inTimeWindow(time) &&
            !isActivity && isBackgroundType && !needsInput && ACTIONS.contains(normalize(button));
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String plain = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return plain.trim().replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }
}
