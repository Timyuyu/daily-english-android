package com.dailyenglish.app;

import android.content.*;
import android.database.sqlite.*;
import java.util.*;

public final class Notebook extends SQLiteOpenHelper {
    private static final java.util.regex.Pattern POS=java.util.regex.Pattern.compile("(?i)(?<![A-Za-z.])(n|v|vt|vi|a|adj|ad|adv|prep|pron|conj|num|art|aux|int|interj|abbr)\\.(?=\\s|$)");
    public static String partsOfSpeech(String meaning) {
        Map<String,String> labels=Map.ofEntries(Map.entry("n","名词"),Map.entry("v","动词"),Map.entry("vt","及物动词"),Map.entry("vi","不及物动词"),Map.entry("a","形容词"),Map.entry("adj","形容词"),Map.entry("ad","副词"),Map.entry("adv","副词"),Map.entry("prep","介词"),Map.entry("pron","代词"),Map.entry("conj","连词"),Map.entry("num","数词"),Map.entry("art","冠词"),Map.entry("aux","助动词"),Map.entry("int","感叹词"),Map.entry("interj","感叹词"),Map.entry("abbr","缩略词"));
        var matcher=POS.matcher(meaning);Set<String> parts=new LinkedHashSet<>();while(matcher.find())parts.add(labels.get(matcher.group(1).toLowerCase(Locale.ROOT)));
        return parts.isEmpty()?"未标注":String.join(" / ",parts);
    }
    public static String translation(String meaning) { return POS.matcher(meaning).replaceAll("").replaceAll("(?m)^[ \\t]+","").trim(); }
    public record Word(String word,String meaning,String sentence,int streak,long due) {}
    public Notebook(Context context) { super(context.getApplicationContext(),"vocabulary.db",null,1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE words(word TEXT PRIMARY KEY NOT NULL, meaning TEXT NOT NULL, sentence TEXT NOT NULL, streak INTEGER NOT NULL DEFAULT 0, due INTEGER NOT NULL DEFAULT 0)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {}
    public boolean add(OfflineDictionary.Entry entry,String sentence) {
        ContentValues values=new ContentValues();values.put("word",entry.word());values.put("meaning",entry.meaning());values.put("sentence",sentence);
        return getWritableDatabase().insertWithOnConflict("words",null,values,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
    }
    public List<Word> list() {
        List<Word> result=new ArrayList<>();
        try(var cursor=getReadableDatabase().rawQuery("SELECT word,meaning,sentence,streak,due FROM words ORDER BY due,word",null)) {
            while(cursor.moveToNext())result.add(new Word(cursor.getString(0),cursor.getString(1),cursor.getString(2),cursor.getInt(3),cursor.getLong(4)));
        }
        return result;
    }
    public static boolean matches(String answer,String word) { return OfflineDictionary.normalize(answer).equals(OfflineDictionary.normalize(word)); }
    public static long nextDue(boolean correct,int streak,long now) {
        int[] days={1,3,7,14,30};return now+(correct?days[Math.min(Math.max(streak,0),days.length-1)]*86_400_000L:600_000L);
    }
    public void grade(Word word,boolean correct,long now) {
        ContentValues values=new ContentValues();values.put("streak",correct?word.streak()+1:0);values.put("due",nextDue(correct,word.streak(),now));
        getWritableDatabase().update("words",values,"word=?",new String[]{word.word()});
    }
    public void remove(String word) { getWritableDatabase().delete("words","word=?",new String[]{word}); }
}
