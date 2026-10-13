package com.dailyenglish.app;

import org.junit.Test;
import org.json.*;
import java.time.*;
import static org.junit.Assert.*;

public class CoreTest {
    private Instant at(String local) { return LocalDateTime.parse(local).atZone(SlotClock.ZONE).toInstant(); }
    @Test public void boundariesAndCatchUpAreShanghaiBased() {
        assertEquals("2026-10-12:NIGHT",SlotClock.current(at("2026-10-13T07:59:59")).key());
        assertEquals("2026-10-13:DAY",SlotClock.current(at("2026-10-13T08:00:00")).key());
        assertEquals("2026-10-13:DAY",SlotClock.current(at("2026-10-13T22:59:59")).key());
        assertEquals("2026-10-13:NIGHT",SlotClock.current(at("2026-10-13T23:00:00")).key());
        assertEquals("2026-10-14:DAY",SlotClock.next(at("2026-10-13T23:00:00")).key());
        assertEquals("2026-10-13:NIGHT",SlotClock.next(at("2026-10-13T08:00:00")).key());
        var due=SlotClock.due(LocalDate.parse("2026-10-11"),at("2026-10-13T08:00:00"));
        assertEquals(5,due.size()); assertEquals("2026-10-13:DAY",due.get(4).key());
        assertEquals(5,due.stream().map(SlotClock.Slot::key).distinct().count());
        assertTrue(SlotClock.due(LocalDate.parse("2026-10-13"),at("2026-10-13T07:59:59")).isEmpty());
        assertEquals("NIGHT",SlotClock.current(Instant.parse("2026-10-13T15:00:00Z")).period());
    }
    @Test public void sentencesPreserveOriginalTextAndNumbers() {
        var lines=SlotClock.sentences("I paid 3.5 dollars.\n\nIt felt good! Are you ready? Yes.");
        assertEquals(4,lines.size());assertEquals("I paid 3.5 dollars.",lines.get(0));
        assertEquals("Yes.",lines.get(3));
        assertEquals(2,SlotClock.sentences("I thought, “Why is it slow?” Then I walked.").size());
    }
    private JSONObject valid() throws Exception {
        return new JSONObject().put("title","一次普通的散步").put("english","Walking ".repeat(119)+"helps.")
                .put("chinese","这是一段忠实的中文翻译。".repeat(10))
                .put("expressions",new JSONArray().put(new JSONObject().put("en","Walking").put("zh","散步"))
                        .put(new JSONObject().put("en","helps").put("zh","有帮助"))
                        .put(new JSONObject().put("en","Walking helps").put("zh","散步有帮助")));
    }
    @Test public void parserRejectsIncompleteAndOversizedContent() throws Exception {
        var slot=new SlotClock.Slot(LocalDate.parse("2026-10-13"),"DAY");
        Article parsed=ContentParser.parse(valid().toString(),slot);assertEquals(slot.key(),parsed.id);
        assertThrows(JSONException.class,()->ContentParser.parse(valid().put("english","Too short.").toString(),slot));
        assertThrows(JSONException.class,()->ContentParser.parse(valid().put("expressions",new JSONArray()).toString(),slot));
        assertThrows(JSONException.class,()->ContentParser.parse(valid().put("chinese","总结").toString(),slot));
        assertThrows(JSONException.class,()->ContentParser.parse("{}",slot));
    }
}
