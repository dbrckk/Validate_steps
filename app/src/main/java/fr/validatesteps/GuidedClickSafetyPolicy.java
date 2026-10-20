package fr.validatesteps;

/** Pure, fail-closed selection of an accessibility button or close wrapper. */
public final class GuidedClickSafetyPolicy {
    // Never climb to a large, unrelated container which can do something else.
    public static final int MAX_PARENT_HOPS = 2;

    private GuidedClickSafetyPolicy() {}

    public static boolean permitted(String targetPackage, String candidatePackage,
            int parentHops, boolean clickable, boolean enabled, boolean visible) {
        return GuidedClaimPolicy.isTarget(targetPackage)
            && targetPackage.equals(candidatePackage)
            && parentHops >= 0 && parentHops <= MAX_PARENT_HOPS
            && clickable && enabled && visible;
    }
}
