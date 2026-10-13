package com.dailyenglish.app;

import android.content.Context;
import androidx.work.*;
import java.time.*;
import java.util.concurrent.TimeUnit;

public final class Scheduler {
    private static Constraints network() { return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(); }
    public static void ensure(Context context) {
        WorkManager manager = WorkManager.getInstance(context);
        manager.enqueueUniquePeriodicWork("daily-catchup", ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(GenerateWorker.class, 15, TimeUnit.MINUTES).setConstraints(network())
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build());
        Instant now = Instant.now();
        SlotClock.Slot next = SlotClock.next(now);
        manager.enqueueUniqueWork("scheduled-" + next.key(), ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(GenerateWorker.class).setConstraints(network())
                        .setInitialDelay(Duration.between(now, next.time().toInstant()).toMillis(), TimeUnit.MILLISECONDS)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build());
    }
    public static void catchUp(Context context) {
        ensure(context);
        WorkManager.getInstance(context).enqueueUniqueWork("open-catchup", ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(GenerateWorker.class).setConstraints(network())
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build());
    }
}
