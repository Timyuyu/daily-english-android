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
