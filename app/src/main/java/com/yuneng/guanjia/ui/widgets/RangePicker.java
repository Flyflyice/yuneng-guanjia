package com.yuneng.guanjia.ui.widgets;

import android.content.Context;

import com.yuneng.guanjia.model.TimeRange;
import com.yuneng.guanjia.ui.Ui;

import java.util.Calendar;

/**
 * 卡片级时间范围选择器。
 * 点击卡片标题旁的“近一年 ▾”后依次选择 近一年 / 年度 / 月度 / 自定义，
 * 选定后通过 {@link Listener} 回调新的 {@link TimeRange}。
 */
public final class RangePicker {
    public interface Listener {
        void onRange(TimeRange range);
    }

    private static final String[] MODES = {"近一年", "年度", "月度", "自定义"};

    private RangePicker() {
    }

    public static void show(Context c, TimeRange current, Listener listener) {
        if (listener == null) return;
        int selected = 0;
        if (current != null) {
            if (current.mode == TimeRange.MODE_YEAR) selected = 1;
            else if (current.mode == TimeRange.MODE_MONTH) selected = 2;
            else if (current.mode == TimeRange.MODE_CUSTOM) selected = 3;
        }
        PickerDialog.choose(c, "选择时间范围", MODES, selected, (index, value) -> {
            if (index == 0) {
                listener.onRange(TimeRange.lastYear());
            } else if (index == 1) {
                pickYear(c, current, listener);
            } else if (index == 2) {
                pickMonth(c, current, listener);
            } else {
                pickCustom(c, current, listener);
            }
        });
    }

    /** 年份列表：近 10 年，最新在前。 */
    private static void pickYear(Context c, TimeRange current, Listener listener) {
        final int now = Calendar.getInstance().get(Calendar.YEAR);
        String[] items = new String[10];
        for (int i = 0; i < items.length; i++) items[i] = (now - i) + "年";
        int selected = 0;
        if (current != null && current.mode == TimeRange.MODE_YEAR
                && current.year <= now && current.year > now - 10) {
            selected = now - current.year;
        }
        PickerDialog.choose(c, "选择年份", items, selected,
                (index, value) -> listener.onRange(TimeRange.year(now - index)));
    }

    /** 月份列表：最近 24 个月，最新在前。 */
    private static void pickMonth(Context c, TimeRange current, Listener listener) {
        final int[] years = new int[24];
        final int[] months = new int[24];
        String[] items = new String[24];
        Calendar cur = Calendar.getInstance();
        for (int i = 0; i < items.length; i++) {
            years[i] = cur.get(Calendar.YEAR);
            months[i] = cur.get(Calendar.MONTH) + 1;
            items[i] = TimeRange.monthText(years[i], months[i]);
            cur.add(Calendar.MONTH, -1);
        }
        int selected = 0;
        if (current != null && current.mode == TimeRange.MODE_MONTH) {
            for (int i = 0; i < items.length; i++) {
                if (years[i] == current.year && months[i] == current.month) {
                    selected = i;
                    break;
                }
            }
        }
        PickerDialog.choose(c, "选择月份", items, selected,
                (index, value) -> listener.onRange(TimeRange.month(years[index], months[index])));
    }

    /** 自定义：先选开始日期，再选结束日期（最多 3 年）。 */
    private static void pickCustom(Context c, TimeRange current, Listener listener) {
        long today = System.currentTimeMillis();
        long start = current == null || current.isAll() ? today : current.startMs;
        long end = current == null || current.isAll() ? today : current.endMs - 1;
        PickerDialog.date(c, "选择开始日期", start, (i1, v1) -> {
            final long s = Long.parseLong(v1);
            final long defaultEnd = Math.max(s, end);
            PickerDialog.date(c, "选择结束日期", defaultEnd, (i2, v2) -> {
                long e = Long.parseLong(v2);
                if (e < s) {
                    Ui.toast(c, "结束日期不能早于开始日期");
                    return;
                }
                TimeRange next = TimeRange.custom(s, e);
                if (next.spanDays() > TimeRange.MAX_CUSTOM_DAYS) {
                    Ui.toast(c, "自定义区间最多 3 年，已自动截断");
                    next = TimeRange.custom(s, s + (long) TimeRange.MAX_CUSTOM_DAYS * 86400000L);
                }
                listener.onRange(next);
            });
        });
    }
}
