package fr.validatesteps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ext.SdkExtensions;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.StepsRecord;
import android.os.OutcomeReceiver;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.BiConsumer;

/** Read-only deduplicated real steps; never writes fake data or contacts rewards APIs. */
public final class HealthReader {
    public static final String READ_STEPS = "android.permission.health.READ_STEPS";
    public static final String READ_BACKGROUND =
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND";
    private HealthReader() {}

    /**
     * Framework Health Connect background reads require Android 15 or
     * Android 14 U Extension 13. Otherwise permission requests cannot work.
     */
    public static boolean backgroundSupported() {
        return Build.VERSION.SDK_INT >= 35 ||
            (Build.VERSION.SDK_INT >= 34 &&
                SdkExtensions.getExtensionVersion(34) >= 13);
    }

    public static void queryDay(Context context, LocalDate day, BiConsumer<Long, String> result) {
        if (!backgroundSupported()) {
            result.accept(null, "background_health_unsupported");
            return;
        }
        if (context.checkSelfPermission(READ_STEPS) != PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(READ_BACKGROUND) != PackageManager.PERMISSION_GRANTED) {
            result.accept(null, "permission_missing");
            return;
        }
        HealthConnectManager health = context.getSystemService(HealthConnectManager.class);
        if (health == null) {
            result.accept(null, "health_connect_unavailable");
            return;
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant start = day.atStartOfDay(zone).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();
        if (Instant.now().isBefore(end)) end = Instant.now();
        if (!end.isAfter(start)) {
            result.accept(null, "invalid_time_range");
            return;
        }
        TimeInstantRangeFilter range = new TimeInstantRangeFilter.Builder()
            .setStartTime(start).setEndTime(end).build();
        AggregateRecordsRequest<Long> request = new AggregateRecordsRequest.Builder<Long>(range)
            .addAggregationType(StepsRecord.STEPS_COUNT_TOTAL).build();
        try {
            health.aggregate(request, context.getMainExecutor(),
                new OutcomeReceiver<AggregateRecordsResponse<Long>, HealthConnectException>() {
                    @Override public void onResult(AggregateRecordsResponse<Long> response) {
                        Long count = response.get(StepsRecord.STEPS_COUNT_TOTAL);
                        if (count == null) result.accept(null, "no_steps_data");
                        else result.accept(count, "steps_read");
                    }
                    @Override public void onError(HealthConnectException error) {
                        result.accept(null, "health_connect_error_" + error.getErrorCode());
                    }
                });
        } catch (RuntimeException error) {
            result.accept(null, "health_connect_request_failed");
        }
    }
}
