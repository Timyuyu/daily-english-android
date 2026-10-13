package com.dailyenglish.app;

import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,application=DailyApp.class)
public class NotebookTest {
    @Test public void meaningSeparatesPartOfSpeechWithoutInventingUnknownLabels() {
        assertEquals("名词 / 及物动词",Notebook.partsOfSpeech("n. 边界\nvt. 限定"));
        assertEquals("边界\n限定",Notebook.translation("n. 边界\nvt. 限定"));
        assertEquals("形容词",Notebook.partsOfSpeech("a. 舒适的\nadj. 自在的"));
        assertEquals("未标注",Notebook.partsOfSpeech("美国 U.S.A.；I am 的缩略形式"));
        assertEquals("美国 U.S.A.",Notebook.translation("美国 U.S.A."));
    }
    @Test public void vocabularyPersistsDeduplicatesAndSchedulesBothOutcomes() {
        Context context=RuntimeEnvironment.getApplication();context.deleteDatabase("vocabulary.db");
        var entry=new OfflineDictionary.Entry("boundary","n. 边界；界限");
        try(var notebook=new Notebook(context)) {
            assertTrue(notebook.add(entry,"Healthy boundaries matter."));
            assertFalse(notebook.add(entry,"Must not overwrite original context."));
            var word=notebook.list().get(0);assertEquals("Healthy boundaries matter.",word.sentence());
            assertTrue(Notebook.matches(" BOUNDARY ",word.word()));assertFalse(Notebook.matches("boundry",word.word()));
            notebook.grade(word,true,1000);assertEquals(86_401_000L,notebook.list().get(0).due());
        }
        try(var notebook=new Notebook(context)) {
            var word=notebook.list().get(0);assertEquals(1,word.streak());
            notebook.grade(word,false,2000);assertEquals(602000L,notebook.list().get(0).due());assertEquals(0,notebook.list().get(0).streak());
            notebook.remove(word.word());assertTrue(notebook.list().isEmpty());
        }
        assertTrue(Notebook.matches("DON’T","don't"));
        assertEquals(30L*86_400_000,Notebook.nextDue(true,50,0));
    }
}
