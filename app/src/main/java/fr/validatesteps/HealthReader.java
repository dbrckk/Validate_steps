package fr.validatesteps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.StepsRecord;
import android.os.OutcomeReceiver;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.BiConsumer;

/** Reads real Health Connect aggregates. Never writes or synthesizes steps. */
public final class HealthReader {
    public static final String READ_STEPS = "android.permission.health.READ_STEPS";
    public static final String READ_BACKGROUND =
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND";

    private HealthReader() {}

    public static void queryToday(Context context, BiConsumer<Long, String> result) {
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
        ZonedDateTime start = LocalDate.now(zone).atStartOfDay(zone);
        TimeInstantRangeFilter range = new TimeInstantRangeFilter.Builder()
            .setStartTime(start.toInstant())
            .setEndTime(java.time.Instant.now())
            .build();
        AggregateRecordsRequest<Long> request = new AggregateRecordsRequest.Builder<Long>(range)
            .addAggregationType(StepsRecord.STEPS_COUNT_TOTAL)
            .build();
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
