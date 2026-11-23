package com.yuneng.guanjia.ui.screens;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.model.Stats;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.charts.LineChartView;
import com.yuneng.guanjia.ui.widgets.AppBar;
import com.yuneng.guanjia.ui.widgets.IconView;
import com.yuneng.guanjia.ui.widgets.PickerDialog;
import com.yuneng.guanjia.util.Formats;

import java.util.ArrayList;
import java.util.List;

/** 行程报告：周期行驶、能耗、费用与第五代 DM-i 效率折算。 */
public class TripScreen extends BaseScreen {
    private Stats stats;

    public TripScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected void build() {
        stats = repo.stats(vehicle);
        AppBar bar = new AppBar(c);
        bar.title("行程报告").background(Ui.GREEN).dark(false);
        bar.back(v -> activity.pop());
        bar.actionIcon(IconView.SHARE, Color.WHITE, v -> share());
        add(bar);

        scrollBody(24);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        body.setPadding(Ui.dp(c, 12), Ui.dp(c, 0), Ui.dp(c, 12), 0);
        body.addView(hero());
        body.addView(Ui.space(c, 10));
        body.addView(overviewCard());
        body.addView(Ui.space(c, 10));
        body.addView(energyCard());
        body.addView(Ui.space(c, 10));
        if (vehicle.isHybrid()) {
            body.addView(dmiCard());
            body.addView(Ui.space(c, 10));
        }
        body.addView(trendCard());
        body.addView(Ui.space(c, 10));
        body.addView(adviceCard());
        body.addView(Ui.space(c, 14));
    }

    private View hero() {
        LinearLayout card = Ui.vertical(c);
        card.setBackground(Ui.gradient(new int[]{Ui.GREEN, Ui.GREEN_DARK}, Ui.dp(c, 20)));
        card.setPadding(Ui.dp(c, 18), Ui.dp(c, 16), Ui.dp(c, 18), Ui.dp(c, 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(c, 12);
        card.setLayoutParams(lp);

        LinearLayout badge = Ui.horizontal(c);
        badge.setBackground(Ui.round(0x33FFFFFF, Ui.dp(c, 10)));
        badge.setPadding(Ui.dp(c, 9), Ui.dp(c, 5), Ui.dp(c, 9), Ui.dp(c, 5));
        badge.addView(Ui.icon(c, IconView.CHART, 13, Color.WHITE));
        badge.addView(Ui.regular(c, " 已持续记录 " + stats.days + " 天 · " + stats.recordCount + " 条", 10.5f, Color.WHITE));
        LinearLayout badgeRow = Ui.horizontal(c);
        badgeRow.addView(badge);
        badgeRow.addView(Ui.flexible(c));
        badgeRow.addView(Ui.icon(c, IconView.STAR, 20, 0xFFFFD54F));
        card.addView(badgeRow);

        card.addView(Ui.regular(c, vehicle.shortName(), 12.5f, 0xE6FFFFFF));
        String title = stats.recordCount == 0 ? "还没有记录"
                : stats.anomalies.isEmpty() ? "一切正常，继续保持！" : "出现异常啦！";
        TextView big = Ui.black(c, title, 25, Color.WHITE);
        big.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 10));
        card.addView(big);

        LinearLayout row = Ui.horizontal(c);
        row.addView(heroMetric("最近能耗", Formats.num(stats.latestCombined, 2),
                vehicle.isElectric() ? "度/百公里" : "升/百公里"), weight());
        row.addView(heroMetric("最近能费", Formats.num(stats.latestCostPerKm, 3), "元/公里"), weight());
        card.addView(row);

        if (stats.recordCount == 0) {
            TextView tip = Ui.regular(c, "· 记录一次充电或加油，就能生成行程报告。", 9.5f, 0xFFFFF3CD);
            tip.setPadding(0, Ui.dp(c, 9), 0, 0);
            card.addView(tip);
        } else if (!stats.anomalies.isEmpty()) {
            TextView tip = Ui.regular(c, "· " + stats.anomalies.get(0), 9.5f, 0xFFFFF3CD);
            tip.setPadding(0, Ui.dp(c, 9), 0, 0);
            card.addView(tip);
        }
        return card;
    }

    private View heroMetric(String label, String value, String unit) {
        LinearLayout box = Ui.vertical(c);
        box.addView(Ui.regular(c, label, 9.5f, 0xCCFFFFFF));
        LinearLayout row = Ui.horizontal(c);
        row.setGravity(Gravity.BOTTOM);
        row.addView(Ui.bold(c, value, 22, Color.WHITE));
        TextView u = Ui.regular(c, unit, 8.5f, 0xCCFFFFFF);
        u.setPadding(Ui.dp(c, 3), 0, 0, Ui.dp(c, 3));
        row.addView(u);
        box.addView(row);
        return box;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    }

