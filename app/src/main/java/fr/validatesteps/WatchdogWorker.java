package fr.validatesteps;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Independent no-UI safety net for an alarm missed by device power management.
 * Periodic WorkManager execution is INEXACT and never substitutes exact alarms.
 */
public final class WatchdogWorker extends Worker {
    private static final String UNIQUE = "recovery-watchdog";

    public WatchdogWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void configure(Context context, boolean enabled) {
        WorkManager manager = WorkManager.getInstance(context);
        if (!enabled) {
            manager.cancelUniqueWork(UNIQUE);
            return;
        }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                WatchdogWorker.class, 30, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES)
            .build();
        manager.enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        RunStore store = new RunStore(context);
        LocalDate day = LocalDate.now();
        if (CatchupPolicy.shouldAttempt(ZonedDateTime.now(), store.enabled(),
                store.successfulOn(day), store.permanentlyUnavailableOn(day))) {
            AuditWorker.enqueue(context, day);
        }
        return Result.success();
    }
}
