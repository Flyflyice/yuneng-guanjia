package com.yuneng.guanjia.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class Formats {
    private static final Locale CN = Locale.CHINA;

    private Formats() {
    }

    public static String dateTime(long ts) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", CN).format(new Date(ts));
    }

    public static String date(long ts) {
        return new SimpleDateFormat("yyyy-MM-dd", CN).format(new Date(ts));
    }

    public static String dateSlash(long ts) {
        return new SimpleDateFormat("yyyy/MM/dd", CN).format(new Date(ts));
    }

    public static String monthDay(long ts) {
        return new SimpleDateFormat("MM/dd", CN).format(new Date(ts));
    }

    public static String monthDayDot(long ts) {
        return new SimpleDateFormat("MM.dd", CN).format(new Date(ts));
    }

    public static String time(long ts) {
        return new SimpleDateFormat("HH:mm", CN).format(new Date(ts));
    }

    public static String yearMonthDayChinese(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return c.get(Calendar.YEAR) + "年" + (c.get(Calendar.MONTH) + 1) + "月"
                + c.get(Calendar.DAY_OF_MONTH) + "日";
    }

    public static String monthDayChinese(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return (c.get(Calendar.MONTH) + 1) + "月" + c.get(Calendar.DAY_OF_MONTH) + "日";
    }

    public static String weekday(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        String[] names = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return names[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static String friendly(long ts) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ts);
        if (sameDay(now, then)) return "今天 " + time(ts);
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(now, then)) return "昨天 " + time(ts);
        return monthDay(ts) + " " + time(ts);
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    public static String num(double v, int digits) {
        if (Double.isNaN(v) || Double.isInfinite(v)) v = 0;
        return String.format(CN, "%." + digits + "f", v);
    }

    public static String trim(double v, int maxDigits) {
        if (Double.isNaN(v) || Double.isInfinite(v)) v = 0;
        String s = String.format(CN, "%." + maxDigits + "f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static String money(double v) {
        return num(v, 2);
    }

    public static String price(double v) {
        return num(v, 2);
    }

    public static String km(double v) {
        return num(v, 0);
    }

    public static String percent(double v) {
        return num(v, 1);
    }

    public static double parse(String s, double fallback) {
        if (s == null) return fallback;
        s = s.trim().replace("，", "").replace(",", "");
        if (s.isEmpty() || ".".equals(s)) return fallback;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static long startOfDay(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static long daysBetween(long a, long b) {
        return Math.max(1, Math.abs(startOfDay(b) - startOfDay(a)) / 86400000L);
    }
}
