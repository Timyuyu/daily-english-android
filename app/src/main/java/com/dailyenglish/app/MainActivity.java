package com.dailyenglish.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import org.json.*;
import java.time.*;
import java.util.*;

public class MainActivity extends ComponentActivity {
    private static final int INK=Color.rgb(27,44,43), MUTED=Color.rgb(96,115,110), GREEN=Color.rgb(35,108,97), BG=Color.rgb(244,246,245);
    private LinearLayout root, menuPanel, content, titleArea, controls, playbackControls;
    private TextView sync, speech;
    private Button day, night, readingToggle;
    private SpeechPlayer player;
    private VocabularyUi vocabulary;
    private List<Article> articles=List.of();
    private Article selected;
    private String period=SlotClock.current(Instant.now()).period(), selectedId;
    private int tab;
    private boolean pinned, manualPeriod;
    private boolean readingMode=true;
    private final OnBackPressedCallback readingBack=new OnBackPressedCallback(true) {
        @Override public void handleOnBackPressed() { setReadingMode(false); }
    };
    private ConnectivityManager connectivity;
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener prefListener=(p,k)-> runOnUiThread(this::updateStatus);
    private final ConnectivityManager.NetworkCallback networkCallback=new ConnectivityManager.NetworkCallback() {
        @Override public void onAvailable(Network network) { Scheduler.catchUp(MainActivity.this); }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state!=null) { period=state.getString("period",period); selectedId=state.getString("article"); pinned=state.getBoolean("pinned"); manualPeriod=state.getBoolean("manual"); tab=state.getInt("tab"); readingMode=state.getBoolean("reading",true); }
        String notificationId=getIntent().getStringExtra("article_id");
        if (notificationId!=null) { selectedId=notificationId; pinned=true; }
        build();
        getOnBackPressedDispatcher().addCallback(this,readingBack);
        player=new SpeechPlayer(this,(message,sentence)-> {
            speech.setText(message);
            for (int i=0;i<content.getChildCount();i++) content.getChildAt(i).setBackgroundColor(i==sentence && tab==0 ? Color.rgb(225,240,234) : Color.WHITE);
        });
        vocabulary=new VocabularyUi(this,player);
        player.setRate(getSharedPreferences("settings",MODE_PRIVATE).getFloat("rate",1f));
        ArticleDb.get(this).articles().observeAll().observe(this,data->{ articles=data; choose(); updateStatus(); });
        getSharedPreferences("settings",MODE_PRIVATE).registerOnSharedPreferenceChangeListener(prefListener);
        connectivity=getSystemService(ConnectivityManager.class);
        connectivity.registerDefaultNetworkCallback(networkCallback);
        updateStatus();
    }
    @Override protected void onResume() {
        super.onResume();
        if (!manualPeriod && !pinned) period=SlotClock.current(Instant.now()).period();
        Scheduler.catchUp(this); choose(); updateStatus();
    }
    @Override protected void onPause() { super.onPause(); if (player!=null) player.pause(); }
    @Override protected void onDestroy() {
        if (player!=null) player.close();
        if (connectivity!=null) connectivity.unregisterNetworkCallback(networkCallback);
        getSharedPreferences("settings",MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(prefListener);
        super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("period",period);state.putString("article",selectedId);state.putBoolean("pinned",pinned);state.putBoolean("manual",manualPeriod);state.putInt("tab",tab);
        state.putBoolean("reading",readingMode);
    }
    private void build() {
        root=column(); root.setBackgroundColor(BG); root.setPadding(dp(20),dp(8),dp(20),0);
        root.setOnApplyWindowInsetsListener((v,insets)-> {
            var bars=insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            root.setPadding(dp(20)+bars.left,dp(8)+bars.top,dp(20)+bars.right,bars.bottom); return insets;
        });
        setContentView(root);
        menuPanel=column();root.addView(menuPanel);
        LinearLayout header=row();
        TextView brand=text("Daily English",24); brand.setTypeface(null,Typeface.BOLD); header.addView(brand,new LinearLayout.LayoutParams(0,dp(52),1));
        header.addView(button("设置",this::settings)); menuPanel.addView(header);
        menuPanel.addView(text("每天两篇，慢慢说得更自然。",14));
        LinearLayout periods=row();
        day=button("☀ 白天 · 08:00",()->switchPeriod("DAY")); night=button("☾ 晚上 · 23:00",()->switchPeriod("NIGHT"));
        weighted(periods,day);weighted(periods,night);menuPanel.addView(periods);
        LinearLayout library=row(); weighted(library,button("篇章目录 / 搜索",()->directory(false))); weighted(library,button("★ 收藏",()->directory(true))); menuPanel.addView(library);
        LinearLayout vocabularySections=row();weighted(vocabularySections,button("生词本",()->vocabulary.notebook()));weighted(vocabularySections,button("精句本",()->vocabulary.sentences()));weighted(vocabularySections,button("背诵复习",()->vocabulary.practice()));menuPanel.addView(vocabularySections);
        sync=text("",12);sync.setPadding(0,dp(6),0,dp(8));menuPanel.addView(sync);
        titleArea=column();menuPanel.addView(titleArea);
        LinearLayout tabs=row();
        for (int i=0;i<3;i++) { int n=i; weighted(tabs,button(new String[]{"英文","中文","表达"}[i],()-> { tab=n;renderBody(); })); }
        readingToggle=button("退出全屏",()->setReadingMode(!readingMode));weighted(tabs,readingToggle);
        root.addView(tabs);
        controls=column(); LinearLayout playback=row();playbackControls=playback;
        weighted(playback,button("▶ 全文",()-> { if (selected!=null) player.full(); }));
        weighted(playback,button("Ⅱ 暂停",()->player.pause()));
        weighted(playback,button("▷ 继续",()->player.resume())); root.addView(playback);
        LinearLayout options=row(); TextView label=text("语速",13); options.addView(label);
        Spinner speed=new Spinner(this); String[] speeds={"0.75×","1.0×","1.25×","1.5×"}; float[] rates={0.75f,1f,1.25f,1.5f};
        speed.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,speeds));
        float savedRate=getSharedPreferences("settings",MODE_PRIVATE).getFloat("rate",1f);
        int savedIndex=1;for(int i=0;i<rates.length;i++) if(rates[i]==savedRate) savedIndex=i; speed.setSelection(savedIndex);
        speed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> p,View v,int position,long id){ if(player!=null)player.setRate(rates[position]);getSharedPreferences("settings",MODE_PRIVATE).edit().putFloat("rate",rates[position]).apply(); }
            @Override public void onNothingSelected(AdapterView<?> p){}
        }); options.addView(speed,new LinearLayout.LayoutParams(0,dp(48),1));
        CheckBox loop=new CheckBox(this);loop.setText("循环全文");loop.setTextSize(13);loop.setOnCheckedChangeListener((b,checked)->player.repeat=checked);options.addView(loop);controls.addView(options);
        root.addView(controls);speech=text("正在准备内置美式离线语音…",12);speech.setMaxLines(2);speech.setEllipsize(TextUtils.TruncateAt.END);root.addView(speech);
        ScrollView scroll=new ScrollView(this);scroll.setTag("reading-body");scroll.setFillViewport(true);content=column();content.setTag("article-content");content.setPadding(0,dp(12),0,dp(24));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setReadingMode(readingMode);
    }
    private void setReadingMode(boolean enabled) {
        readingMode=enabled;menuPanel.setVisibility(enabled?View.GONE:View.VISIBLE);
        readingToggle.setText(enabled?"退出全屏":"全屏阅读");readingBack.setEnabled(enabled);
        updateReadingControls();
    }
    private void updateReadingControls() {
        controls.setVisibility(tab==0&&!readingMode?View.VISIBLE:View.GONE);
        playbackControls.setVisibility(tab==0?View.VISIBLE:View.GONE);
        speech.setVisibility(tab==0?View.VISIBLE:View.GONE);
    }
    private void switchPeriod(String p) { period=p;manualPeriod=true;pinned=false;selectedId=null;choose(); }
    private void choose() {
        Article chosen=null;
        if(pinned) chosen=articles.stream().filter(a->a.id.equals(selectedId)).findFirst().orElse(null);
        if(chosen==null) chosen=articles.stream().filter(a->a.period.equals(period)).findFirst().orElse(null);
        if(chosen!=null&&pinned)period=chosen.period;
        boolean changed=selected==null || chosen==null || !selected.id.equals(chosen.id);
        selected=chosen; if(selected!=null)selectedId=selected.id;
        if(changed && player!=null) player.load(selected==null?List.of():SlotClock.sentences(selected.english));
        day.setTextColor(period.equals("DAY")?GREEN:MUTED);night.setTextColor(period.equals("NIGHT")?GREEN:MUTED);
        titleArea.removeAllViews();
        if(selected!=null){
            LinearLayout row=row();TextView heading=text(selected.title,22);heading.setTypeface(null,Typeface.BOLD);row.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(button(selected.favorite?"★":"☆",()->{ Article a=selected;DailyApp.IO.execute(()->ArticleDb.get(this).articles().favorite(a.id,!a.favorite)); }));titleArea.addView(row);
            String edition=selected.period.equals("DAY")?"白天版":"晚上版";
            titleArea.addView(text(selected.id.startsWith("archive-")?edition:selected.date+" · "+edition+" · 约45–60秒",12));
        }
        renderBody();
    }
    private void renderBody() {
        content.removeAllViews();updateReadingControls();
        if(selected==null){ content.addView(text("篇章正在载入，请稍候。",18));return; }
        if(tab==0){
            List<String> sentences=SlotClock.sentences(selected.english);
            for(int i=0;i<sentences.size();i++){
                int index=i;LinearLayout line=row();line.setGravity(Gravity.TOP);line.setBackgroundColor(Color.WHITE);line.setPadding(dp(12),dp(14),dp(4),dp(14));
                TextView sentence=text(sentences.get(i),18);sentence.setLineSpacing(dp(4),1f);vocabulary.enableWords(sentence,sentences.get(i));line.addView(sentence,new LinearLayout.LayoutParams(0,-2,1));
                LinearLayout actions=column();
                Button speaker=button("🔊",()->player.one(index));speaker.setContentDescription("朗读第"+(i+1)+"句，美式英语");actions.addView(speaker,new LinearLayout.LayoutParams(dp(48),dp(48)));
                Button save=button("＋",()->{});save.setContentDescription("加入第"+(i+1)+"句到精句本");save.setTooltipText("加入精句本");save.setOnClickListener(v->vocabulary.saveSentence(sentences.get(index),selected.title,save));actions.addView(save,new LinearLayout.LayoutParams(dp(48),dp(48)));
                line.addView(actions,new LinearLayout.LayoutParams(dp(48),-2));content.addView(line);
            }
        } else if(tab==1){ TextView translation=text(selected.chinese,18);translation.setLineSpacing(dp(8),1f);translation.setTextIsSelectable(true);content.addView(translation); }
        else {
            try { JSONArray data=new JSONArray(selected.expressions);for(int i=0;i<data.length();i++){
                JSONObject e=data.getJSONObject(i);LinearLayout box=column();box.setPadding(dp(16),dp(16),dp(16),dp(16));box.setBackgroundColor(Color.WHITE);
                TextView en=text(e.getString("en"),19);en.setTypeface(null,Typeface.BOLD);vocabulary.enableWords(en,e.getString("en"));box.addView(en);box.addView(text(e.getString("zh"),16));content.addView(box);
            }} catch(JSONException e){content.addView(text("这篇原文未提供表达列表。",16));}
        }
    }
    private void updateStatus() {
        if(sync==null)return;
        var prefs=getSharedPreferences("settings",MODE_PRIVATE);
        String error=prefs.getString("last_error","");
        boolean hasCurrent=articles.stream().anyMatch(a->a.id.equals(SlotClock.current(Instant.now()).key()));
        sync.setText(!error.isEmpty()?error:!KeyVault.configured(this)?"篇章已缓存 · 设置密钥后开启每日更新":!hasCurrent?"当前篇章待生成 · 联网后自动补齐":"篇章已保存，可离线阅读 · 北京时间");
    }
    private void directory(boolean favoritesOnly) {
        LinearLayout body=column();body.setPadding(dp(16),dp(8),dp(16),dp(8));
        EditText search=new EditText(this);search.setHint("搜索标题 / 英文 / 中文");search.setSingleLine(true);body.addView(search);
        Spinner filter=new Spinner(this);String[] labels={"全部篇章","白天版","晚上版"};filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));body.addView(filter);
        TextView hint=text("",12);body.addView(hint);
        ScrollView scroll=new ScrollView(this);LinearLayout list=column();scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(-1,dp(380)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(favoritesOnly?"我的收藏":"篇章目录").setView(body).setNegativeButton("关闭",null).create();
        Runnable populate=()->{
            list.removeAllViews();String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);int choice=filter.getSelectedItemPosition();int count=0;
            String selectedPeriod=new String[]{"","DAY","NIGHT"}[choice];
            boolean hasDedicated=articles.stream().anyMatch(a->a.period.equals(selectedPeriod));
            hint.setText(choice>0&&!hasDedicated?labels[choice]+"暂无篇章，联网后自动补齐。":"");
            hint.setVisibility(hint.getText().length()==0?View.GONE:View.VISIBLE);
            for(Article a:articles){
                if(favoritesOnly&&!a.favorite)continue;
                if(choice>0&&!a.period.equals(selectedPeriod))continue;
                if(!(a.title+" "+a.english+" "+a.chinese).toLowerCase(Locale.ROOT).contains(q))continue;
                String edition=a.period.equals("DAY")?"白天版":"晚上版";
                Button entry=button(a.title+"\n"+(a.id.startsWith("archive-")?edition:a.date+" · "+edition)+(a.favorite?" ★":""),()->{pinned=true;selectedId=a.id;period=a.period;choose();setReadingMode(true);dialog.dismiss();});
                entry.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);entry.setSingleLine(false);list.addView(entry);count++;
            }
            if(count==0)list.addView(text(favoritesOnly?"还没有收藏，点击篇章旁的 ☆ 添加。":"没有匹配的篇章。",16));
        };
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){populate.run();}public void afterTextChanged(Editable e){}});
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){populate.run();}public void onNothingSelected(AdapterView<?> p){}});
        populate.run();dialog.show();
    }
    private void settings() {
        LinearLayout body=column();body.setPadding(dp(20),dp(8),dp(20),dp(8));
        body.addView(text("每天08:00与23:00更新（北京时间）。联网后或打开应用时补齐未生成内容。",15));
        body.addView(text("已在聊天中公开的密钥请先撤销／轮换。新密钥仅在此设备加密保存，直接用于 DeepSeek 请求。",14));
        EditText key=new EditText(this);key.setHint(KeyVault.configured(this)?"已配置 · 留空保留，输入可更换":"输入新的 DeepSeek API 密钥");
        key.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setTransformationMethod(PasswordTransformationMethod.getInstance());key.setSingleLine(true);key.setSaveEnabled(false);key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);body.addView(key);
        body.addView(text("模型：DeepSeek Flash · 已到时的每篇内容会产生少量 API 用量。",12));
        body.addView(button("现在检查 / 补生成",()->{Scheduler.catchUp(this);toast("已安排联网补生成");}));
        body.addView(button("开启新篇章通知",()->{if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);else open(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));}));
        body.addView(button("内置离线语音说明",()->new AlertDialog.Builder(this).setTitle("美式英语离线朗读").setMessage("已内置美国英语男声 Ryan，无需联网、密钥或另外下载语音包。首次打开会准备本机语音数据，请稍候。\n\n点每句小喇叭或全文播放即可朗读已保存内容。如没声音，请调高手机媒体音量，并检查是否连着蓝牙耳机。").setPositiveButton("知道了",null).show()));
        body.addView(button("OriginOS 后台运行说明",this::backgroundHelp));
        body.addView(button("删除已保存密钥",()->new AlertDialog.Builder(this).setMessage("删除后停止新的生成请求，已保存篇章保留。正在进行的请求可能已经发出。").setPositiveButton("删除",(d,w)->{KeyVault.clear(this);toast("密钥已删除");updateStatus();}).setNegativeButton("取消",null).show()));
        ScrollView scroll=new ScrollView(this);scroll.addView(body);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("设置 · V1.7").setView(scroll).setPositiveButton("保存",null).setNegativeButton("关闭",null).create();
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        dialog.setOnShowListener(d->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String value=key.getText().toString().trim();
            if(value.isEmpty()){dialog.dismiss();return;}
            DailyApp.IO.execute(()->{
                try {KeyVault.save(this,value);runOnUiThread(()->{key.getText().clear();dialog.dismiss();Scheduler.catchUp(this);updateStatus();toast("已加密保存，开始补生成");});}
                catch(Exception e){runOnUiThread(()->key.setError(e instanceof IllegalArgumentException?"密钥格式不正确":"保存失败，请重新输入"));}
            });
        });});dialog.show();
    }
    private void backgroundHelp() {
        new AlertDialog.Builder(this).setTitle("OriginOS 后台运行")
                .setMessage("请在系统设置中允许 Daily English 自启动、后台运行和通知，并在最近任务中锁定应用；在电池设置中允许较高后台耗电（名称可能随系统版本变化）。\n\n08:00／23:00安排后台尝试，系统省电、强制停止、重启后未解锁等情况可能延迟。联网或打开应用后会补生成。\n\n首次配置密钥后开始记录每日缺失篇章；不会自动生成安装前的历史内容。")
                .setPositiveButton("应用设置",(d,w)->open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))))
                .setNeutralButton("电池设置",(d,w)->open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))).setNegativeButton("知道了",null).show();
    }
    private void open(Intent intent){try{startActivity(intent);}catch(ActivityNotFoundException e){toast("请从系统设置中打开对应选项");}}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private TextView text(String value,int size){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(size<=14?MUTED:INK);t.setPadding(0,dp(3),0,dp(3));return t;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(GREEN);b.setTextSize(13);b.setMinHeight(dp(48));b.setMinimumWidth(0);b.setPadding(dp(8),0,dp(8),0);b.setOnClickListener(v->action.run());return b;}
    private void weighted(LinearLayout row,View view){row.addView(view,new LinearLayout.LayoutParams(0,-2,1));}
}
