package com.yuneng.guanjia.ui.screens;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.model.EnergyRecord;
import com.yuneng.guanjia.model.Stats;
import com.yuneng.guanjia.model.TimeRange;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.charts.BarChartView;
import com.yuneng.guanjia.ui.charts.DonutChartView;
import com.yuneng.guanjia.ui.widgets.AppBar;
import com.yuneng.guanjia.ui.widgets.IconView;
import com.yuneng.guanjia.ui.widgets.PickerDialog;
import com.yuneng.guanjia.util.Formats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 费用页：总支出、费用构成、月度支出与支出流水。 */
public class CostScreen extends BaseScreen {
    /** 费用页只保留一个时间窗口：绿色“总支出”卡右上角切换，其余卡片全部联动。 */
    private TimeRange summaryRange = TimeRange.lastYear();

    public CostScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected void build() {
        if (summaryRange == null) summaryRange = TimeRange.lastYear();
        AppBar bar = new AppBar(c);
        bar.title("费用").dark(false).background(Ui.GREEN);
        bar.actionIcon(IconView.PLUS, Color.WHITE, v -> chooseAdd());
        add(bar);
        scrollBody(24);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        body.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), 0);

        body.addView(summaryCard());
        body.addView(Ui.space(c, 10));
        body.addView(donutCard());
        body.addView(Ui.space(c, 10));
        body.addView(monthCard());
        body.addView(Ui.space(c, 10));
        body.addView(expenseListCard());
        body.addView(Ui.space(c, 10));
        body.addView(incomeCard());
        body.addView(Ui.space(c, 16));
    }

    private void chooseAdd() {
        PickerDialog.choose(c, "记一笔", new String[]{"支出", "收入"}, 0, (index, value) -> {
            if (index == 0) activity.openForm(EnergyRecord.TYPE_EXPENSE, null);
            else activity.openForm(EnergyRecord.TYPE_INCOME, null);
        });
    }

    private View summaryCard() {
        Stats s = repo.stats(vehicle, summaryRange);
        LinearLayout card = whiteCard();
        card.setBackground(Ui.gradient(new int[]{Ui.GREEN, Ui.GREEN_DARK}, Ui.dp(c, 18)));
        LinearLayout head = Ui.horizontal(c);
        head.addView(rangeLabel(summaryRange, 0xE6FFFFFF, r -> summaryRange = r));
        head.addView(Ui.regular(c, " 总支出（元）", 11.5f, 0xE6FFFFFF));
        card.addView(head);
        LinearLayout valueRow = Ui.horizontal(c);
        valueRow.setGravity(Gravity.BOTTOM);
        TextView value = Ui.black(c, Formats.num(s.totalCost, 2), 34, Color.WHITE);
        valueRow.addView(value);
        TextView unit = Ui.regular(c, "元", 12, 0xD9FFFFFF);
        unit.setPadding(Ui.dp(c, 5), 0, 0, Ui.dp(c, 7));
        valueRow.addView(unit);
        card.addView(valueRow);

        LinearLayout row = Ui.horizontal(c);
        row.setPadding(0, Ui.dp(c, 14), 0, 0);
        row.addView(mini("支出/公里", Formats.num(s.totalCostPerKm, 2), "元"), weight());
        row.addView(mini("能费/公里", Formats.num(s.energyCostPerKm, 2), "元"), weight());
        row.addView(mini("成本/天", Formats.num(s.costPerDay, 2), "元"), weight());
        card.addView(row);
        return card;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    }

    private View mini(String label, String value, String unit) {
        LinearLayout box = Ui.vertical(c);
        box.addView(Ui.regular(c, label, 10, 0xCCFFFFFF));
        LinearLayout row = Ui.horizontal(c);
        row.setGravity(Gravity.BOTTOM);
        row.addView(Ui.bold(c, value, 16, Color.WHITE));
        TextView u = Ui.regular(c, unit, 9, 0xCCFFFFFF);
        u.setPadding(Ui.dp(c, 2), 0, 0, Ui.dp(c, 2));
        row.addView(u);
        box.addView(row);
        return box;
    }

    private View donutCard() {
        Stats s = repo.stats(vehicle, summaryRange);
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "费用构成", 15, Ui.INK));
        title.addView(Ui.flexible(c));
        card.addView(title);

        Map<String, Double> parts = categoryTotals(summaryRange, s);
        List<Float> values = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, Double> e : parts.entrySet()) {
            if (e.getValue() <= 0) continue;
            values.add(e.getValue().floatValue());
            names.add(e.getKey());
            colors.add(categoryColor(e.getKey()));
        }
        if (values.isEmpty()) {
            values.add(1f);
            colors.add(0xFFE8ECEF);
        }

        DonutChartView donut = new DonutChartView(c);
        donut.setData(values, colors, Formats.money(s.totalCost));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 210));
        dlp.topMargin = Ui.dp(c, 6);
        card.addView(donut, dlp);
        donut.animateIn();

        if (names.isEmpty()) {
            TextView empty = Ui.regular(c, "暂无支出记录", 10.5f, Ui.MUTED);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 2));
            card.addView(empty);
            return card;
        }

        LinearLayout legend = Ui.vertical(c);
        for (int i = 0; i < names.size(); i += 2) {
            LinearLayout row = Ui.horizontal(c);
            row.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 3));
            row.addView(legendItem(names.get(i), colors.get(i), values.get(i)), weight());
            if (i + 1 < names.size()) {
                row.addView(legendItem(names.get(i + 1), colors.get(i + 1), values.get(i + 1)), weight());
            } else {
                row.addView(new View(c), weight());
            }
            legend.addView(row);
        }
        card.addView(legend);
        return card;
    }

    private View legendItem(String name, int color, float value) {
        LinearLayout row = Ui.horizontal(c);
        row.setPadding(0, 0, Ui.dp(c, 10), 0);
        View dot = new View(c);
        dot.setBackground(Ui.circle(color, Ui.dp(c, 8)));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(c, 8), Ui.dp(c, 8));
        dlp.rightMargin = Ui.dp(c, 5);
        row.addView(dot, dlp);
        TextView label = Ui.regular(c, name, 10, Ui.SUB);
        label.setSingleLine(true);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nlp = new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        nlp.rightMargin = Ui.dp(c, 6);
        row.addView(label, nlp);
        TextView amount = Ui.medium(c, Formats.num(value, 0), 10, Ui.INK_2);
        amount.setSingleLine(true);
        row.addView(amount);
        return row;
    }

    private View monthCard() {
        Stats s = repo.stats(vehicle, summaryRange);
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        View bar = new View(c);
        bar.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 2)));
        title.addView(bar, new LinearLayout.LayoutParams(Ui.dp(c, 3), Ui.dp(c, 15)));
        title.addView(Ui.bold(c, " 支出月度统计", 14.5f, Ui.INK));
        title.addView(Ui.regular(c, "（自然月）", 9.5f, Ui.SUB));
        title.addView(Ui.flexible(c));
        card.addView(title);

        BarChartView chart = new BarChartView(c);
        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        for (Stats.MonthPoint m : s.months) {
            labels.add(m.label);
            values.add((float) m.totalCost());
        }
        chart.setData(labels, values);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 170));
        lp.topMargin = Ui.dp(c, 8);
        card.addView(chart, lp);
        chart.animateIn();
        return card;
    }

    private View expenseListCard() {
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "支出明细", 15, Ui.INK));
        title.addView(Ui.flexible(c));
        title.addView(Ui.spaceW(c, 6));
        TextView all = Ui.regular(c, "全部  >", 10.5f, Ui.SUB);
        title.addView(all);
        card.addView(title);

        List<EnergyRecord> expenses = inRange(repo.getRecords(vehicle.id, EnergyRecord.TYPE_EXPENSE), summaryRange);
        Ui.clickable(all, v -> activity.openRecordList());
        if (expenses.isEmpty()) {
            TextView empty = Ui.regular(c, "该时间段内还没有支出记录", 12, Ui.MUTED);
            empty.setPadding(0, Ui.dp(c, 18), 0, Ui.dp(c, 8));
            card.addView(empty);
        } else {
            int count = Math.min(6, expenses.size());
            for (int i = 0; i < count; i++) {
                EnergyRecord r = expenses.get(i);
                card.addView(recordRow(r));
                if (i != count - 1) card.addView(Ui.dashedDivider(c));
            }
        }
        return card;
    }

    private View recordRow(EnergyRecord r) {
        LinearLayout row = Ui.horizontal(c);
        row.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 10));
        View iconBg = new View(c);
        iconBg.setBackground(Ui.circle(0x14FF6B62, Ui.dp(c, 36)));
        FrameLayout iconBox = Ui.frame(c);
        iconBox.addView(iconBg, new LinearLayout.LayoutParams(Ui.dp(c, 36), Ui.dp(c, 36)));
        iconBox.addView(Ui.icon(c, categoryIcon(r.category), 19, 0xFFFF6B62),
                new android.widget.FrameLayout.LayoutParams(Ui.dp(c, 19), Ui.dp(c, 19), Gravity.CENTER));
        row.addView(iconBox, new LinearLayout.LayoutParams(Ui.dp(c, 36), Ui.dp(c, 36)));
        LinearLayout texts = Ui.vertical(c);
        texts.setPadding(Ui.dp(c, 10), 0, 0, 0);
        texts.addView(Ui.medium(c, r.category == null || r.category.isEmpty() ? "其他支出" : r.category, 12.5f, Ui.INK));
        texts.addView(Ui.regular(c, Formats.date(r.timestamp) + "  " + (safe(r.note).isEmpty() ? "无备注" : r.note), 9.5f, Ui.SUB));
        row.addView(texts, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Ui.bold(c, "-" + Formats.num(r.amount, 2), 13, Ui.INK));
        Ui.clickable(row, v -> activity.openForm(EnergyRecord.TYPE_EXPENSE, r));
        return row;
    }

    private View incomeCard() {
        Stats s = repo.stats(vehicle, summaryRange);
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "收入", 15, Ui.INK));
        title.addView(Ui.flexible(c));
        title.addView(Ui.spaceW(c, 6));
        TextView add = Ui.medium(c, "+ 记收入", 10.5f, Ui.GREEN_DARK);
        title.addView(add);
        Ui.clickable(add, v -> activity.openForm(EnergyRecord.TYPE_INCOME, null));
        card.addView(title);

        List<EnergyRecord> incomes = inRange(repo.getRecords(vehicle.id, EnergyRecord.TYPE_INCOME), summaryRange);
        TextView total = Ui.regular(c, "收入合计", 11, Ui.SUB);
        total.setPadding(0, Ui.dp(c, 6), 0, 0);
        card.addView(total);
        TextView amount = Ui.bold(c, Formats.money(s.income) + " 元", 20, Ui.GREEN_DARK);
        card.addView(amount);
        if (!incomes.isEmpty()) {
            EnergyRecord last = incomes.get(0);
            TextView desc = Ui.regular(c, "最近：" + (safe(last.category).isEmpty() ? "其他" : last.category)
                    + " · " + Formats.date(last.timestamp) + " · " + Formats.money(last.amount) + " 元", 10, Ui.SUB);
            desc.setPadding(0, Ui.dp(c, 6), 0, 0);
            card.addView(desc);
            Ui.clickable(desc, v -> activity.openForm(EnergyRecord.TYPE_INCOME, last));
        } else {
            TextView empty = Ui.regular(c, "该时间段内还没有收入记录", 10.5f, Ui.MUTED);
            empty.setPadding(0, Ui.dp(c, 6), 0, 0);
            card.addView(empty);
        }
        return card;
    }

    private Map<String, Double> categoryTotals(TimeRange range, Stats stats) {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("油费", stats.fuelCost);
        map.put("电费", stats.chargeCost);
        Map<String, Double> ext = new LinkedHashMap<>();
        for (EnergyRecord r : repo.getRecords(vehicle.id)) {
            if (!r.isExpense()) continue;
            if (range != null && !range.contains(r.timestamp)) continue;
            String key = r.category == null || r.category.isEmpty() ? "其他" : r.category;
            ext.put(key, ext.containsKey(key) ? ext.get(key) + r.amount : r.amount);
        }
        map.putAll(ext);
        return map;
    }

    /** 只保留给定时间窗口内的记录。 */
    private List<EnergyRecord> inRange(List<EnergyRecord> source, TimeRange range) {
        if (range == null || range.isAll()) return source;
        List<EnergyRecord> list = new ArrayList<>();
        for (EnergyRecord r : source) {
            if (range.contains(r.timestamp)) list.add(r);
        }
        return list;
    }

    private int categoryColor(String name) {
        switch (name) {
            case "油费":
                return Ui.GREEN;
            case "电费":
                return 0xFF22C7C7;
            case "保险和交通规费":
                return 0xFF2196F3;
            case "停车费":
                return 0xFFFFC107;
            case "维修":
                return 0xFFFF6B62;
            case "高速费":
                return 0xFF9C6BFF;
            case "过路费":
                return 0xFF4D9EFF;
            default:
                return 0xFFB9C0C7;
        }
    }

    private int categoryIcon(String name) {
        if (name == null) return IconView.MORE;
        if (name.contains("保险")) return IconView.DOCUMENT;
        if (name.contains("停车")) return IconView.LOCATION;
        if (name.contains("维修") || name.contains("保养")) return IconView.SETTINGS;
        if (name.contains("高速") || name.contains("过路")) return IconView.ROAD;
        if (name.contains("油")) return IconView.FUEL;
        if (name.contains("电")) return IconView.BOLT;
        return IconView.MORE;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
