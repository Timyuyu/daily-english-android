package com.dailyenglish.app;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.work.Configuration;
import java.util.concurrent.*;

public class DailyApp extends Application implements Configuration.Provider {
    public static final ExecutorService IO = Executors.newSingleThreadExecutor();
    @NonNull @Override public Configuration getWorkManagerConfiguration() { return new Configuration.Builder().build(); }
    @Override public void onCreate() {
        super.onCreate();
        IO.execute(() -> {
            try { ArticleDb.get(this).seed(this); }
            catch (Exception e) { getSharedPreferences("settings", MODE_PRIVATE).edit().putString("last_error", "篇章载入失败，请重新打开应用").apply(); }
        });
        Scheduler.ensure(this);
    }
}
