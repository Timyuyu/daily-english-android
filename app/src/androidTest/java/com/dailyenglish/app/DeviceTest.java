package com.dailyenglish.app;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DeviceTest {
    private void screenshot(String name) throws Exception {
        Thread.sleep(2500); // Let dialog transitions and queued save toasts finish before visual inspection.
        var instrumentation=InstrumentationRegistry.getInstrumentation();var bitmap=instrumentation.getUiAutomation().takeScreenshot();
        try(var out=new java.io.FileOutputStream(new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null),name))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    @Test public void sentenceSavingAndIndividualRemovalStayOnTheCurrentList() throws Exception {
        var instrumentation=InstrumentationRegistry.getInstrumentation();Context context=instrumentation.getTargetContext();
        MainActivity activity=(MainActivity)instrumentation.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        String sentence=null;
        try {
            var selectedField=MainActivity.class.getDeclaredField("selected");selectedField.setAccessible(true);
            var current=new java.util.concurrent.atomic.AtomicReference<Article>();
            for(int i=0;i<100&&current.get()==null;i++){instrumentation.runOnMainSync(()->{try{current.set((Article)selectedField.get(activity));}catch(Exception e){throw new AssertionError(e);}});Thread.sleep(50);}
            assertNotNull(current.get());sentence=SlotClock.sentences(current.get().english).get(0);
            String other="Don't forget to take a break.";
            try(var book=new SentenceBook(context);var words=new Notebook(context)) {
                book.remove(sentence);book.remove(other);book.add(other,"Another article");words.remove("boundary");words.remove("calm");
                words.add(new OfflineDictionary.Entry("boundary","n. 边界"),"Healthy boundaries matter.");words.add(new OfflineDictionary.Entry("calm","a. 平静的"),"Keep calm.");
            }
            click("加入第1句到精句本");
            for(int i=0;i<100&&!waitNode("加入第1句到精句本").isEnabled();i++)Thread.sleep(50);
            click("加入第1句到精句本");
            for(int i=0;i<100&&!waitNode("加入第1句到精句本").isEnabled();i++)Thread.sleep(50);
            try(var book=new SentenceBook(context)){assertEquals(2,book.list().size());assertEquals(sentence,book.list().get(0).english());assertEquals(current.get().title,book.list().get(0).title());}
            click("退出全屏");click("精句本");waitNode(sentence);click("朗读精句 "+sentence);Thread.sleep(1200);waitNode("精句本");waitNode(sentence);
            screenshot("v17-sentences.png");click("移除精句 "+sentence);waitNode("全部精句 · 1 句");waitNode(other);
            assertNull(findNode(instrumentation.getUiAutomation().getRootInActiveWindow(),sentence));
            try(var book=new SentenceBook(context)){assertEquals(other,book.list().get(0).english());}
            click("移除精句 "+other);waitNode("全部精句 · 0 句");waitNode("关闭");click("关闭");
            click("生词本");waitNode("boundary");waitNode("calm");screenshot("v17-notebook.png");click("移出生词 boundary");waitNode("全部生词 · 1 词 · A–Z");waitNode("calm");
            assertNull(findNode(instrumentation.getUiAutomation().getRootInActiveWindow(),"boundary"));
            click("朗读生词 calm");Thread.sleep(1200);waitNode("calm");waitNode("关闭");
            try(var words=new Notebook(context)){assertEquals(1,words.list().size());assertEquals("calm",words.list().get(0).word());}
            click("移出生词 calm");waitNode("全部生词 · 0 词 · A–Z");click("关闭");assertFalse(activity.isFinishing());
        } finally {
            try(var book=new SentenceBook(context);var words=new Notebook(context)){if(sentence!=null)book.remove(sentence);book.remove("Don't forget to take a break.");words.remove("boundary");words.remove("calm");}
            instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();
        }
    }
    private View speaker(View view) {
        if("朗读第1句，美式英语".contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=speaker(group.getChildAt(i));if(found!=null)return found;}
        return null;
    }
    private boolean hasText(View view,String message) {
        if(view instanceof TextView text&&text.getText().toString().contains(message))return true;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)if(hasText(group.getChildAt(i),message))return true;
        return false;
    }
    private android.view.accessibility.AccessibilityNodeInfo findNode(android.view.accessibility.AccessibilityNodeInfo node,String text) {
        if(node==null)return null;
        if(node.getText()!=null&&node.getText().toString().equals(text))return node;
        if(node.getContentDescription()!=null&&node.getContentDescription().toString().equals(text))return node;
        for(int i=0;i<node.getChildCount();i++){var found=findNode(node.getChild(i),text);if(found!=null)return found;}
        return null;
    }
    private android.view.accessibility.AccessibilityNodeInfo waitNode(String text) throws Exception {
        for(int i=0;i<100;i++) {
            var node=findNode(InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow(),text);
            if(node!=null)return node;Thread.sleep(100);
        }
        throw new AssertionError("UI text not found: "+text);
    }
    private void click(String text) throws Exception { assertTrue(waitNode(text).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)); }
    private android.view.accessibility.AccessibilityNodeInfo inputNode(android.view.accessibility.AccessibilityNodeInfo node) {
        if(node==null)return null;
        if("android.widget.EditText".contentEquals(node.getClassName()))return node;
        for(int i=0;i<node.getChildCount();i++){var found=inputNode(node.getChild(i));if(found!=null)return found;}
        return null;
    }
    private void answer(String text) {
        var node=inputNode(InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow());assertNotNull(node);
        android.os.Bundle args=new android.os.Bundle();args.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);
        assertTrue(node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,args));
    }
    @Test public void offlineWordTapSavesAndReviewsBothWays() throws Exception {
        var instrumentation=InstrumentationRegistry.getInstrumentation();Context context=instrumentation.getTargetContext();
        var entry=OfflineDictionary.lookup(context,"boundary");assertNotNull(entry);assertTrue(entry.meaning().matches("(?s).*[\\p{IsHan}].*"));
        assertNotNull(OfflineDictionary.lookup(context,"comfortable"));assertNotNull(OfflineDictionary.lookup(context,"DON’T"));
        assertNotNull(OfflineDictionary.lookup(context,"everyone's"));assertNull(OfflineDictionary.lookup(context,"qzxqzxnotaword"));
        try(var notebook=new Notebook(context)){notebook.remove("boundary");}
        MainActivity activity=(MainActivity)instrumentation.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            var field=MainActivity.class.getDeclaredField("vocabulary");field.setAccessible(true);var vocabulary=(VocabularyUi)field.get(activity);
            instrumentation.runOnMainSync(()->{
                TextView word=new TextView(activity);vocabulary.enableWords(word,"A boundary matters.");
                android.text.Spanned spans=(android.text.Spanned)word.getText();
                for(var span:spans.getSpans(0,spans.length(),android.text.style.ClickableSpan.class))
                    if(spans.subSequence(spans.getSpanStart(span),spans.getSpanEnd(span)).toString().equals("boundary"))span.onClick(word);
            });
            waitNode("加入生词本");for(int i=0;i<100&&!waitNode("加入生词本").isEnabled();i++)Thread.sleep(100);
            click("🔊 男声美语发音");Thread.sleep(1800);assertFalse(activity.isDestroyed());
            click("加入生词本");waitNode("已加入生词本");click("关闭");
            click("退出全屏");click("生词本");waitNode("boundary");waitNode("词性：名词");
            assertNull(findNode(instrumentation.getUiAutomation().getRootInActiveWindow(),"看中文 · 拼英文"));
            var before=new android.graphics.Rect();waitNode("朗读生词 boundary").getBoundsInScreen(before);
            click("朗读生词 boundary");Thread.sleep(1500);waitNode("boundary");waitNode("词性：名词");
            click("朗读生词 boundary");Thread.sleep(1500);waitNode("boundary");waitNode("关闭");
            var after=new android.graphics.Rect();waitNode("朗读生词 boundary").getBoundsInScreen(after);assertEquals("Speaking must preserve the list position",before,after);
            assertFalse(activity.isDestroyed());
            var bookImage=instrumentation.getUiAutomation().takeScreenshot();
            try(var out=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"v16-notebook.png"))){bookImage.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bookImage.recycle();
            click("关闭");click("背诵复习");click("看中文 · 拼英文");waitNode("检查答案");
            click("🔊 听英文读音");Thread.sleep(1200);waitNode("检查答案");
            answer(" BOUNDARY ");click("检查答案");waitNode("下一词");
            try(var notebook=new Notebook(context)){assertEquals(1,notebook.list().stream().filter(w->w.word().equals("boundary")).findFirst().orElseThrow().streak());}
            click("结束");waitNode("背诵复习");click("听英文 · 拼英文");waitNode("检查答案");
            assertNull("Listening must hide the English answer",findNode(instrumentation.getUiAutomation().getRootInActiveWindow(),"boundary"));
            Thread.sleep(1500);answer("boundry");click("检查答案");waitNode("下一词");
            try(var notebook=new Notebook(context)) {
                var word=notebook.list().stream().filter(w->w.word().equals("boundary")).findFirst().orElseThrow();
                assertEquals(0,word.streak());assertTrue(word.due()>System.currentTimeMillis());assertTrue(word.due()<System.currentTimeMillis()+610000);
            }
            var screenshot=instrumentation.getUiAutomation().takeScreenshot();
            try(var out=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"v16-review.png"))){screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}screenshot.recycle();
            click("结束");waitNode("背诵复习");click("关闭");
        } finally {
            try(var notebook=new Notebook(context)){notebook.remove("boundary");}
            instrumentation.runOnMainSync(activity::finish);instrumentation.waitForIdleSync();
        }
    }
    @Test public void clickingSentenceFinishesPlaybackWithoutExitingApp() throws Exception {
        var instrumentation=InstrumentationRegistry.getInstrumentation();
        Context context=instrumentation.getTargetContext();
        MainActivity activity=(MainActivity)instrumentation.startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            var target=new java.util.concurrent.atomic.AtomicReference<View>();
            for(int i=0;i<100&&target.get()==null;i++){
                instrumentation.runOnMainSync(()->target.set(speaker(activity.getWindow().getDecorView())));Thread.sleep(50);
            }
            assertNotNull("First sentence must be available",target.get());
            instrumentation.runOnMainSync(()->target.get().performClick());
            var completed=new java.util.concurrent.atomic.AtomicBoolean();
            for(int i=0;i<400&&!completed.get();i++){
                instrumentation.runOnMainSync(()->completed.set(hasText(activity.getWindow().getDecorView(),"朗读完成")));Thread.sleep(50);
            }
            assertTrue("Speech must complete, not only initialize",completed.get());assertFalse(activity.isFinishing());
            instrumentation.runOnMainSync(()->target.get().performClick());Thread.sleep(3000);
            assertFalse("Repeated taps must not exit the app",activity.isFinishing());
        } finally {instrumentation.runOnMainSync(activity::finish);}
    }
    @Test public void bundledUsVoiceGeneratesAudibleSamplesWithoutSystemTts() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        var voice=OfflineVoice.create(c);
        try {
            var audio=voice.generate("One thing I really like about Shanghai is that doing things alone feels normal.",0,1f);
            assertEquals(22050,audio.getSampleRate());assertTrue(audio.getSamples().length>22050);
            double energy=0;for(float sample:audio.getSamples())energy+=sample*sample;
            assertTrue("Synthesized audio must not be silent",Math.sqrt(energy/audio.getSamples().length)>0.005);
        } finally {voice.release();}
    }
    @Test public void roomDeduplicatesWithoutLosingFavorites() throws Exception {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        ArticleDb db=Room.inMemoryDatabaseBuilder(c,ArticleDb.class).build();
        try {
            Article a=new Article();a.id="2026-10-13:DAY";a.date="2026-10-13";a.period="DAY";a.title="original";
            assertTrue(db.articles().insert(a)>0);db.articles().favorite(a.id,true);
            a.title="duplicate";assertEquals(-1,db.articles().insert(a));
            assertEquals("original",db.articles().get(a.id).title);assertTrue(db.articles().get(a.id).favorite);
            Article collision=new Article();collision.id="different-id";collision.date=a.date;collision.period=a.period;
            assertEquals(-1,db.articles().insert(collision));
            db.seed(c);db.seed(c);assertNotNull(db.articles().get(a.id));
        } finally { db.close(); }
    }
    @Test public void vaultRoundTripsSyntheticKeyAndStoresNoPlaintext() throws Exception {
        Context c=new android.content.ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()) {
            @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode) {
                return super.getSharedPreferences("test-"+name,mode);
            }
        };
        String synthetic="sk-"+"testonly".repeat(4); // Synthetic fixture, never a real credential.
        KeyVault.clear(c);
        KeyVault.save(c,synthetic);assertEquals(synthetic,KeyVault.read(c));
        assertFalse(c.getSharedPreferences("vault",Context.MODE_PRIVATE).getAll().toString().contains(synthetic));
        KeyVault.clear(c);assertEquals("",KeyVault.read(c));
    }
}
