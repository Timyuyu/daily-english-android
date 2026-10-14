package com.dailyenglish.app;

import org.junit.Test;
import org.json.*;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.*;
import static org.junit.Assert.*;

public class DataExportTest {
    @Test public void zipRetainsCompleteOriginalsBooksProgressAndValidHashes() throws Exception {
        Article a=new Article();a.id="2026-10-14:DAY";a.date="2026-10-14";a.period="DAY";a.title="A [title] 中文";
        a.english="I'm obsessed with learning.\nA second complete paragraph.";a.chinese="我热爱学习。\n完整第二段。";
        a.expressions="[{\"en\":\"obsessed with\",\"zh\":\"对……着迷\"}]";a.source="original-source";a.favorite=true;a.createdAt=12345;
        var word=new Notebook.Word("obsessed","a. 着迷的","I'm obsessed.",3,1234567);
        var sentence=new SentenceBook.Sentence("Don't forget to learn.",a.title);
        String when="2026-10-14T16:00:00+08:00[Asia/Shanghai]";
        byte[] zip=DataExport.encode(List.of(a),List.of(word),List.of(sentence),when,"zip");
        Map<String,byte[]> files=new HashMap<>();
        try(var input=new ZipInputStream(new ByteArrayInputStream(zip),StandardCharsets.UTF_8)) {
            ZipEntry entry;while((entry=input.getNextEntry())!=null)files.put(entry.getName(),input.readAllBytes());
        }
        assertEquals(Set.of("最新数据包.md","data.json","说明.md","manifest.json"),files.keySet());
        JSONObject data=new JSONObject(new String(files.get("data.json"),StandardCharsets.UTF_8));
        JSONObject article=data.getJSONArray("articles").getJSONObject(0);
        assertEquals(a.english,article.getString("english"));assertEquals(a.chinese,article.getString("chinese"));
        assertEquals(a.expressions,article.getString("expressions"));assertTrue(article.getBoolean("favorite"));assertEquals(a.createdAt,article.getLong("createdAt"));
        JSONObject savedWord=data.getJSONArray("vocabulary").getJSONObject(0);
        assertEquals(word.meaning(),savedWord.getString("meaning"));assertEquals(3,savedWord.getInt("streak"));assertEquals(1234567,savedWord.getLong("due"));
        assertEquals(sentence.english(),data.getJSONArray("sentences").getJSONObject(0).getString("english"));
        JSONObject manifest=new JSONObject(new String(files.get("manifest.json"),StandardCharsets.UTF_8));
        assertEquals(1,manifest.getInt("articleCount"));assertEquals(1,manifest.getInt("wordCount"));assertEquals(1,manifest.getInt("sentenceCount"));
        JSONArray checks=manifest.getJSONArray("files");
        for(int i=0;i<checks.length();i++) {
            JSONObject check=checks.getJSONObject(i);byte[] bytes=files.get(check.getString("name"));
            assertEquals(bytes.length,check.getLong("bytes"));assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)),check.getString("sha256"));
        }
        byte[] markdown=DataExport.encode(List.of(a),List.of(word),List.of(sentence),when,"md");
        assertArrayEquals(files.get("最新数据包.md"),markdown);
        String readable=new String(markdown,StandardCharsets.UTF_8);
        for(String original:List.of(a.english,a.chinese,word.meaning(),sentence.english()))assertTrue(readable.contains(original));
        assertFalse(data.has("settings"));assertFalse(data.has("key"));
    }
    @Test public void emptyBooksAndUnknownHistoricalDatesExportWithoutInventingData() throws Exception {
        Article a=new Article();a.id="archive-original";a.date=a.id;a.period="NIGHT";a.english="Original.";
        String readable=new String(DataExport.encode(List.of(a),List.of(),List.of(),"now","md"),StandardCharsets.UTF_8);
        assertTrue(readable.contains("原文日期未提供"));assertTrue(readable.contains("暂无生词"));assertTrue(readable.contains("暂无精句"));
        assertThrows(IllegalArgumentException.class,()->DataExport.encode(List.of(),List.of(),List.of(),"now","exe"));
    }
}
