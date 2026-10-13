package com.dailyenglish.app;

import android.app.AlertDialog;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,application=DailyApp.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class VocabularyUiTest {
    private View find(View view,String label) {
        if(view instanceof TextView t&&t.getText().toString().equals(label))return view;
        if(view instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){View found=find(g.getChildAt(i),label);if(found!=null)return found;}
        return null;
    }
    @Test public void notebookIsOnlyAWordListAndSpeakingKeepsItOpen() throws Exception {
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try {
            var activity=controller.get();var field=MainActivity.class.getDeclaredField("vocabulary");field.setAccessible(true);var ui=(VocabularyUi)field.get(activity);
            var show=VocabularyUi.class.getDeclaredMethod("showNotebook",List.class);show.setAccessible(true);
            show.invoke(ui,List.of(new Notebook.Word("boundary","n. 边界, 分界线","context",0,0)));
            AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View root=dialog.getWindow().getDecorView();
            assertNull("Practice must be in its own section",find(root,"看中文 · 拼英文"));
            assertNotNull(find(root,"boundary"));assertNotNull(find(root,"词性：名词"));assertNotNull(find(root,"边界, 分界线"));
            View speaker=root.findViewWithTag("word-speaker-boundary");assertNotNull(speaker);
            speaker.performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertTrue("Pronunciation must not dismiss the word list",dialog.isShowing());
            assertSame(dialog,org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog());
            speaker.performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(dialog.isShowing());dialog.dismiss();
            View home=activity.getWindow().getDecorView();assertNotNull(find(home,"生词本"));assertNotNull(find(home,"精句本"));assertNotNull(find(home,"背诵复习"));assertNull(find(home,"生词本 / 背诵复习"));
        } finally {controller.pause().stop().destroy();}
    }
}
