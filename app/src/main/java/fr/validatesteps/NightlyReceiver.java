package fr.validatesteps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.time.LocalDate;

/** Executes at 22:00 local time without starting an Activity. */
public final class NightlyReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!new RunStore(context).enabled()) return;

        // Always arm tomorrow first even if the asynchronous read later fails.
        DailyScheduler.schedule(context);
        RunStore store = new RunStore(context);
        store.start(LocalDate.now());

        PendingResult pending = goAsync();
        try {
            HealthReader.queryToday(context.getApplicationContext(), (steps, status) -> {
                try {
                    store.finish(steps, status);
                    Notifier.show(context.getApplicationContext(), steps, status);
                } finally {
                    pending.finish();
                }
            });
        } catch (RuntimeException error) {
            try {
                store.finish(null, "unexpected_error");
                Notifier.show(context.getApplicationContext(), null, "unexpected_error");
            } finally {
                pending.finish();
            }
        }
    }
}
