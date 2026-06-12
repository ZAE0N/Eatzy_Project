package com.inhatc.eatzy_project;

/** createdAt(ms) 를 "방금 전 / N분 전 / N시간 전 / N일 전" 으로 바꾼다. */
public class TimeUtil {

    public static String relative(long createdAt) {
        if (createdAt <= 0) return "";
        long diff = System.currentTimeMillis() - createdAt;
        if (diff < 0) diff = 0;

        long min = diff / 60000;
        if (min < 1) return "방금 전";
        if (min < 60) return min + "분 전";

        long hour = min / 60;
        if (hour < 24) return hour + "시간 전";

        long day = hour / 24;
        return day + "일 전";
    }

    private TimeUtil() { }
}
