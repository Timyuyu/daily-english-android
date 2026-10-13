package com.dailyenglish.app;

import java.time.*;
import java.util.*;

public final class SlotClock {
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public record Slot(LocalDate date, String period) {
        public String key() { return date + ":" + period; }
        public ZonedDateTime time() { return date.atTime(period.equals("DAY") ? 8 : 23, 0).atZone(ZONE); }
    }
    public static Slot current(Instant now) {
        ZonedDateTime t = now.atZone(ZONE);
        if (t.getHour() < 8) return new Slot(t.toLocalDate().minusDays(1), "NIGHT");
        return new Slot(t.toLocalDate(), t.getHour() < 23 ? "DAY" : "NIGHT");
    }
    public static Slot next(Instant now) {
        ZonedDateTime t = now.atZone(ZONE);
        if (t.getHour() < 8) return new Slot(t.toLocalDate(), "DAY");
        if (t.getHour() < 23) return new Slot(t.toLocalDate(), "NIGHT");
        return new Slot(t.toLocalDate().plusDays(1), "DAY");
    }
    public static List<Slot> due(LocalDate start, Instant now) {
        List<Slot> result = new ArrayList<>();
        LocalDate today = now.atZone(ZONE).toLocalDate();
        for (LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            for (String p : List.of("DAY", "NIGHT")) {
                Slot slot = new Slot(d, p);
                if (!slot.time().toInstant().isAfter(now)) result.add(slot);
            }
        }
        return result;
    }
    public static List<String> sentences(String english) {
        return Arrays.stream(english.trim().split("(?<=[.!?])\\s+|(?<=[.!?][\"”])\\s+"))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
    public static int wordCount(String english) {
        return english.trim().isEmpty() ? 0 : english.trim().split("\\s+").length;
    }
}
