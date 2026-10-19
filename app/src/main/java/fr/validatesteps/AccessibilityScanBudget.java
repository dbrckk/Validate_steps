package fr.validatesteps;

/**
 * Hard safety bound for traversal of third-party accessibility trees.
 *
 * A depth-only guard does not prevent traversing thousands of visible nodes
 * at each level. Each event gets a fresh shared budget, consumed at most once
 * per visited node, preventing expensive UI-thread scans and app freezes.
 */
public final class AccessibilityScanBudget {
    public static final int MAX_NODES = 400;
    public static final int MAX_DEPTH = 20;
    public static final int MAX_CHILDREN = 60;

    private int visited;

    /** Depth starts at zero for the root. Refuse any node beyond the limits. */
    public boolean visit(int depth) {
        if (depth < 0 || depth > MAX_DEPTH || visited >= MAX_NODES) return false;
        visited++;
        return true;
    }

    public boolean exhausted() {
        return visited >= MAX_NODES;
    }

    public int visited() {
        return visited;
    }
}
