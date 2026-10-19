package fr.validatesteps;

import android.content.Context;
import android.content.pm.PackageManager;

/** Exact official Android package identifiers; no broad installed-app inventory. */
public final class ProviderPackages {
    public static final String WEWARD = "com.weward";
    public static final String MACADAM = "com.macadamapp";
    public static final String WINWALK = "com.winwalk.android";
    public static final String SWEATCOIN = "in.sweatco.app";

    private ProviderPackages() {}

    public static boolean installed(Context context, String packageName) {
        if (!GuidedClaimPolicy.isTarget(packageName)) return false;
        try {
            context.getPackageManager().getPackageInfo(packageName,
                PackageManager.PackageInfoFlags.of(0));
            return true;
        } catch (PackageManager.NameNotFoundException missing) {
            return false;
        }
    }

    public static String diagnostic(Context context) {
        return "Applications installées : WeWard " +
            (installed(context, WEWARD) ? "oui" : "non") +
            ", Macadam " + (installed(context, MACADAM) ? "oui" : "non") +
            ", Winwalk " + (installed(context, WINWALK) ? "oui" : "non");
    }
}
