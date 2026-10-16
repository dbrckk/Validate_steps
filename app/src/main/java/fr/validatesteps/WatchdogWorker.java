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
import java.util.concurrent.TimeUnit;

/**
 * Two one-shot WorkManager safeguards per day (22:20 and 23:15).
 * Unlike the former periodic watchdog, does not wake all day long.
 * Execution may be late under Android's battery policies.
 */
public final class WatchdogWorker extends Worker {
    private static final String TAG = "step-audit-nightly-fallback";
    // v0.4 scheduled a periodic worker with this unique name. WorkManager
    // persists it across APK upgrades, so we must cancel it explicitly.
    private static final String LEGACY_PERIODIC = "recovery-watchdog";
    private static final String DAY = "scheduled_audit_date";
    private static final String UNIQUE_PREFIX = "step-audit-fallback-";

    public WatchdogWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void configure(Context context, boolean enabled, ZonedDateTime next22) {
        WorkManager manager = WorkManager.getInstance(context);
        manager.cancelUniqueWork(LEGACY_PERIODIC);
        if (!enabled) {
            manager.cancelAllWorkByTag(TAG);
            return;
        }
        scheduleOne(manager, next22, SafetyNetPlan.early(next22), "early");
        scheduleOne(manager, next22, SafetyNetPlan.late(next22), "late");
    }

    private static void scheduleOne(WorkManager manager, ZonedDateTime next22,
            ZonedDateTime fallback, String slot) {
        Data input = new Data.Builder()
            .putString(DAY, next22.toLocalDate().toString()).build();
        long delay = SafetyNetPlan.delayMillis(ZonedDateTime.now(), fallback);
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WatchdogWorker.class)
            .setInputData(input)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(TAG)
            .build();
        manager.enqueueUniqueWork(UNIQUE_PREFIX + next22.toLocalDate() + "-" + slot,
            ExistingWorkPolicy.REPLACE, request);
    }

    @NonNull @Override public Result doWork() {
        Context context = getApplicationContext();
        RunStore store = new RunStore(context);
        LocalDate today = LocalDate.now();
        LocalDate scheduled = AuditDay.resolve(getInputData().getString(DAY), today);
        // Re-arm tomorrow even if the 22:00 AlarmManager broadcast was dropped.
        // The next two fallbacks are scheduled once for that future date.
        if (store.enabled()) DailyScheduler.schedule(context);
        RewardActionListener.inspectConnectedNow();
        // A delayed fallback must never attribute yesterday's steps to today.
        if (scheduled == null || !scheduled.equals(today)) return Result.success();
        if (CatchupPolicy.shouldAttempt(ZonedDateTime.now(), store.enabled(),
                store.successfulOn(today), store.permanentlyUnavailableOn(today))) {
            AuditWorker.enqueue(context, today);
        }
        return Result.success();
    }
}
