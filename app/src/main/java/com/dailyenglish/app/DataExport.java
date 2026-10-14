package com.dailyenglish.app;

import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.zip.*;

public final class DataExport {
    public static byte[] capture(Context context,String format) throws Exception {
        List<Article> articles=ArticleDb.get(context).articles().all();
        try(var words=new Notebook(context);var sentences=new SentenceBook(context)) {
            return encode(articles,words.list(),sentences.list(),ZonedDateTime.now(SlotClock.ZONE).toString(),format);
        }
    }
    public static byte[] encode(List<Article> articles,List<Notebook.Word> words,List<SentenceBook.Sentence> sentences,String exportedAt,String format) throws Exception {
        JSONObject data=new JSONObject().put("schemaVersion",1).put("appVersion","1.8").put("exportedAt",exportedAt).put("timeZone","Asia/Shanghai");
        JSONArray articleData=new JSONArray(),wordData=new JSONArray(),sentenceData=new JSONArray();
        StringBuilder md=new StringBuilder("# Daily English 最新数据包\n\n导出时间："+exportedAt+"\n\n篇章 "+articles.size()+" 篇 · 生词 "+words.size()+" 词 · 精句 "+sentences.size()+" 句\n\n");
        md.append("## 篇章\n\n");
        for(Article a:articles) {
            articleData.put(new JSONObject().put("id",a.id).put("date",a.date).put("period",a.period).put("title",a.title)
                    .put("english",a.english).put("chinese",a.chinese).put("expressions",a.expressions)
                    .put("source",a.source).put("favorite",a.favorite).put("createdAt",a.createdAt));
            md.append("### ").append(inline(a.title)).append("\n\n")
                    .append(a.period.equals("DAY")?"白天版":"晚上版").append(" · ").append(a.id.startsWith("archive-")?"原文日期未提供":a.date)
                    .append(a.favorite?" · 已收藏":"").append("\n\nID：").append(inline(a.id)).append("\n\n来源：").append(inline(a.source))
                    .append("\n\n创建时间戳：").append(a.createdAt).append("\n\n#### 英文\n\n").append(a.english)
                    .append("\n\n#### 中文\n\n").append(a.chinese).append("\n\n#### 表达\n\n");
            try {
                JSONArray expressions=new JSONArray(a.expressions);
                for(int i=0;i<expressions.length();i++) {
                    JSONObject expression=expressions.getJSONObject(i);
                    md.append("- ").append(inline(expression.optString("en"))).append("：").append(inline(expression.optString("zh"))).append("\n");
                }
                if(expressions.length()==0)md.append("未提供表达列表。\n");
            } catch(JSONException e){md.append(a.expressions).append("\n");}
            md.append("\n---\n\n");
        }
        md.append("## 生词本\n\n");
        for(var word:words.stream().sorted(Comparator.comparing(Notebook.Word::word)).toList()) {
            wordData.put(new JSONObject().put("word",word.word()).put("meaning",word.meaning()).put("partsOfSpeech",Notebook.partsOfSpeech(word.meaning()))
                    .put("sentence",word.sentence()).put("streak",word.streak()).put("due",word.due()));
            md.append("### ").append(inline(word.word())).append("\n\n词性：").append(Notebook.partsOfSpeech(word.meaning()))
                    .append("\n\n").append(word.meaning()).append("\n\n原句：").append(word.sentence()).append("\n\n连续正确次数：").append(word.streak())
                    .append("\n\n下次复习：").append(word.due()==0?"待首次复习":Instant.ofEpochMilli(word.due()).atZone(SlotClock.ZONE).toString()).append("\n\n");
        }
        if(words.isEmpty())md.append("暂无生词。\n\n");
        md.append("## 精句本\n\n");
        for(var sentence:sentences) {
            sentenceData.put(new JSONObject().put("english",sentence.english()).put("title",sentence.title()));
            md.append(sentence.english()).append("\n\n来源篇章：").append(inline(sentence.title())).append("\n\n---\n\n");
        }
        if(sentences.isEmpty())md.append("暂无精句。\n\n");
        data.put("articles",articleData).put("vocabulary",wordData).put("sentences",sentenceData);
        byte[] markdown=md.toString().getBytes(StandardCharsets.UTF_8);
        if(format.equals("md"))return markdown;
        if(!format.equals("zip"))throw new IllegalArgumentException("不支持的格式");
        Map<String,byte[]> files=new LinkedHashMap<>();
        files.put("最新数据包.md",markdown);files.put("data.json",data.toString(2).getBytes(StandardCharsets.UTF_8));
        files.put("说明.md",("# 数据包说明\n\n最新数据包.md 可直接阅读；data.json 保留完整字段和复习进度，schemaVersion=1。\n\n"
                +"数据来自导出时本机已保存内容，不含已移除篇章，不含 API 密钥、设备设置、语音模型或词典。\n\n"
                +"历史原文未提供的日期不会编造。此版本支持导出，尚未提供数据包导入。\n\n"
                +"分享前可检查生词原句、精句及篇章中是否包含个人信息。\n").getBytes(StandardCharsets.UTF_8));
        JSONObject manifest=new JSONObject().put("schemaVersion",1).put("exportedAt",exportedAt)
                .put("articleCount",articles.size()).put("wordCount",words.size()).put("sentenceCount",sentences.size());
        JSONArray hashes=new JSONArray();
        for(var file:files.entrySet())hashes.put(new JSONObject().put("name",file.getKey()).put("bytes",file.getValue().length)
                .put("sha256",HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(file.getValue()))));
        files.put("manifest.json",manifest.put("files",hashes).toString(2).getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(output,StandardCharsets.UTF_8)) {
            for(var file:files.entrySet()){zip.putNextEntry(new ZipEntry(file.getKey()));zip.write(file.getValue());zip.closeEntry();}
        }
        return output.toByteArray();
    }
    private static String inline(String value){return value.replace("\r","").replace("\n"," ").replace("\\","\\\\").replace("*","\\*").replace("_","\\_").replace("[","\\[").replace("]","\\]").replace("#","\\#");}
}
