package fr.validatesteps;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Data;
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
 * Durable no-UI reader. Work input is the date of the alarm, not the execution
 * date; WorkManager may postpone it until after midnight.
 */
public final class AuditWorker extends Worker {
    private static final String INPUT_DAY = "audit_day";
    private static final String QUEUE = "step-audit-serialized";
    public AuditWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void enqueue(Context context, LocalDate day) {
        Data data = new Data.Builder().putString(INPUT_DAY, day.toString()).build();
        OneTimeWorkRequest job = new OneTimeWorkRequest.Builder(AuditWorker.class)
            .setInputData(data).build();
        // Unlike KEEP, queue a new retry when an earlier read is still running.
        // Unlike REPLACE, do not interrupt an in-flight Health Connect read.
        WorkManager.getInstance(context).enqueueUniqueWork(
            QUEUE, ExistingWorkPolicy.APPEND_OR_REPLACE, job);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        RunStore store = new RunStore(context);
        if (!store.enabled()) return Result.success();

        LocalDate now = LocalDate.now();
        LocalDate day = AuditDay.resolve(getInputData().getString(INPUT_DAY), now);
        if (day == null || !store.begin(day)) return Result.success();
        NightTimeline timeline = new NightTimeline(context);
        timeline.add(NightTimeline.Event.AUDIT_STARTED, day);

        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicReference<Long> steps = new AtomicReference<>();
        final AtomicReference<String> statusRef = new AtomicReference<>("read_timeout");
        try {
            HealthReader.queryDay(context, day, (value, status) -> {
                steps.set(value);
                statusRef.set(status);
                completed.countDown();
            });
            if (!completed.await(25, TimeUnit.SECONDS))
                statusRef.set("read_timeout");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            statusRef.set("interrupted");
        } catch (RuntimeException unexpected) {
            statusRef.set("unexpected_error");
        }

        String status = StepValuePolicy.storedStatus(steps.get(), statusRef.get());
        Long count = "steps_read".equals(status) ? steps.get() : null;
        if (!store.finish(day, count, status)) return Result.success();

        boolean isCurrentDay = day.equals(LocalDate.now());
        boolean missingPermission = "permission_missing".equals(status) ||
            "health_connect_unavailable".equals(status) ||
            "background_health_unsupported".equals(status);
        if ("steps_read".equals(status) || missingPermission) {
            timeline.add("steps_read".equals(status)
                ? NightTimeline.Event.AUDIT_OK : NightTimeline.Event.AUDIT_BLOCKED, day);
            if (isCurrentDay) DailyScheduler.cancelRecovery(context);
            Notifier.show(context, day, count, status);
        } else if (isCurrentDay) {
            timeline.add(NightTimeline.Event.AUDIT_RETRY_NEEDED, day);
            DailyScheduler.scheduleRecovery(context, false);
            if (!RetryPolicy.canRecover(ZonedDateTime.now()))
                Notifier.show(context, day, null, status);
        } else {
            timeline.add(NightTimeline.Event.AUDIT_OLD_DAY, day);
            // A late read is still attributed correctly, but no expired
            // third-party reward can be claimed on the next day.
            Notifier.show(context, day, null, status);
        }
        return Result.success();
    }
}
