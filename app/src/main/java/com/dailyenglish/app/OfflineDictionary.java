package com.dailyenglish.app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class OfflineDictionary {
    public record Entry(String word,String meaning) {}
    public static String normalize(String word) { return word.trim().replace('’','\'').toLowerCase(Locale.ROOT); }
    private static synchronized File file(Context context) throws IOException {
        File file=new File(context.getNoBackupFilesDir(),"ecdict-v1.db");
        if(!file.exists()) {
            File temporary=new File(file.getPath()+".part");
            try(var input=context.getAssets().open("dictionary.db")) {
                Files.copy(input,temporary.toPath(),StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temporary.toPath(),file.toPath(),StandardCopyOption.REPLACE_EXISTING);
        }
        return file;
    }
    public static Entry lookup(Context context,String input) throws IOException {
        String word=normalize(input);
        if(!word.matches("[a-z]+(?:['-][a-z]+)*"))return null;
        try(var db=SQLiteDatabase.openDatabase(file(context).getPath(),null,SQLiteDatabase.OPEN_READONLY)) {
            String[] forms=word.endsWith("'s")?new String[]{word,word.substring(0,word.length()-2)}:new String[]{word};
            for(String form:forms) {
                try(var cursor=db.rawQuery("SELECT word,meaning FROM entries WHERE word=? UNION ALL SELECT e.word,e.meaning FROM aliases a JOIN entries e ON e.word=a.base WHERE a.form=? LIMIT 1",new String[]{form,form})) {
                    if(cursor.moveToFirst())return new Entry(cursor.getString(0),cursor.getString(1));
                }
            }
            return null;
        }
    }
}
