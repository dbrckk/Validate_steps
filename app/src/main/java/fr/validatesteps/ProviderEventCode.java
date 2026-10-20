package fr.validatesteps;

/** Strict bounded log codes; never persist arbitrary package or notification text. */
public final class ProviderEventCode {
    private ProviderEventCode() {}

    public static String code(String packageName) {
        if (ProviderPackages.WEWARD.equals(packageName)) return "WEWARD";
        if (ProviderPackages.MACADAM.equals(packageName)) return "MACADAM";
        if (ProviderPackages.WINWALK.equals(packageName)) return "WINWALK";
        if (ProviderPackages.SWEATCOIN.equals(packageName)) return "SWEATCOIN";
        return "";
    }
}