    private View overviewCard() {
        LinearLayout card = whiteCard();
        card.addView(cardTitle("行驶概览", "本周期"));
        LinearLayout row1 = Ui.horizontal(c);
        row1.addView(metric("行驶天数", String.valueOf(stats.activeDays), "天", Ui.INK), weight());
        row1.addView(metric("行驶距离", Formats.num(stats.distance, 0), "公里", Ui.INK), weight());
        row1.addView(metric("日均行程", Formats.num(stats.distance / Math.max(1, stats.activeDays), 1), "公里", Ui.GREEN_DARK), weight());
        card.addView(row1);
        card.addView(Ui.space(c, 12));
        LinearLayout row2 = Ui.horizontal(c);
        row2.addView(metric("充电次数", String.valueOf(stats.chargeCount), "次", Ui.INK), weight());
        row2.addView(metric("加油次数", String.valueOf(stats.fuelCount), "次", Ui.INK), weight());
        row2.addView(metric("统计周期", String.valueOf(stats.days), "天", Ui.INK), weight());
        card.addView(row2);
        return card;
    }

    private View energyCard() {
        LinearLayout card = whiteCard();
        card.addView(cardTitle("能耗与花费", "累计"));
        if (!vehicle.isElectric()) {
            LinearLayout row = Ui.horizontal(c);
            row.addView(metric("燃油消耗", Formats.num(stats.totalFuel, 2), "升", 0xFFE06A10), weight());
            row.addView(metric("油费支出", Formats.num(stats.fuelCost, 2), "元", Ui.INK), weight());
            row.addView(metric("每公里油费", Formats.num(stats.distance > 0 ? stats.fuelCost / stats.distance : 0, 3), "元", Ui.INK), weight());
            card.addView(row);
            if (!vehicle.isFuelOnly()) card.addView(Ui.space(c, 12));
        }
        if (!vehicle.isFuelOnly()) {
            LinearLayout row2 = Ui.horizontal(c);
            row2.addView(metric("电量消耗", Formats.num(stats.totalKwh, 2), "度", Ui.BLUE), weight());
            row2.addView(metric("电费支出", Formats.num(stats.chargeCost, 2), "元", Ui.INK), weight());
            row2.addView(metric("每公里电费", Formats.num(stats.distance > 0 ? stats.chargeCost / stats.distance : 0, 3), "元", Ui.INK), weight());
            card.addView(row2);
        }

        LinearLayout foot = Ui.horizontal(c);
        foot.setPadding(0, Ui.dp(c, 12), 0, 0);
        if (!vehicle.isElectric()) {
            foot.addView(Ui.regular(c, "平均油耗 ", 10, Ui.SUB));
            foot.addView(Ui.medium(c, Formats.num(stats.avgFuel, 2) + " 升/百公里", 10, 0xFFE06A10));
        }
        if (!vehicle.isElectric() && !vehicle.isFuelOnly()) foot.addView(Ui.flexible(c));
        if (!vehicle.isFuelOnly()) {
            foot.addView(Ui.regular(c, "平均电耗 ", 10, Ui.SUB));
            foot.addView(Ui.medium(c, Formats.num(stats.avgKwh, 2) + " 度/百公里", 10, Ui.BLUE));
        }
        card.addView(foot);
        return card;
    }

