package fr.validatesteps;

import static org.junit.Assert.*;
import org.junit.Test;

public final class AccessibilityScanBudgetTest {
    @Test public void rootAndMaximumDepthAreIncluded() {
        AccessibilityScanBudget budget = new AccessibilityScanBudget();
        assertTrue(budget.visit(0));
        assertTrue(budget.visit(AccessibilityScanBudget.MAX_DEPTH));
        assertFalse(budget.visit(AccessibilityScanBudget.MAX_DEPTH + 1));
        assertFalse(budget.visit(-1));
        assertEquals(2, budget.visited());
    }

    @Test public void neverScansBeyondGlobalNodeLimit() {
        AccessibilityScanBudget budget = new AccessibilityScanBudget();
        for (int i = 0; i < AccessibilityScanBudget.MAX_NODES; i++)
            assertTrue(budget.visit(3));
        assertTrue(budget.exhausted());
        for (int i = 0; i < 20; i++)
            assertFalse(budget.visit(0));
        assertEquals(AccessibilityScanBudget.MAX_NODES, budget.visited());
    }

    @Test public void eachEventCanReceiveAFreshBudget() {
        AccessibilityScanBudget full = new AccessibilityScanBudget();
        for (int i = 0; i < AccessibilityScanBudget.MAX_NODES; i++)
            full.visit(0);
        assertFalse(full.visit(0));
        AccessibilityScanBudget next = new AccessibilityScanBudget();
        assertTrue(next.visit(0));
        assertEquals(1, next.visited());
    }

    @Test public void perNodeChildLimitIsConservative() {
        assertTrue(AccessibilityScanBudget.MAX_CHILDREN > 0);
        assertTrue(AccessibilityScanBudget.MAX_CHILDREN <= AccessibilityScanBudget.MAX_NODES);
    }
}
