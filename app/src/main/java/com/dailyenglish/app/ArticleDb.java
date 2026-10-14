package com.dailyenglish.app;

import android.content.Context;
import androidx.room.*;
import org.json.*;
import java.nio.charset.StandardCharsets;

@Database(entities = {Article.class,DeletedArticle.class}, version = 2, exportSchema = true)
public abstract class ArticleDb extends RoomDatabase {
    public static final androidx.room.migration.Migration MIGRATION_1_2=new androidx.room.migration.Migration(1,2) {
        @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS deleted_articles (id TEXT NOT NULL, PRIMARY KEY(id))");
        }
    };
    private static volatile ArticleDb instance;
    public abstract ArticleDao articles();
    public static ArticleDb get(Context context) {
        if (instance == null) synchronized (ArticleDb.class) {
            if (instance == null) instance = Room.databaseBuilder(context.getApplicationContext(), ArticleDb.class, "daily-english.db").addMigrations(MIGRATION_1_2).build();
        }
        return instance;
    }
    public synchronized void seed(Context context) throws Exception {
        try (var stream = context.getAssets().open("history.json")) {
            JSONArray data = new JSONArray(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            runInTransaction(() -> {
                for (int i = 0; i < data.length(); i++) {
                    JSONObject j = data.optJSONObject(i);
                    Article a = new Article();
                    a.id = j.optString("id");
                    // Imported content is grouped by topic; its original publication date is not invented.
                    a.date = a.id;
                    a.period = j.optString("period");
                    a.title = j.optString("title");
                    a.english = j.optString("english");
                    a.chinese = j.optString("chinese");
                    a.expressions = j.optJSONArray("expressions").toString();
                    a.source = j.optString("source");
                    a.createdAt = j.optLong("createdAt");
                    articles().insert(a);
                    articles().classifyImported(a.id,a.period);
                }
            });
        }
    }
}
