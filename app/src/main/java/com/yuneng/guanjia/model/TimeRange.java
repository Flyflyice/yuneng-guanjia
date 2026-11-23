package com.yuneng.guanjia.model;

import java.util.Calendar;
import java.util.Locale;

/**
 * 统计时间窗口：全部 / 近一年 / 自然年 / 自然月 / 自定义起止日期。
 *
 * 区间统一为左闭右开 [startMs, endMs)，避免跨月、跨年边界被重复统计。
 */
public final class TimeRange {
    public static final int MODE_ALL = 0;
    public static final int MODE_ROLLING_YEAR = 1;
    public static final int MODE_YEAR = 2;
    public static final int MODE_MONTH = 3;
    public static final int MODE_CUSTOM = 4;

    /** 自定义时间段允许的最长跨度（天），避免图表柱子过密。 */
    public static final int MAX_CUSTOM_DAYS = 1096;

    public final int mode;
    /** 起始时间戳，包含。 */
    public final long startMs;
    /** 结束时间戳，不包含。 */
    public final long endMs;
    /** 自然年/自然月模式下的年份，其余模式为 0。 */
    public final int year;
    /** 自然月模式下的月份（1-12），其余模式为 0。 */
    public final int month;

    private TimeRange(int mode, long startMs, long endMs, int year, int month) {
        this.mode = mode;
        this.startMs = startMs;
        this.endMs = endMs;
        this.year = year;
        this.month = month;
    }

    /** 全部记录（不限制时间）。 */
    public static TimeRange all() {
        return new TimeRange(MODE_ALL, Long.MIN_VALUE, Long.MAX_VALUE, 0, 0);
    }

    /**
     * 最近 12 个自然月（含本月），也就是默认的“近一年”。
     * 例如当前是 2025 年 11 月，则范围为 2024-12-01 00:00 至 2025-12-01 00:00。
     */
    public static TimeRange lastYear() {
        Calendar end = startOfMonth(Calendar.getInstance());
        end.add(Calendar.MONTH, 1);
        Calendar start = (Calendar) end.clone();
        start.add(Calendar.MONTH, -12);
        return new TimeRange(MODE_ROLLING_YEAR,
                start.getTimeInMillis(), end.getTimeInMillis(), 0, 0);
    }

    /** 指定自然年，1 月 1 日 00:00 至次年 1 月 1 日 00:00。 */
    public static TimeRange year(int y) {
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(y, Calendar.JANUARY, 1, 0, 0, 0);
        Calendar end = Calendar.getInstance();
        end.clear();
        end.set(y + 1, Calendar.JANUARY, 1, 0, 0, 0);
        return new TimeRange(MODE_YEAR, start.getTimeInMillis(), end.getTimeInMillis(), y, 0);
    }

    /** 指定自然月，当月 1 日 00:00 至次月 1 日 00:00。 */
    public static TimeRange month(int y, int m) {
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(y, m - 1, 1, 0, 0, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.MONTH, 1);
        return new TimeRange(MODE_MONTH, start.getTimeInMillis(), end.getTimeInMillis(), y, m);
    }

    /** 自定义起止日期（按自然日取整）：起始日 00:00 至结束日次日 00:00。 */
    public static TimeRange custom(long startTs, long endTs) {
        if (endTs < startTs) {
            long t = startTs;
            startTs = endTs;
            endTs = t;
        }
        long start = startOfDay(startTs);
        long end = startOfDay(endTs) + 86400000L;
        return new TimeRange(MODE_CUSTOM, start, end, 0, 0);
    }

    public boolean isAll() {
        return mode == MODE_ALL;
    }

    public boolean contains(long ts) {
        return ts >= startMs && ts < endMs;
    }

    /** 该窗口覆盖的自然月数量，用于图表横轴。 */
    public int monthCount() {
        if (mode == MODE_YEAR || mode == MODE_ROLLING_YEAR) return 12;
        if (mode == MODE_MONTH) return 1;
        if (mode == MODE_CUSTOM) {
            Calendar s = Calendar.getInstance();
            s.setTimeInMillis(startMs);
            Calendar e = Calendar.getInstance();
            e.setTimeInMillis(endMs - 1);
            int count = (e.get(Calendar.YEAR) - s.get(Calendar.YEAR)) * 12
                    + (e.get(Calendar.MONTH) - s.get(Calendar.MONTH)) + 1;
            return Math.max(1, count);
        }
        return 12;
    }

    /** 卡片角标用的短标签，例如“近一年”“2025年”“2025.11”“自定义”。 */
    public String shortLabel() {
        switch (mode) {
            case MODE_ALL:
                return "全部";
            case MODE_ROLLING_YEAR:
                return "近一年";
            case MODE_YEAR:
                return String.format(Locale.CHINA, "%d年", year);
            case MODE_MONTH:
                return String.format(Locale.CHINA, "%d.%02d", year, month);
            default:
                return "自定义";
        }
    }

    /** 选择器状态用的模式名。 */
    public String modeLabel() {
        switch (mode) {
            case MODE_ALL:
                return "全部";
            case MODE_ROLLING_YEAR:
                return "近一年";
            case MODE_YEAR:
                return "年度";
            case MODE_MONTH:
                return "月度";
            default:
                return "自定义";
        }
    }

    /** 完整区间文字，例如“2025-01-01 至 2025-12-31”。 */
    public String describe() {
        if (mode == MODE_ALL) return "全部记录";
        return date(startMs) + " 至 " + date(endMs - 1);
    }

    /** 区间覆盖的天数；全部记录返回 0 表示不限。 */
    public int spanDays() {
        if (mode == MODE_ALL) return 0;
        return (int) Math.max(1, (endMs - startMs) / 86400000L);
    }

    public static String date(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return String.format(Locale.CHINA, "%d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String monthText(int y, int m) {
        return String.format(Locale.CHINA, "%d年%d月", y, m);
    }

    private static long startOfDay(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static Calendar startOfMonth(Calendar src) {
        Calendar c = (Calendar) src.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }
}
