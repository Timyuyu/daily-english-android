package com.dailyenglish.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.work.*;
import org.json.JSONException;
import java.time.*;
import java.io.IOException;

public class GenerateWorker extends Worker {
    // ponytail: one device/account, one generation lock; a server is needed for multi-device deduplication.
    private static final Object LOCK = new Object();
    public GenerateWorker(@NonNull Context context, @NonNull WorkerParameters parameters) { super(context, parameters); }
    @NonNull @Override public Result doWork() {
        synchronized (LOCK) {
            Context context = getApplicationContext();
            var prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
            Scheduler.ensure(context);
            String key;
            try { key = KeyVault.read(context); }
            catch (Exception e) { error("密钥无法读取，请在设置中重新输入"); return Result.failure(); }
            if (key.isEmpty()) { error("请先在设置中配置新的 DeepSeek 密钥"); return Result.success(); }
            Instant now = Instant.now();
            String startText = prefs.getString("start_date", SlotClock.current(now).date().toString());
            if (!prefs.contains("start_date")) prefs.edit().putString("start_date", startText).commit();
            ArticleDao dao = ArticleDb.get(context).articles();
            int added = 0;
            var pending = SlotClock.due(LocalDate.parse(startText), now);
            java.util.Collections.reverse(pending); // Show the current article before backfilling older missed days.
            for (SlotClock.Slot slot : pending) {
                if (isStopped()) return Result.retry();
                if (!KeyVault.configured(context)) return Result.success();
                if (dao.get(slot.key()) != null) continue;
                try {
                    Article article = DeepSeek.generate(key, slot, dao.recentTitles());
                    if (isStopped()) return Result.retry();
                    // The database's unique date/period index also protects inserts after crashes/retries.
                    if (dao.insert(article) != -1) { notifyArticle(article); added++; }
                    prefs.edit().putString("last_error", "").putLong("last_sync", System.currentTimeMillis()).apply();
                } catch (DeepSeek.ApiError e) {
                    error(switch (e.status) {
                        case 401, 403 -> "密钥无效或无权限，请更换密钥";
                        case 402 -> "DeepSeek 余额不足，请充值后重试";
                        case 429 -> "服务繁忙或请求频繁，将自动重试";
                        default -> "DeepSeek 请求失败（HTTP " + e.status + "），请稍后重试";
                    });
                    return e.status == 429 || e.status >= 500 ? Result.retry() : Result.failure();
                } catch (JSONException e) {
                    error("生成内容不完整或长度不符合要求，正在重新尝试");
                    return getRunAttemptCount() < 2 ? Result.retry() : Result.failure();
                } catch (IOException e) {
                    error("网络连接超时或失败，联网后自动重试"); return Result.retry();
                } catch (Exception e) {
                    error("生成暂未完成，请稍后重试"); return Result.failure();
                }
                // Keep each background job below the system time budget; periodic/open catch-up continues the backlog.
                if (added >= 4) { error("本轮已补齐4篇；如有遗漏，下轮继续补齐"); return Result.success(); }
            }
            prefs.edit().putString("last_error", "").putLong("last_sync", System.currentTimeMillis()).apply();
            return Result.success();
        }
    }
    private void error(String message) {
        getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("last_error", message).apply();
    }
    private void notifyArticle(Article article) {
        Context context = getApplicationContext();
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("new-articles", "每日英语新篇章", NotificationManager.IMPORTANCE_DEFAULT));
        Intent intent = new Intent(context, MainActivity.class).putExtra("article_id", article.id);
        PendingIntent pending = PendingIntent.getActivity(context, article.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            manager.notify(article.id.hashCode(), new Notification.Builder(context, "new-articles")
                    .setSmallIcon(R.drawable.ic_book).setContentTitle((article.period.equals("DAY") ? "白天版" : "晚上版") + "已更新")
                    .setContentText(article.title).setContentIntent(pending).setAutoCancel(true).build());
        } catch (SecurityException ignored) { /* Notification permission may be revoked during generation. */ }
    }
}
