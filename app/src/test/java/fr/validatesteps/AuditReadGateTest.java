package fr.validatesteps;

import static org.junit.Assert.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public final class AuditReadGateTest {
    @Test public void successfulReadIsImmutable() throws Exception {
        AuditReadGate gate = new AuditReadGate();
        assertTrue(gate.accept(1327L, "steps_read"));
        assertFalse(gate.accept(9999L, "steps_read"));
        AuditReadGate.Snapshot result = gate.await(1, TimeUnit.MILLISECONDS);
        assertEquals("steps_read", result.status);
        assertEquals(Long.valueOf(1327), result.steps);
    }

    @Test public void timeoutCannotBecomeSuccessAfterDeadline() throws Exception {
        AuditReadGate gate = new AuditReadGate();
        AuditReadGate.Snapshot result = gate.await(0, TimeUnit.MILLISECONDS);
        assertEquals("read_timeout", result.status);
        assertNull(result.steps);
        assertFalse(gate.accept(5000L, "steps_read"));
        assertEquals("read_timeout", gate.await(0, TimeUnit.MILLISECONDS).status);
    }

    @Test public void invalidSuccessCountIsRejected() throws Exception {
        AuditReadGate gate = new AuditReadGate();
        gate.accept(-4L, "steps_read");
        AuditReadGate.Snapshot result = gate.await(0, TimeUnit.MILLISECONDS);
        assertEquals("invalid_steps_data", result.status);
    }

    @Test public void exceptionClosesGateAndBlocksDelayedCallback() {
        AuditReadGate gate = new AuditReadGate();
        assertEquals("unexpected_error", gate.fail("unexpected_error").status);
        assertFalse(gate.accept(10L, "steps_read"));
    }

    @Test public void firstResultWinsEvenIfLaterFailureArrives() throws Exception {
        AuditReadGate gate = new AuditReadGate();
        assertTrue(gate.accept(0L, "steps_read"));
        assertEquals("steps_read", gate.fail("interrupted").status);
        assertEquals(Long.valueOf(0), gate.await(0, TimeUnit.MILLISECONDS).steps);
    }
}
