package com.dailyenglish.app;

import android.app.*;
import android.graphics.Color;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.regex.Pattern;

public final class VocabularyUi {
    private final Activity activity;
    private final SpeechPlayer player;
    public VocabularyUi(Activity activity,SpeechPlayer player) { this.activity=activity;this.player=player; }
    private boolean alive() { return !activity.isFinishing()&&!activity.isDestroyed(); }
    private int dp(int n) { return Math.round(n*activity.getResources().getDisplayMetrics().density); }
    private LinearLayout body() { LinearLayout view=new LinearLayout(activity);view.setOrientation(LinearLayout.VERTICAL);view.setPadding(dp(18),dp(8),dp(18),dp(12));return view; }
    private TextView text(String value) { TextView view=new TextView(activity);view.setText(value);view.setTextSize(17);view.setTextColor(Color.rgb(27,44,43));view.setPadding(0,dp(8),0,dp(8));return view; }
    private Button button(String label,Runnable action) { Button b=new Button(activity);b.setText(label);b.setAllCaps(false);b.setOnClickListener(v->action.run());return b; }
    private void toast(String value) { Toast.makeText(activity,value,Toast.LENGTH_SHORT).show(); }
    public void enableWords(TextView view,String sentence) {
        SpannableString spans=new SpannableString(sentence);
        var matcher=Pattern.compile("[A-Za-z]+(?:['’\u002d][A-Za-z]+)*").matcher(sentence);
        while(matcher.find()) {
            String word=matcher.group();
            spans.setSpan(new ClickableSpan() {
                @Override public void onClick(View clicked) { lookup(word,sentence); }
                @Override public void updateDrawState(TextPaint paint) { paint.setUnderlineText(false); }
            },matcher.start(),matcher.end(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        view.setText(spans);view.setMovementMethod(LinkMovementMethod.getInstance());
    }
    public void lookup(String word,String sentence) {
        player.stop();
        LinearLayout content=body();TextView meaning=text("正在查询离线词典…");content.addView(meaning);
        content.addView(button("🔊 男声美语发音",()->player.word(word)));
        Button add=button("加入生词本",()->{});add.setEnabled(false);content.addView(add);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(word).setView(scroll).setNegativeButton("关闭",null).create();
        dialog.setOnDismissListener(d->player.stop());dialog.show();
        DailyApp.IO.execute(()->{
            try {
                var entry=OfflineDictionary.lookup(activity,word);
                activity.runOnUiThread(()->{
                    if(!alive()||!dialog.isShowing())return;
                    meaning.setText(entry==null?"离线词库暂未收录此词。仍可听发音。":(entry.word().equals(OfflineDictionary.normalize(word))?"":"词典原型："+entry.word()+"\n")+entry.meaning()+"\n\n离线英汉词库（ECDICT＋常用缩略语）");
                    if(entry!=null) {
                        add.setEnabled(true);add.setOnClickListener(v->{
                            add.setEnabled(false);
                            DailyApp.IO.execute(()->{
                                try(var notebook=new Notebook(activity)) {
                                    boolean saved=notebook.add(entry,sentence);
                                    activity.runOnUiThread(()->{if(alive()&&dialog.isShowing())add.setText(saved?"已加入生词本":"已在生词本中");});
                                }
                            });
                        });
                    }
                });
            } catch(Exception e) { activity.runOnUiThread(()->{if(alive()&&dialog.isShowing())meaning.setText("离线词典载入失败，请重试。");}); }
        });
    }
    public void notebook() {
        player.stop();
        DailyApp.IO.execute(()->{
            try(var notebook=new Notebook(activity)) {
                var words=notebook.list();activity.runOnUiThread(()->{if(alive())showNotebook(words);});
            }
        });
    }
    private void showNotebook(List<Notebook.Word> words) {
        LinearLayout content=body();int[] count={words.size()};TextView total=text("全部生词 · "+count[0]+" 词 · A–Z");content.addView(total);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("生词本").setView(scroll).setNegativeButton("关闭",null).create();
        if(words.isEmpty())content.addView(text("在英文篇章里点一个单词，再点“加入生词本”。"));
        for(var word:words.stream().sorted(Comparator.comparing(Notebook.Word::word)).toList()) {
            LinearLayout entry=body();entry.setTag("notebook-entry-"+word.word());
            LinearLayout heading=new LinearLayout(activity);heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView label=text(word.word());label.setTextSize(21);label.setTypeface(null,android.graphics.Typeface.BOLD);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));
            Button speaker=button("🔊",()->player.word(word.word()));speaker.setTag("word-speaker-"+word.word());speaker.setContentDescription("朗读生词 "+word.word());heading.addView(speaker,new LinearLayout.LayoutParams(dp(52),dp(48)));
            Button remove=button("移除",()->{});remove.setContentDescription("移出生词 "+word.word());heading.addView(remove,new LinearLayout.LayoutParams(dp(72),dp(48)));
            remove.setOnClickListener(v->{
                remove.setEnabled(false);
                DailyApp.IO.execute(()->{
                    try(var notebook=new Notebook(activity)) {
                        notebook.remove(word.word());
                        activity.runOnUiThread(()->{
                            if(!alive()||!dialog.isShowing())return;
                            player.stop();content.removeView(entry);total.setText("全部生词 · "+(--count[0])+" 词 · A–Z");
                            if(count[0]==0)content.addView(text("生词本为空，可在英文篇章里加入单词。"));
                        });
                    }
                });
            });
            entry.addView(heading);TextView pos=text("词性："+Notebook.partsOfSpeech(word.meaning()));pos.setTextSize(14);entry.addView(pos);entry.addView(text(Notebook.translation(word.meaning())));content.addView(entry);
        }
        dialog.setOnDismissListener(d->player.stop());dialog.show();
    }
    public void saveSentence(String english,String title,Button button) {
        button.setEnabled(false);
        DailyApp.IO.execute(()->{
            try(var book=new SentenceBook(activity)) {
                boolean saved=book.add(english,title);
                activity.runOnUiThread(()->{if(alive()){button.setEnabled(true);toast(saved?"已加入精句本":"这句已在精句本中");}});
            }
        });
    }
    public void sentences() {
        player.stop();
        DailyApp.IO.execute(()->{
            try(var book=new SentenceBook(activity)) {
                var sentences=book.list();activity.runOnUiThread(()->{if(alive())showSentences(sentences);});
            }
        });
    }
    private void showSentences(List<SentenceBook.Sentence> sentences) {
        LinearLayout content=body();int[] count={sentences.size()};TextView total=text("全部精句 · "+count[0]+" 句");content.addView(total);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("精句本").setView(scroll).setNegativeButton("关闭",null).create();
        if(sentences.isEmpty())content.addView(text("在英文篇章每句旁点 ＋，加入精句本。"));
        for(var sentence:sentences) {
            LinearLayout entry=body();TextView english=text(sentence.english());english.setTextIsSelectable(true);entry.addView(english);
            TextView source=text("来自："+sentence.title());source.setTextSize(13);entry.addView(source);
            LinearLayout actions=new LinearLayout(activity);
            Button speaker=button("🔊 朗读",()->player.word(sentence.english()));speaker.setContentDescription("朗读精句 "+sentence.english());actions.addView(speaker,new LinearLayout.LayoutParams(0,dp(48),1));
            Button remove=button("移除",()->{});remove.setContentDescription("移除精句 "+sentence.english());actions.addView(remove,new LinearLayout.LayoutParams(0,dp(48),1));entry.addView(actions);content.addView(entry);
            remove.setOnClickListener(v->{
                remove.setEnabled(false);
                DailyApp.IO.execute(()->{
                    try(var book=new SentenceBook(activity)) {
                        book.remove(sentence.english());
                        activity.runOnUiThread(()->{
                            if(!alive()||!dialog.isShowing())return;
                            player.stop();content.removeView(entry);total.setText("全部精句 · "+(--count[0])+" 句");
                            if(count[0]==0)content.addView(text("精句本为空，可在英文篇章每句旁点 ＋ 添加。"));
                        });
                    }
                });
            });
        }
        dialog.setOnDismissListener(d->player.stop());dialog.show();
    }
    public void practice() {
        player.stop();
        DailyApp.IO.execute(()->{
            try(var notebook=new Notebook(activity)) {
                var words=notebook.list();activity.runOnUiThread(()->{if(alive())showPractice(words);});
            }
        });
    }
    private void showPractice(List<Notebook.Word> words) {
        LinearLayout content=body();long now=System.currentTimeMillis();var due=words.stream().filter(w->w.due()<=now).toList();
        content.addView(text("共 "+words.size()+" 词 · 待复习 "+due.size()+" 词"));
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("背诵复习").setView(scroll).setNegativeButton("关闭",null).create();
        content.addView(button("看中文 · 拼英文",()->{dialog.dismiss();review(words,0,false);}));
        content.addView(button("听英文 · 拼英文",()->{dialog.dismiss();review(words,0,true);}));
        content.addView(button("复习到期单词",()->{
            new AlertDialog.Builder(activity).setTitle("选择复习方式").setItems(new String[]{"看中文 · 拼英文","听英文 · 拼英文"},(d,choice)->{dialog.dismiss();review(due,0,choice==1);}).show();
        }));
        if(words.isEmpty())content.addView(text("暂无生词。请先在英文篇章里加入生词本。"));
        dialog.setOnDismissListener(d->player.stop());dialog.show();
    }
    private void review(List<Notebook.Word> words,int index,boolean listening) {
        if(words.isEmpty()){toast("暂无待练习单词");return;}
        if(index>=words.size()){toast("本轮练习完成，复习进度已保存");practice();return;}
        var word=words.get(index);LinearLayout content=body();
        content.addView(text(listening?"听读音，输入英文单词。":word.meaning()));
        content.addView(button("🔊 听英文读音",()->player.word(word.word())));
        EditText answer=new EditText(activity);answer.setHint("输入英文");answer.setTag("review-answer");answer.setSingleLine(true);
        answer.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        answer.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);content.addView(answer);
        TextView result=text("");result.setTag("review-result");content.addView(result);
        ScrollView scroll=new ScrollView(activity);scroll.addView(content);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle((listening?"听音拼写":"中文拼写")+" · "+(index+1)+" / "+words.size()).setView(scroll).setNegativeButton("结束",(d,w)->practice()).create();
        Button submit=button("检查答案",()->{});content.addView(submit);
        submit.setOnClickListener(v->{
            if(answer.getText().toString().trim().isEmpty()){answer.setError("请先输入英文");return;}
            activity.getSystemService(android.view.inputmethod.InputMethodManager.class).hideSoftInputFromWindow(answer.getWindowToken(),0);
            boolean correct=Notebook.matches(answer.getText().toString(),word.word());submit.setEnabled(false);answer.setEnabled(false);
            DailyApp.IO.execute(()->{
                try(var notebook=new Notebook(activity)) {
                    notebook.grade(word,correct,System.currentTimeMillis());
                    activity.runOnUiThread(()->{
                        if(!alive()||!dialog.isShowing())return;
                        result.setText((correct?"✓ 正确":"再记一次，正确答案："+word.word())+"\n"+word.meaning()+"\n"+(correct?"已安排下次复习":"10分钟后再复习"));
                        submit.setText("下一词");submit.setEnabled(true);submit.setOnClickListener(next->{dialog.dismiss();review(words,index+1,listening);});
                    });
                }
            });
        });
        dialog.setOnDismissListener(d->player.stop());dialog.show();if(listening)player.word(word.word());
    }
}
