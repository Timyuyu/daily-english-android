package com.dailyenglish.app;

import android.content.*;
import android.database.sqlite.*;
import java.util.*;

public final class SentenceBook extends SQLiteOpenHelper {
    public record Sentence(String english,String title) {}
    public SentenceBook(Context context) { super(context.getApplicationContext(),"sentences.db",null,1); }
    @Override public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE sentences(english TEXT PRIMARY KEY NOT NULL, title TEXT NOT NULL)"); }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {}
    public boolean add(String english,String title) {
        ContentValues values=new ContentValues();values.put("english",english);values.put("title",title);
        return getWritableDatabase().insertWithOnConflict("sentences",null,values,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
    }
    public List<Sentence> list() {
        List<Sentence> result=new ArrayList<>();
        try(var cursor=getReadableDatabase().rawQuery("SELECT english,title FROM sentences ORDER BY rowid DESC",null)) {
            while(cursor.moveToNext())result.add(new Sentence(cursor.getString(0),cursor.getString(1)));
        }
        return result;
    }
    public void remove(String english) { getWritableDatabase().delete("sentences","english=?",new String[]{english}); }
}
