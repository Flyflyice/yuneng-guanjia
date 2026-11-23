package com.yuneng.guanjia.ui.screens;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.model.EnergyRecord;
import com.yuneng.guanjia.model.Stats;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.charts.DonutChartView;
import com.yuneng.guanjia.ui.widgets.AppBar;
import com.yuneng.guanjia.ui.widgets.IconView;
import com.yuneng.guanjia.util.Formats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 统计页：总支出拆解、费用结构、分类排行与第五代 DM-i 能耗折算。 */
public class StatsScreen extends BaseScreen {
    /** 总支出、费用构成、分类排行都按“有记录以来的全量”口径统计，彼此联动。 */
    private Stats baseStats;

    public StatsScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected void build() {
        baseStats = repo.stats(vehicle);
        AppBar bar = new AppBar(c);
        bar.title("统计").dark(false).background(Ui.GREEN);
        bar.actionIcon(IconView.SHARE, Color.WHITE, v -> activity.openTrip());
        add(bar);
        scrollBody(24);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        body.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), 0);

        body.addView(totalCard());
        body.addView(Ui.space(c, 10));
        body.addView(donutCard());
        body.addView(Ui.space(c, 10));
        body.addView(rankingCard());
        body.addView(Ui.space(c, 10));
        if (vehicle.isHybrid()) {
            body.addView(dmiCard());
        } else if (vehicle.isElectric()) {
            body.addView(evCard());
        } else {
            body.addView(iceCard());
        }
        body.addView(Ui.space(c, 10));
        body.addView(healthCard());
        body.addView(Ui.space(c, 16));
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    }

    private View totalCard() {
        Stats s = baseStats;
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "总支出", 15, Ui.INK));
        title.addView(Ui.icon(c, IconView.QUESTION, 13, Ui.MUTED));
        title.addView(Ui.flexible(c));
        title.addView(Ui.regular(c, "全部记录", 10.5f, Ui.SUB));
        card.addView(title);

        TextView total = Ui.black(c, Formats.num(s.totalCost, 1), 30, 0xFFFF3B30);
        total.setPadding(0, Ui.dp(c, 4), 0, 0);
        card.addView(total);
        TextView eq = Ui.regular(c, "= 油费总计 + 电费总计 + 其他支出", 10, Ui.SUB);
        card.addView(eq);

        LinearLayout breakdown = Ui.horizontal(c);
        breakdown.setPadding(0, Ui.dp(c, 12), 0, 0);
        breakdown.addView(part("油费总计", Formats.num(s.fuelCost, 1), "元"), weight());
        breakdown.addView(plus());
        breakdown.addView(part("电费总计", Formats.num(s.chargeCost, 1), "元"), weight());
        breakdown.addView(plus());
        breakdown.addView(part("其他支出", Formats.num(s.otherExpense, 1), "元"), weight());
        card.addView(breakdown);
        card.addView(Ui.space(c, 8));
        card.addView(Ui.dashedDivider(c));
        card.addView(Ui.space(c, 10));

        LinearLayout metrics = Ui.horizontal(c);
        metrics.addView(metric("支出/公里", Formats.num(s.totalCostPerKm, 2), "元"), weight());
        metrics.addView(metric("能费/公里", Formats.num(s.energyCostPerKm, 2), "元"), weight());
        metrics.addView(metric("成本/天", Formats.num(s.costPerDay, 2), "元"), weight());
        card.addView(metrics);
        return card;
    }

    private View plus() {
        TextView t = Ui.regular(c, "+", 13, Ui.MUTED);
        t.setPadding(Ui.dp(c, 5), Ui.dp(c, 16), Ui.dp(c, 5), 0);
        return t;
    }

    private View part(String label, String value, String unit) {
        LinearLayout box = Ui.vertical(c);
        box.addView(Ui.regular(c, label, 9.5f, Ui.SUB));
        LinearLayout row = Ui.horizontal(c);
        row.setGravity(Gravity.BOTTOM);
        row.addView(Ui.bold(c, value, 14, Ui.INK));
        TextView u = Ui.regular(c, unit, 9, Ui.SUB);
        u.setPadding(Ui.dp(c, 2), 0, 0, Ui.dp(c, 2));
        row.addView(u);
        box.addView(row);
        return box;
    }

    private View metric(String label, String value, String unit) {
        LinearLayout box = Ui.vertical(c);
        LinearLayout labelRow = Ui.horizontal(c);
        labelRow.addView(Ui.regular(c, label, 10, Ui.SUB));
        labelRow.addView(Ui.icon(c, IconView.QUESTION, 11, Ui.MUTED));
        box.addView(labelRow);
        LinearLayout row = Ui.horizontal(c);
        row.setGravity(Gravity.BOTTOM);
        row.addView(Ui.bold(c, value, 18, Ui.INK));
        TextView u = Ui.regular(c, unit, 9.5f, Ui.SUB);
        u.setPadding(Ui.dp(c, 3), 0, 0, Ui.dp(c, 2));
        row.addView(u);
        box.addView(row);
        return box;
    }

    private View donutCard() {
        Stats s = baseStats;
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        View bar = new View(c);
        bar.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 2)));
        title.addView(bar, new LinearLayout.LayoutParams(Ui.dp(c, 3), Ui.dp(c, 15)));
        title.addView(Ui.bold(c, " 费用构成统计", 14.5f, Ui.INK));
        title.addView(Ui.flexible(c));
        card.addView(title);

        Map<String, Double> parts = categoryTotals(s);
        List<Float> values = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, Double> e : parts.entrySet()) {
            if (e.getValue() <= 0) continue;
            values.add(e.getValue().floatValue());
            names.add(e.getKey());
            colors.add(color(e.getKey()));
        }
        DonutChartView donut = new DonutChartView(c);
        donut.setData(values, colors, Formats.num(s.totalCost, 0));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 220));
        card.addView(donut, dlp);
        donut.animateIn();

        LinearLayout legend = Ui.vertical(c);
        for (int i = 0; i < names.size(); i += 2) {
            LinearLayout row = Ui.horizontal(c);
            row.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 2));
            row.addView(legendItem(names.get(i), colors.get(i)), weight());
            if (i + 1 < names.size()) row.addView(legendItem(names.get(i + 1), colors.get(i + 1)), weight());
            legend.addView(row);
        }
        card.addView(legend);
        return card;
    }

    private View legendItem(String name, int color) {
        LinearLayout row = Ui.horizontal(c);
        View dot = new View(c);
        dot.setBackground(Ui.circle(color, Ui.dp(c, 8)));
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(c, 8), Ui.dp(c, 8));
        dlp.rightMargin = Ui.dp(c, 5);
        row.addView(dot, dlp);
        row.addView(Ui.regular(c, name, 10, Ui.SUB));
        return row;
    }

    private View rankingCard() {
        Stats s = baseStats;
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "分类排行", 15, Ui.INK));
        title.addView(Ui.flexible(c));
        card.addView(title);

        Map<String, Double> parts = categoryTotals(s);
        List<Map.Entry<String, Double>> list = new ArrayList<>(parts.entrySet());
        Collections.sort(list, (a, b) -> Double.compare(b.getValue(), a.getValue()));
        double max = list.isEmpty() ? 1 : list.get(0).getValue();
        int count = Math.min(6, list.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<String, Double> e = list.get(i);
            LinearLayout row = Ui.vertical(c);
            row.setPadding(0, Ui.dp(c, 7), 0, Ui.dp(c, 3));
            LinearLayout top = Ui.horizontal(c);
            top.addView(Ui.medium(c, e.getKey(), 11.5f, Ui.INK_2));
            top.addView(Ui.flexible(c));
            top.addView(Ui.medium(c, Formats.money(e.getValue()) + " 元", 11, Ui.INK));
            row.addView(top);
            FrameLayout track = Ui.frame(c);
            track.setBackground(Ui.round(0xFFF1F4F2, Ui.dp(c, 4)));
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 7));
            tlp.topMargin = Ui.dp(c, 5);
            row.addView(track, tlp);
            View fill = new View(c);
            fill.setBackground(Ui.gradient(new int[]{Ui.GREEN, Ui.GREEN_MID}, Ui.dp(c, 4)));
            int w = (int) Math.max(Ui.dp(c, 6),
                    (Ui.screenWidth(c) - Ui.dp(c, 58)) * (e.getValue() / max));
            android.widget.FrameLayout.LayoutParams flp = new android.widget.FrameLayout.LayoutParams(
                    w, Ui.dp(c, 7));
            track.addView(fill, flp);
            card.addView(row);
        }
        return card;
    }

    private View evCard() {
        Stats stats = baseStats;
        LinearLayout card = whiteCard();
        card.setBackground(Ui.gradient(new int[]{Ui.GREEN_TINT, Ui.GREEN_LIGHT}, Ui.dp(c, 18)));
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "纯电效率", 14.5f, Ui.GREEN_DEEP));
        title.addView(Ui.flexible(c));
        title.addView(Ui.icon(c, IconView.BOLT, 20, Ui.GREEN));
        card.addView(title);
        TextView sub = Ui.regular(c, "按充电电量与行驶里程折算 · 电池容量 "
                + Formats.num(stats.batteryCapacity, 2) + " 度", 10, Ui.SUB);
        sub.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 8));
        card.addView(sub);

        LinearLayout row1 = Ui.horizontal(c);
        row1.addView(dmiMetric("平均电耗", Formats.num(stats.avgKwh, 2), "度/百公里"), weight());
        row1.addView(dmiMetric("电池健康度", Formats.num(stats.batteryHealth, 1), "%"), weight());
        card.addView(row1);
        card.addView(Ui.space(c, 8));
        LinearLayout row2 = Ui.horizontal(c);
        row2.addView(dmiMetric("累计电费", Formats.num(stats.chargeCost, 0), "元"), weight());
        row2.addView(dmiMetric("每公里电费", Formats.num(stats.energyCostPerKm, 3), "元/公里"), weight());
        card.addView(row2);

        TextView formula = Ui.regular(c, "电耗 = 充电电量 ÷ 行驶里程 × 100。当前平均电耗 "
                + Formats.num(stats.avgKwh, 2) + " 度/百公里。", 9.5f, Ui.SUB);
        formula.setPadding(0, Ui.dp(c, 10), 0, 0);
        formula.setLineSpacing(Ui.dp(c, 3), 1f);
        card.addView(formula);
        return card;
    }

    private View iceCard() {
        Stats stats = baseStats;
        LinearLayout card = whiteCard();
        card.setBackground(Ui.gradient(new int[]{Ui.GREEN_TINT, Ui.GREEN_LIGHT}, Ui.dp(c, 18)));
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "燃油效率", 14.5f, Ui.GREEN_DEEP));
        title.addView(Ui.flexible(c));
        title.addView(Ui.icon(c, IconView.FUEL, 20, Ui.GREEN));
        card.addView(title);
        TextView sub = Ui.regular(c, "发动机热效率 " + Formats.num(stats.thermalEfficiency * 100, 2)
                + "% · 按加油量与行驶里程折算", 10, Ui.SUB);
        sub.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 8));
        card.addView(sub);

        LinearLayout row1 = Ui.horizontal(c);
        row1.addView(dmiMetric("平均油耗", Formats.num(stats.avgFuel, 2), "升/百公里"), weight());
        row1.addView(dmiMetric("每公里油费", Formats.num(stats.fuelCost / Math.max(1, stats.distance), 3), "元/公里"), weight());
        card.addView(row1);
        card.addView(Ui.space(c, 8));
        LinearLayout row2 = Ui.horizontal(c);
        row2.addView(dmiMetric("发动机热效率", Formats.num(stats.thermalEfficiency * 100, 2), "%"), weight());
        row2.addView(dmiMetric("累计油费", Formats.num(stats.fuelCost, 0), "元"), weight());
        card.addView(row2);

        TextView formula = Ui.regular(c, "油耗 = 加油量 ÷ 行驶里程 × 100。当前平均油耗 "
                + Formats.num(stats.avgFuel, 2) + " 升/百公里。", 9.5f, Ui.SUB);
        formula.setPadding(0, Ui.dp(c, 10), 0, 0);
        formula.setLineSpacing(Ui.dp(c, 3), 1f);
        card.addView(formula);
        return card;
    }

    private View dmiCard() {
        Stats stats = baseStats;
        LinearLayout card = whiteCard();
        card.setBackground(Ui.gradient(new int[]{Ui.GREEN_TINT, Ui.GREEN_LIGHT}, Ui.dp(c, 18)));
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, vehicle.energyLabel() + " 能耗折算", 14.5f, Ui.GREEN_DEEP));
        title.addView(Ui.flexible(c));
        title.addView(Ui.icon(c, IconView.LEAF, 20, Ui.GREEN));
        card.addView(title);
        TextView sub = Ui.regular(c, "发动机热效率 46.06% · 电驱效率 92% · 92# 汽油", 10, Ui.SUB);
        sub.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 8));
        card.addView(sub);

        LinearLayout row1 = Ui.horizontal(c);
        row1.addView(dmiMetric("等效油耗", Formats.num(stats.dmEquivalent, 2), "升/百公里"), weight());
        row1.addView(dmiMetric("每升油发电", Formats.num(stats.dmKwhPerL, 2), "kWh"), weight());
        card.addView(row1);
        card.addView(Ui.space(c, 8));
        LinearLayout row2 = Ui.horizontal(c);
        row2.addView(dmiMetric("发动机热效率", Formats.num(stats.thermalEfficiency * 100, 2), "%"), weight());
        row2.addView(dmiMetric("电量折油耗", Formats.num(stats.avgKwh * 0.31, 2), "升/百公里"), weight());
        card.addView(row2);
        if (stats.electricDistance > 0 || stats.fuelDistance > 0) {
            card.addView(Ui.space(c, 8));
            LinearLayout row3 = Ui.horizontal(c);
            row3.addView(dmiMetric("纯电里程", Formats.num(stats.electricDistance, 0), "公里"), weight());
            row3.addView(dmiMetric("燃油里程", Formats.num(stats.fuelDistance, 0), "公里"), weight());
            card.addView(row3);
            card.addView(Ui.space(c, 8));
            LinearLayout row4 = Ui.horizontal(c);
            row4.addView(dmiMetric("纯电电耗", Formats.num(stats.pureElectricEconomy, 2), "度/百公里"), weight());
            row4.addView(dmiMetric("混动里程", Formats.num(stats.hybridDistance, 0), "公里"), weight());
            card.addView(row4);
        }

        TextView formula = Ui.regular(c, "折算口径：综合能耗 = 平均油耗 + 平均电耗 ÷ (8.9 × 热效率 × 电驱效率)。"
                + "当前综合能耗 " + Formats.num(stats.combined, 2) + " 升/百公里。"
                + "纯电/燃油里程由“充电时剩余油量”和“加油时剩余电量”的变化拆分。", 9.5f, Ui.SUB);
        formula.setPadding(0, Ui.dp(c, 10), 0, 0);
        formula.setLineSpacing(Ui.dp(c, 3), 1f);
        card.addView(formula);
        return card;
    }

    private View dmiMetric(String label, String value, String unit) {
        LinearLayout box = Ui.vertical(c);
        box.addView(Ui.regular(c, label, 9.5f, Ui.SUB));
        LinearLayout row = Ui.horizontal(c);
        row.setGravity(Gravity.BOTTOM);
        row.addView(Ui.bold(c, value, 18, Ui.GREEN_DEEP));
        TextView u = Ui.regular(c, unit, 9, Ui.SUB);
        u.setPadding(Ui.dp(c, 3), 0, 0, Ui.dp(c, 2));
        row.addView(u);
        box.addView(row);
        return box;
    }

    private View healthCard() {
        Stats stats = baseStats;
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "数据健康度", 15, Ui.INK));
        title.addView(Ui.flexible(c));
        title.addView(Ui.chip(c, stats.anomalies.isEmpty() ? "良好" : stats.anomalies.size() + " 项待完善",
                stats.anomalies.isEmpty() ? Ui.GREEN_LIGHT : 0xFFFFF1E8,
                stats.anomalies.isEmpty() ? Ui.GREEN_DARK : 0xFFE06A10, 10));
        card.addView(title);
        card.addView(Ui.regular(c, "统计区间 " + stats.days + " 天 · " + stats.recordCount
                + " 条能耗记录 · " + stats.chargeCount + " 次充电 · " + stats.fuelCount + " 次加油", 10.5f, Ui.SUB));

        FrameLayout track = Ui.frame(c);
        track.setBackground(Ui.round(0xFFF1F4F2, Ui.dp(c, 5)));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 9));
        tlp.topMargin = Ui.dp(c, 10);
        card.addView(track, tlp);
        View fill = new View(c);
        fill.setBackground(Ui.gradient(new int[]{Ui.GREEN, Ui.GREEN_MID}, Ui.dp(c, 5)));
        int score = Math.max(10, 100 - stats.anomalies.size() * 9);
        android.widget.FrameLayout.LayoutParams flp = new android.widget.FrameLayout.LayoutParams(0, Ui.dp(c, 9));
        track.addView(fill, flp);
        track.post(() -> {
            android.widget.FrameLayout.LayoutParams p = (android.widget.FrameLayout.LayoutParams) fill.getLayoutParams();
            p.width = (int) (track.getWidth() * score / 100f);
            fill.setLayoutParams(p);
        });
        if (!stats.anomalies.isEmpty()) {
            TextView tip = Ui.regular(c, "· " + stats.anomalies.get(0), 9.5f, 0xFFE06A10);
            tip.setPadding(0, Ui.dp(c, 7), 0, 0);
            card.addView(tip);
        }
        return card;
    }

    private Map<String, Double> categoryTotals(Stats stats) {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("油费", stats.fuelCost);
        map.put("电费", stats.chargeCost);
        for (EnergyRecord r : repo.getRecords(vehicle.id)) {
            if (!r.isExpense()) continue;
            String key = r.category == null || r.category.isEmpty() ? "其他" : r.category;
            map.put(key, map.containsKey(key) ? map.get(key) + r.amount : r.amount);
        }
        return map;
    }

    private int color(String name) {
        if ("油费".equals(name)) return Ui.GREEN;
        if ("电费".equals(name)) return 0xFF22C7C7;
        if ("保险和交通规费".equals(name)) return 0xFF2196F3;
        if ("停车费".equals(name)) return 0xFFFFC107;
        if ("维修".equals(name)) return 0xFFFF6B62;
        if ("高速费".equals(name)) return 0xFF9C6BFF;
        if ("过路费".equals(name)) return 0xFF4D9EFF;
        return 0xFFB9C0C7;
    }
}
