package com.dailyenglish.app;

import org.json.*;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class DeepSeek {
    public static final String MODEL = "deepseek-flash";
    public static class ApiError extends IOException {
        public final int status;
        ApiError(int status) { super("API HTTP " + status); this.status = status; }
    }
    public static Article generate(String key, SlotClock.Slot slot, List<String> recent) throws Exception {
        JSONObject request = new JSONObject();
        request.put("model", MODEL);
        request.put("thinking", new JSONObject().put("type", "disabled"));
        request.put("response_format", new JSONObject().put("type", "json_object"));
        request.put("max_tokens", 2200);
        request.put("temperature", 0.8);
        String system = "你是美式英语口语教练。生成适合上海成年人日常生活观察、个人成长、关系与边界的英文口播。"
                + "必须是自然的第一人称美式英语，英文严格120到140个英文单词，正常口语约45–60秒。不要中英混排。"
                + "使用当代美国成年人日常会说的口语、自然缩略形式和常见搭配，避免直译中文、书面腔、刻意堆俚语。句子短而易于跟读。"
                + "返回json对象：{\"title\":\"中文标题\",\"english\":\"完整英文篇章，句末使用标点\","
                + "\"chinese\":\"逐句忠实的完整中文翻译，不要总结替代翻译\",\"expressions\":[{\"en\":\"来自正文的表达\",\"zh\":\"中文解释\"}]}。"
                + "expressions应有3至5项。仅返回json，不要代码围栏、音标、额外栏目或虚构时事。";
        JSONArray messages = new JSONArray().put(new JSONObject().put("role", "system").put("content", system))
                .put(new JSONObject().put("role", "user").put("content", "日期：" + slot.date() + "，时段："
                        + (slot.period().equals("DAY") ? "白天，积极清晰" : "晚上，安静反思")
                        + "。请避免重复这些近期主题：" + String.join("、", recent)));
        request.put("messages", messages);
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.deepseek.com/chat/completions").openConnection();
        connection.setConnectTimeout(15000); connection.setReadTimeout(75000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST"); connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestProperty("Authorization", "Bearer " + key);
        try {
            try (OutputStream out = connection.getOutputStream()) { out.write(request.toString().getBytes(StandardCharsets.UTF_8)); }
            int status = connection.getResponseCode();
            if (status != 200) throw new ApiError(status); // Never expose response bodies or credentials in errors/logs.
            byte[] bytes;
            try (InputStream input = connection.getInputStream()) { bytes = input.readNBytes(262145); }
            if (bytes.length > 262144) throw new IOException("响应过大");
            JSONObject response = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            if (!choice.optString("finish_reason").equals("stop")) throw new JSONException("生成未完成");
            return ContentParser.parse(choice.getJSONObject("message").getString("content"), slot);
        } finally { connection.disconnect(); }
    }
}
