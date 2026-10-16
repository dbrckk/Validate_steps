package fr.validatesteps;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Durable no-UI worker. No network, no reward provider access, no screen unlock.
 * WorkManager manages process lifecycle; AlarmManager defines the wall-clock trigger.
 */
public final class AuditWorker extends Worker {
    public AuditWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void enqueue(Context context) {
        String uniqueName = "daily-step-audit-" + LocalDate.now();
        OneTimeWorkRequest job = new OneTimeWorkRequest.Builder(AuditWorker.class).build();
        WorkManager.getInstance(context).enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, job);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        RunStore store = new RunStore(context);
        if (!store.enabled()) return Result.success();

        LocalDate day = LocalDate.now();
        if (store.successfulOn(day)) {
            DailyScheduler.cancelRecovery(context);
            return Result.success();
        }

        store.start(day);
        final CountDownLatch done = new CountDownLatch(1);
        final AtomicReference<Long> steps = new AtomicReference<>();
        final AtomicReference<String> state = new AtomicReference<>("read_timeout");
        try {
            HealthReader.queryDay(context, day, (value, status) -> {
                steps.set(value);
                state.set(status);
                done.countDown();
            });
            if (!done.await(25, TimeUnit.SECONDS)) {
                state.set("read_timeout");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            state.set("interrupted");
        } catch (RuntimeException unexpected) {
            state.set("unexpected_error");
        }

        String status = state.get();
        Long count = "steps_read".equals(status) ? steps.get() : null;
        store.finish(day, count, status);

        boolean permanent = "permission_missing".equals(status) ||
            "health_connect_unavailable".equals(status);
        if ("steps_read".equals(status) || permanent) {
            DailyScheduler.cancelRecovery(context);
            Notifier.show(context, count, status);
        } else {
            DailyScheduler.scheduleRecovery(context, false);
            if (!RetryPolicy.canRecover(ZonedDateTime.now())) {
                Notifier.show(context, null, status);
            }
        }
        return Result.success();
    }
}
