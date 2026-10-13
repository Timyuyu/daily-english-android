package com.dailyenglish.app;

import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,application=DailyApp.class)
public class SentenceBookTest {
    @Test public void booksPersistDeduplicateAndDeleteOnlyTheSelectedEntry() {
        Context context=RuntimeEnvironment.getApplication();context.deleteDatabase("sentences.db");context.deleteDatabase("vocabulary.db");
        String first="I don't have to rush.",second="Small steps still count.";
        try(var book=new SentenceBook(context)) {
            assertTrue(book.add(first,"First article"));assertTrue(book.add(second,"Second article"));assertFalse(book.add(first,"Duplicate must not overwrite"));
        }
        try(var book=new SentenceBook(context);var words=new Notebook(context)) {
            assertEquals(2,book.list().size());book.remove(first);assertEquals(second,book.list().get(0).english());assertEquals("Second article",book.list().get(0).title());
            words.add(new OfflineDictionary.Entry("boundary","n. 边界"),first);words.add(new OfflineDictionary.Entry("calm","a. 平静的"),second);
            var calm=words.list().stream().filter(w->w.word().equals("calm")).findFirst().orElseThrow();words.grade(calm,true,1000);words.remove("boundary");
        }
        try(var book=new SentenceBook(context);var words=new Notebook(context)) {
            assertEquals(1,book.list().size());assertEquals(1,words.list().size());assertEquals("calm",words.list().get(0).word());assertEquals(1,words.list().get(0).streak());assertEquals(86_401_000L,words.list().get(0).due());
            book.remove(second);assertTrue(book.list().isEmpty());assertEquals(1,words.list().size());
        }
    }
}
