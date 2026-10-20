package fr.validatesteps;

import java.util.Set;

/** Explicit, one-tap, user-initiated UI script. Never act on ads or login. */
public final class GuidedClaimPolicy {
    public static final String[] ORDER = {
        ProviderPackages.WEWARD, ProviderPackages.MACADAM,
        ProviderPackages.WINWALK, ProviderPackages.SWEATCOIN
    };
    private static final Set<String> WEWARD_LABELS = Set.of(
        "valider mes pas", "valider les pas", "validate my steps");
    private static final Set<String> MACADAM_LABELS = Set.of(
        "valider mes pas", "valider les pas", "valider mes pas du jour",
        "validate steps", "validate my steps");
    private static final Set<String> WINWALK_LABELS = Set.of(
        "collect step coins", "collect coins from steps",
        "recuperer les pieces de mes pas", "collecter les pieces de mes pas");

    private GuidedClaimPolicy() {}

    public static boolean isTarget(String pkg) {
        if (pkg == null) return false;
        for (String value : ORDER) if (value.equals(pkg)) return true;
        return false;
    }

    public static boolean canClick(String pkg, String label) {
        String text = normalize(label);
        if (ProviderPackages.WEWARD.equals(pkg)) return WEWARD_LABELS.contains(text);
        if (ProviderPackages.MACADAM.equals(pkg)) return MACADAM_LABELS.contains(text);
        if (ProviderPackages.WINWALK.equals(pkg)) return WINWALK_LABELS.contains(text);
        // Sweatcoin normally converts in background; never assume a collect button.
        return false;
    }

    public static String normalize(String text) {
        return StepLabelNormalizer.normalize(text);
    }

}
