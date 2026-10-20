package fr.validatesteps;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe single-result gate for asynchronous Health Connect callbacks.
 *
 * A provider callback can arrive after the 25s worker deadline or arrive
 * more than once. Closing the gate freezes a consistent status/count pair;
 * a delayed callback must never turn a timed-out audit into success.
 */
public final class AuditReadGate {
    public static final class Snapshot {
        public final Long steps;
        public final String status;
        Snapshot(Long steps, String status) {
            this.steps = steps;
            this.status = status;
        }
    }

    private final CountDownLatch completed = new CountDownLatch(1);
    private boolean closed;
    private Long steps;
    private String status = "read_timeout";

    public synchronized boolean accept(Long count, String resultStatus) {
        if (closed) return false;
        closed = true;
        steps = count;
        status = StepValuePolicy.storedStatus(count,
            resultStatus == null ? "unexpected_error" : resultStatus);
        completed.countDown();
        return true;
    }

    /** Waits for the first callback and freezes the outcome on timeout. */
    public Snapshot await(long timeout, TimeUnit unit) throws InterruptedException {
        completed.await(timeout, unit);
        synchronized (this) {
            closed = true;
            return new Snapshot(steps, status);
        }
    }

    /** Stops accepting results after an interrupt or a synchronous exception. */
    public synchronized Snapshot fail(String reason) {
        if (!closed) {
            closed = true;
            status = reason == null ? "unexpected_error" : reason;
            steps = null;
            completed.countDown();
        }
        return new Snapshot(steps, status);
    }
}