    private View dmiCard() {
        LinearLayout card = whiteCard();
        card.setBackground(Ui.gradient(new int[]{Ui.GREEN_TINT, Ui.GREEN_LIGHT}, Ui.dp(c, 18)));
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, vehicle.energyLabel() + " 效率", 14.5f, Ui.GREEN_DEEP));
        title.addView(Ui.flexible(c));
        title.addView(Ui.icon(c, IconView.LEAF, 20, Ui.GREEN));
        card.addView(title);

        TextView sub = Ui.regular(c, "发动机热效率 " + Formats.num(stats.thermalEfficiency * 100, 2)
                + "% · 电驱效率 " + Formats.num(stats.driveEfficiency * 100, 0) + "%", 10, Ui.SUB);
        sub.setPadding(0, Ui.dp(c, 3), 0, Ui.dp(c, 8));
        card.addView(sub);

        LinearLayout row = Ui.horizontal(c);
        row.addView(dmiMetric("每升油发电", Formats.num(stats.dmKwhPerL, 2), "kWh"), weight());
        row.addView(dmiMetric("等效油耗", Formats.num(stats.dmEquivalent, 2), "升/百公里"), weight());
        row.addView(dmiMetric("综合能耗", Formats.num(stats.combined, 2), "升/百公里"), weight());
        card.addView(row);

        TextView formula = Ui.regular(c, "折算公式：等效油耗 = 平均油耗 + 平均电耗 ÷（8.9 kWh/L × 热效率 × 电驱效率）", 9.5f, Ui.SUB);
        formula.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 2));
        card.addView(formula);
        return card;
    }

    private View dmiMetric(String label, String value, String unit) {
        LinearLayout box = Ui.vertical(c);
        box.addView(Ui.regular(c, label, 9.5f, Ui.SUB));
        box.addView(Ui.bold(c, value, 17, Ui.GREEN_DEEP));
        box.addView(Ui.regular(c, unit, 8.5f, Ui.MUTED));
        return box;
    }

    private View trendCard() {
        LinearLayout card = whiteCard();
        String unit = vehicle.isElectric() ? "度" : "升";
        card.addView(cardTitle("能耗变化趋势", "近 12 个月 · " + unit + "/百公里"));
        LineChartView chart = new LineChartView(c);
        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        for (Stats.MonthPoint m : stats.months) {
            labels.add(m.label);
            values.add((float) m.combined);
        }
        chart.setData(labels, values);
        chart.setUnit(unit);
        chart.setDecimals(2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 180));
        lp.topMargin = Ui.dp(c, 6);
        card.addView(chart, lp);
        chart.animateIn();
        return card;
    }

    private View adviceCard() {
        LinearLayout card = whiteCard();
        card.addView(cardTitle("用车建议", ""));
        List<String> tips = new ArrayList<>();
        if (stats.avgKwh > 16) tips.add("平均电耗偏高，建议减少急加速与高速巡航，保持经济时速。");
        else if (stats.avgKwh > 0) tips.add("电耗表现良好，继续保持平稳驾驶与合理胎压。");
        if (stats.avgFuel > 5 && vehicle.isHybrid()) tips.add("亏电油耗偏高，建议及时充电，让 DM-i 更多运行在电驱区间。");
        if (stats.batteryHealth < 90) tips.add("电池估算健康度低于 90%，建议定期做满充满放校准 BMS。");
        if (stats.chargeCount > 0) tips.add("当前平均充电单价 " + Formats.num(stats.avgChargePrice, 2) + " 元/度，优先使用谷电更省钱。");
        if (tips.isEmpty()) tips.add("记录越完整，能耗分析越准确。坚持每次充电、加油后记一笔。");
        for (String s : tips) {
            LinearLayout row = Ui.horizontal(c);
            row.setPadding(0, Ui.dp(c, 5), 0, Ui.dp(c, 5));
            View dot = new View(c);
            dot.setBackground(Ui.circle(Ui.GREEN, Ui.dp(c, 6)));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(c, 6), Ui.dp(c, 6));
            dlp.topMargin = Ui.dp(c, 5);
            dlp.rightMargin = Ui.dp(c, 7);
            row.addView(dot, dlp);
            TextView t = Ui.regular(c, s, 10.5f, Ui.INK_2);
            t.setLineSpacing(Ui.dp(c, 3), 1f);
            row.addView(t, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            card.addView(row);
        }
        return card;
    }

    private View cardTitle(String title, String right) {
        LinearLayout row = Ui.horizontal(c);
        View bar = new View(c);
        bar.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 2)));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(c, 3), Ui.dp(c, 14));
        blp.rightMargin = Ui.dp(c, 6);
        row.addView(bar, blp);
        row.addView(Ui.bold(c, title, 14.5f, Ui.INK));
        row.addView(Ui.flexible(c));
        if (right != null && !right.isEmpty()) row.addView(Ui.regular(c, right, 9.5f, Ui.MUTED));
        return row;
    }

    private View metric(String label, String value, String unit, int color) {
        LinearLayout box = Ui.vertical(c);
        box.addView(Ui.regular(c, label, 9.5f, Ui.SUB));
        TextView v = Ui.bold(c, value, 17, color);
        v.setPadding(0, Ui.dp(c, 1), 0, 0);
        box.addView(v);
        box.addView(Ui.regular(c, unit, 8.5f, Ui.MUTED));
        return box;
    }

    private void share() {
        StringBuilder text = new StringBuilder("【驭能管家 · 行程报告】\n")
                .append(vehicle.shortName()).append("\n")
                .append("行驶 ").append(Formats.num(stats.distance, 0)).append(" 公里 · ")
                .append(stats.days).append(" 天\n");
        if (!vehicle.isElectric()) {
            text.append("平均油耗 ").append(Formats.num(stats.avgFuel, 2)).append(" 升/百公里\n");
        }
        if (!vehicle.isFuelOnly()) {
            text.append("平均电耗 ").append(Formats.num(stats.avgKwh, 2)).append(" 度/百公里\n");
        }
        text.append("综合能耗 ").append(Formats.num(stats.combined, 2))
                .append(vehicle.isElectric() ? " 度/百公里\n" : " 升/百公里\n")
                .append("累计花费 ").append(Formats.num(stats.totalCost, 2)).append(" 元\n");
        if (vehicle.isHybrid()) {
            text.append(vehicle.energyLabel()).append(" 等效油耗 ").append(Formats.num(stats.dmEquivalent, 2)).append(" 升/百公里");
        }
        PickerDialog.info(c, "行程报告", text.toString().trim());
    }
}
