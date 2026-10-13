package com.dailyenglish.app;

import org.json.*;

public final class ContentParser {
    public static Article parse(String json, SlotClock.Slot slot) throws JSONException {
        JSONObject data = new JSONObject(json);
        Article a = new Article();
        a.id = slot.key(); a.date = slot.date().toString(); a.period = slot.period();
        a.title = data.getString("title").trim();
        a.english = data.getString("english").trim();
        a.chinese = data.getString("chinese").trim();
        JSONArray expressions = data.getJSONArray("expressions");
        int words = SlotClock.wordCount(a.english);
        // 120–140 words is approximately 45–60 seconds at a conversational US pace.
        if (a.title.isEmpty() || a.title.length() > 100 || words < 120 || words > 140
                || !a.english.matches("(?s).*[A-Za-z].*") || a.english.matches("(?s).*[\\u4e00-\\u9fff].*")
                || a.chinese.length() < 80 || a.chinese.length() > 3000 || !a.chinese.matches("(?s).*[\\u4e00-\\u9fff].*")
                || expressions.length() < 3 || expressions.length() > 5) throw new JSONException("内容格式不符合要求");
        for (int i = 0; i < expressions.length(); i++) {
            JSONObject e = expressions.getJSONObject(i);
            if (e.getString("en").isBlank() || e.getString("en").length() > 200
                    || !a.english.toLowerCase(java.util.Locale.ROOT).contains(e.getString("en").toLowerCase(java.util.Locale.ROOT))
                    || e.getString("zh").isBlank() || e.getString("zh").length() > 400)
                throw new JSONException("表达列表不完整");
        }
        a.expressions = expressions.toString();
        a.source = "DeepSeek 新生成 · 预计45–60秒（实际随语速变化）";
        a.createdAt = slot.time().toInstant().toEpochMilli();
        return a;
    }
}
