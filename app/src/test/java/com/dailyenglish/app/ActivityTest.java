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
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = DailyApp.class, qualifiers = "w393dp-h820dp-xhdpi")
@LooperMode(LooperMode.Mode.PAUSED)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ActivityTest {
    private Button find(View view,String text) {
        if(view instanceof Button b && b.getText().toString().equals(text)) return b;
        if(view instanceof ViewGroup g) for(int i=0;i<g.getChildCount();i++){Button found=find(g.getChildAt(i),text);if(found!=null)return found;}
        return null;
    }
    @Test public void homeSwitchesTabsAndOpensSafeSettings() {
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try {
            MainActivity activity=controller.get();View root=activity.getWindow().getDecorView();
            find(root,"退出全屏").performClick();
            assertNotNull(find(root,"☀ 白天 · 08:00"));
            find(root,"☾ 晚上 · 23:00").performClick();
            find(root,"中文").performClick();find(root,"表达").performClick();find(root,"英文").performClick();
            assertNotNull(find(root,"▶ 全文"));
            find(root,"设置").performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            assertTrue(dialog.isShowing());
            assertNotEquals(0,dialog.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE);
            dialog.dismiss();
        } finally {controller.pause().stop().destroy();}
    }
    @Test public void readingUsesMostOfTheScreenForEnglishAndChinese() {
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try {
            MainActivity activity=controller.get();
            ViewGroup host=activity.findViewById(android.R.id.content);
            View root=host.getChildAt(0);
            int width=Math.round(393*activity.getResources().getDisplayMetrics().density);
            int height=Math.round(820*activity.getResources().getDisplayMetrics().density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
            assertNotNull(find(root,"退出全屏"));
            View body=root.findViewWithTag("reading-body");
            assertTrue("English body must occupy at least 75% of screen",body.getHeight()>=height*0.75);
            assertNotNull(find(root,"▶ 全文"));
            find(root,"中文").performClick();
            root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
            assertTrue("Chinese body must occupy at least 88% of screen",body.getHeight()>=height*0.88);
            find(root,"退出全屏").performClick();assertNotNull(find(root,"全屏阅读"));
            assertEquals(View.VISIBLE,find(root,"设置").getParent() instanceof View ? ((View)find(root,"设置").getParent()).getVisibility() : View.GONE);
            find(root,"全屏阅读").performClick();
            activity.getOnBackPressedDispatcher().onBackPressed();
            assertNotNull(find(root,"全屏阅读"));assertFalse(activity.isFinishing());
            find(root,"全屏阅读").performClick();
            controller.recreate();
            assertNotNull(find(controller.get().getWindow().getDecorView(),"退出全屏"));
        } finally {controller.pause().stop().destroy();}
    }
    private View findSpeaker(View view) {
        if("朗读第1句，美式英语".contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))return view;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=findSpeaker(group.getChildAt(i));if(found!=null)return found;}
        return null;
    }
    private Spinner filter(View view) {
        if(view instanceof Spinner spinner)return spinner;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){Spinner found=filter(group.getChildAt(i));if(found!=null)return found;}
        return null;
    }
    private Button editionEntry(View view) {
        if(view instanceof Button b&&b.getText().toString().contains("版")&&b.getText().toString().contains("\n"))return b;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){Button found=editionEntry(group.getChildAt(i));if(found!=null)return found;}
        return null;
    }
    private java.util.Set<String> entries(View view) {
        var result=new java.util.HashSet<String>();
        if(view instanceof Button b&&b.getText().toString().contains("\n"))result.add(b.getText().toString().split("\n")[0]);
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)result.addAll(entries(group.getChildAt(i)));
        return result;
    }
    @Test public void dayAndNightContainDifferentContentWithoutHistoricalLabels() throws Exception {
        var controller=Robolectric.buildActivity(MainActivity.class).setup();
        try {
            MainActivity activity=controller.get();View root=activity.getWindow().getDecorView();
            DailyApp.IO.submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);
            for(int i=0;i<100&&findSpeaker(root)==null;i++){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}
            assertNotNull("Imported archive must load",findSpeaker(root));
            find(root,"退出全屏").performClick();find(root,"篇章目录 / 搜索").performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
            AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();View listRoot=dialog.getWindow().getDecorView();
            Spinner periods=filter(listRoot);
            assertEquals(3,periods.getAdapter().getCount());
            periods.setSelection(1);Shadows.shadowOf(Looper.getMainLooper()).idle();var day=entries(listRoot);assertTrue(day.contains("清晨的上海"));assertFalse(day.contains("关系中的边界感"));
            periods.setSelection(2);Shadows.shadowOf(Looper.getMainLooper()).idle();var night=entries(listRoot);assertTrue(night.contains("关系中的边界感"));assertFalse(night.contains("清晨的上海"));
            assertTrue(java.util.Collections.disjoint(day,night));
            editionEntry(listRoot).performClick();assertFalse(dialog.isShowing());assertNotNull(find(root,"退出全屏"));
            find(root,"中文").performClick();
            View page=((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);
            int width=786,height=1640;
            page.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));page.layout(0,0,width,height);
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(width,height,android.graphics.Bitmap.Config.ARGB_8888);
            page.draw(new android.graphics.Canvas(bitmap));
            java.io.File image=new java.io.File("build/reports/chinese-reading-preview.png");image.getParentFile().mkdirs();
            try(var output=new java.io.FileOutputStream(image)){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,output);}bitmap.recycle();
        } finally {controller.pause().stop().destroy();}
    }
}
