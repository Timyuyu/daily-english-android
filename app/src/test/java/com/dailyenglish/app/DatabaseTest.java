package com.dailyenglish.app;

import android.app.Application;
import android.content.Context;
import androidx.room.Room;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class DatabaseTest {
    @Test public void schemaMigrationPreservesExistingArticleAndFavorites() throws Exception {
        Context context=RuntimeEnvironment.getApplication();String name="upgrade-v1.db";context.deleteDatabase(name);
        var path=context.getDatabasePath(name);path.getParentFile().mkdirs();
        try(var old=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path,null)) {
            old.execSQL("CREATE TABLE articles (id TEXT NOT NULL PRIMARY KEY,date TEXT NOT NULL,period TEXT NOT NULL,title TEXT NOT NULL,english TEXT NOT NULL,chinese TEXT NOT NULL,expressions TEXT NOT NULL,source TEXT NOT NULL,favorite INTEGER NOT NULL,createdAt INTEGER NOT NULL)");
            old.execSQL("CREATE UNIQUE INDEX index_articles_date_period ON articles (date,period)");
            old.execSQL("INSERT INTO articles VALUES ('saved','2026-10-13','DAY','Saved title','Original English','完整中文','[]','saved-source',1,1234)");old.setVersion(1);
        }
        ArticleDb db=Room.databaseBuilder(context,ArticleDb.class,name).addMigrations(ArticleDb.MIGRATION_1_2).allowMainThreadQueries().build();
        try {
            Article article=db.articles().get("saved");assertEquals("Original English",article.english);assertEquals("完整中文",article.chinese);assertTrue(article.favorite);
            db.articles().remove(article.id);assertTrue(db.articles().deleted(article.id));assertNull(db.articles().get(article.id));
        } finally {db.close();context.deleteDatabase(name);}
    }
    @Test public void removedArticlesStayRemovedAcrossSeedingAndGeneration() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        ArticleDb db=Room.inMemoryDatabaseBuilder(context,ArticleDb.class).allowMainThreadQueries().build();
        try {
            db.seed(context);Article imported=db.articles().all().get(0);db.articles().remove(imported.id);
            Article daily=new Article();daily.id="2026-10-14:DAY";daily.date="2026-10-14";daily.period="DAY";
            db.articles().insert(daily);db.articles().remove(daily.id);
            db.seed(context);assertEquals(-1,db.articles().insert(daily));assertEquals(-1,db.articles().insert(imported));
            assertEquals(19,db.articles().count());assertNull(db.articles().get(imported.id));assertTrue(db.articles().deleted(daily.id));
        } finally {db.close();}
    }
    private int count(ArticleDb db,String period) {
        try(var cursor=db.query("SELECT COUNT(*) FROM articles WHERE period=?",new Object[]{period})){cursor.moveToFirst();return cursor.getInt(0);}
    }
    @Test public void upgradeClassifiesImportedRowsWithoutLosingSavedContentOrFavorites() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        ArticleDb db=Room.inMemoryDatabaseBuilder(context,ArticleDb.class).allowMainThreadQueries().build();
        try {
            Article old=new Article();old.id="archive-3260206dac56b195";old.date=old.id;old.period="ARCHIVE";old.favorite=true;old.english="Saved original English.";
            db.articles().insert(old);db.seed(context);db.seed(context);
            assertEquals(20,db.articles().count());assertEquals(13,count(db,"DAY"));assertEquals(7,count(db,"NIGHT"));assertEquals(0,count(db,"ARCHIVE"));
            Article migrated=db.articles().get(old.id);assertTrue(migrated.favorite);assertEquals(old.english,migrated.english);assertEquals("DAY",migrated.period);
        } finally {db.close();}
    }
    @Test public void archiveAndDailyRowsSurviveRepeatedImportsAndGeneration() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        ArticleDb db=Room.inMemoryDatabaseBuilder(context,ArticleDb.class).allowMainThreadQueries().build();
        try {
            db.seed(context);
            int imported=db.articles().count();assertEquals(20,imported);
            Article a=new Article();a.id="2026-10-13:DAY";a.date="2026-10-13";a.period="DAY";a.title="original";
            assertTrue(db.articles().insert(a)>0);db.articles().favorite(a.id,true);
            a.title="duplicate";assertEquals(-1,db.articles().insert(a));
            Article collision=new Article();collision.id="different";collision.date=a.date;collision.period=a.period;
            assertEquals(-1,db.articles().insert(collision));
            db.seed(context);assertTrue(db.articles().get(a.id).favorite);assertEquals("original",db.articles().get(a.id).title);
            assertEquals(21,db.articles().count());
        } finally { db.close(); }
    }
}
