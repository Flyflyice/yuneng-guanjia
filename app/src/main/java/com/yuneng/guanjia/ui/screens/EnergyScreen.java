package com.yuneng.guanjia.ui.screens;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.model.Stats;
import com.yuneng.guanjia.model.TimeRange;
import com.yuneng.guanjia.model.Vehicle;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.charts.LineChartView;
import com.yuneng.guanjia.ui.widgets.IconView;
import com.yuneng.guanjia.ui.widgets.PickerDialog;
import com.yuneng.guanjia.util.Formats;

import java.util.ArrayList;
import java.util.List;

/** 首页：最近能耗、累计统计、能耗趋势与电池衰减趋势。 */
public class EnergyScreen extends BaseScreen {
    /** 「最近综合能耗」与异常提醒始终基于全量数据。 */
    private Stats fullStats;
    /** 统计卡与能耗趋势卡共用时间窗口；电池容量趋势卡独立。 */
    private TimeRange statsRange = TimeRange.lastYear();
    private TimeRange batteryRange = TimeRange.lastYear();
    private TextView cityText;

    public EnergyScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected void build() {
        if (statsRange == null) statsRange = TimeRange.lastYear();
        if (batteryRange == null) batteryRange = TimeRange.lastYear();
        fullStats = repo.stats(vehicle);
        add(header());
        scrollBody(22);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setLinearMargins(scroll, 0, -18, 0, 0);

        body.setPadding(Ui.dp(c, 12), Ui.dp(c, 0), Ui.dp(c, 12), 0);
        body.addView(latestCard());
        body.addView(Ui.space(c, 10));
        body.addView(statsCard());
        body.addView(Ui.space(c, 10));
        body.addView(trendCard());
        body.addView(Ui.space(c, 10));
        body.addView(batteryCard());
        body.addView(Ui.space(c, 10));
        body.addView(reportCard());
        body.addView(Ui.space(c, 14));
    }

    private View header() {
        LinearLayout head = Ui.vertical(c);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Ui.GREEN, Ui.GREEN_MID, Ui.GREEN_DARK});
        head.setBackground(bg);
        head.setPadding(Ui.dp(c, 15), Ui.statusBarHeight(c) + Ui.dp(c, 8), Ui.dp(c, 15), Ui.dp(c, 30));

        LinearLayout top = Ui.horizontal(c);
        LinearLayout loc = Ui.horizontal(c);
        loc.addView(Ui.icon(c, IconView.LOCATION, 16, Color.WHITE));
        TextView city = Ui.medium(c, activity.locationName(), 13, Color.WHITE);
        city.setPadding(Ui.dp(c, 4), 0, 0, 0);
        cityText = city;
        loc.addView(city);
        loc.addView(Ui.icon(c, IconView.CHEVRON_RIGHT, 13, 0xCCFFFFFF));
        Ui.clickable(loc, v -> activity.requestLocation());
        top.addView(loc);
        top.addView(Ui.flexible(c));

        FrameLayout bellBox = Ui.frame(c);
        View bell = Ui.icon(c, IconView.BELL, 21, Color.WHITE);
        bellBox.addView(bell, new FrameLayout.LayoutParams(Ui.dp(c, 21), Ui.dp(c, 21), Gravity.CENTER));
        View red = new View(c);
        red.setBackground(Ui.circle(0xFFFF3B30, Ui.dp(c, 6)));
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(Ui.dp(c, 6), Ui.dp(c, 6));
        rlp.gravity = Gravity.TOP | Gravity.END;
        rlp.topMargin = Ui.dp(c, 1);
        if (fullStats.anomalies != null && !fullStats.anomalies.isEmpty()) {
            bellBox.addView(red, rlp);
        }
        top.addView(bellBox, Ui.lp(c, 34, 30));
        Ui.clickable(bellBox, v -> showAlerts());
        View book = Ui.icon(c, IconView.BOOK, 21, Color.WHITE);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(c, 34), Ui.dp(c, 30));
        blp.leftMargin = Ui.dp(c, 4);
        top.addView(book, blp);
        Ui.clickable(book, v -> activity.openRecordList());
        head.addView(top);

        LinearLayout vehicleRow = Ui.horizontal(c);
        vehicleRow.setPadding(0, Ui.dp(c, 14), 0, 0);
        TextView name = Ui.bold(c, vehicle.shortName(), 19, Color.WHITE);
        vehicleRow.addView(name);
        View drop = Ui.icon(c, IconView.CHEVRON_RIGHT, 18, Color.WHITE);
        drop.setRotation(90);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Ui.dp(c, 21), Ui.dp(c, 21));
        dlp.leftMargin = Ui.dp(c, 2);
        vehicleRow.addView(drop, dlp);
        head.addView(vehicleRow);
        Ui.clickable(vehicleRow, v -> activity.openVehicles());

        String specText = vehicle.specLine()
                + (Vehicle.TYPE_PHEV.equals(vehicle.type) ? " · 第五代 DM-i" : "");
        if (!specText.isEmpty()) {
            TextView spec = Ui.regular(c, specText, 11.5f, 0xE6FFFFFF);
            spec.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 14));
            head.addView(spec);
        } else {
            vehicleRow.setPadding(0, Ui.dp(c, 14), 0, Ui.dp(c, 14));
        }

        if (repo.hasUnbackedChanges()) {
            LinearLayout banner = Ui.horizontal(c);
            banner.setBackground(Ui.round(0xFFFFC400, Ui.dp(c, 10)));
            banner.setPadding(Ui.dp(c, 12), Ui.dp(c, 9), Ui.dp(c, 12), Ui.dp(c, 9));
            banner.addView(Ui.icon(c, IconView.BACKUP, 16, 0xFF6B4E00));
            TextView tip = Ui.medium(c, "数据有变化，点此备份到本地。", 11.5f, 0xFF5D4200);
            tip.setPadding(Ui.dp(c, 6), 0, 0, 0);
            banner.addView(tip);
            banner.addView(Ui.flexible(c));
            banner.addView(Ui.icon(c, IconView.CHEVRON_RIGHT, 14, 0xFF6B4E00));
            head.addView(banner);
            Ui.clickable(banner, v -> {
                String path = repo.backupDatabase();
                if (path == null) {
                    Ui.toast(c, "备份失败，请检查存储空间");
                    return;
                }
                // 备份成功后立即移除横幅；新增记录会再次触发显示
                head.removeView(banner);
                PickerDialog.info(c, "备份完成", "数据库已备份到：\n" + path);
            });
        }
        return head;
    }

    private void showAlerts() {
        if (fullStats.anomalies.isEmpty()) {
            PickerDialog.info(c, "数据体检", "太棒了，暂时没有发现异常记录。\n继续保持良好的记录习惯吧！");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String a : fullStats.anomalies) sb.append("· ").append(a).append("\n");
        PickerDialog.info(c, "发现 " + fullStats.anomalies.size() + " 条异常", sb.toString().trim());
    }

    private View latestCard() {
        LinearLayout card = whiteCard();
        LinearLayout top = Ui.horizontal(c);
        top.addView(Ui.bold(c, "最近综合能耗", 14, Ui.INK));
        top.addView(Ui.icon(c, IconView.CHEVRON_RIGHT, 14, Ui.MUTED));
        top.addView(Ui.flexible(c));
        boolean hasData = fullStats.recordCount > 0 && fullStats.latestCombined > 0;
        String level = !hasData ? "暂无数据" : fullStats.latestCombined <= 4 ? "优秀"
                : fullStats.latestCombined <= 6 ? "良好" : "偏高";
        TextView badge = Ui.chip(c, level, !hasData ? 0xFFF2F4F6 : fullStats.latestCombined <= 6 ? Ui.GREEN_LIGHT : 0xFFFFF1E8,
                !hasData ? Ui.SUB : fullStats.latestCombined <= 6 ? Ui.GREEN_DARK : 0xFFE06A10, 10.5f);
        top.addView(badge);
        card.addView(top);

        LinearLayout valueRow = Ui.horizontal(c);
        valueRow.setGravity(Gravity.BOTTOM);
        valueRow.setPadding(0, Ui.dp(c, 6), 0, 0);
        TextView value = Ui.black(c, Formats.num(fullStats.latestCombined, 2), 35, Ui.INK);
        valueRow.addView(value);
        TextView unit = Ui.regular(c, vehicle.isElectric() ? "度/百公里" : "升/百公里", 11.5f, Ui.SUB);
        unit.setPadding(Ui.dp(c, 5), 0, 0, Ui.dp(c, 6));
        valueRow.addView(unit);
        card.addView(valueRow);

        LinearLayout bottom = Ui.horizontal(c);
        TextView desc = Ui.regular(c, "最近一次能耗 · " + (fullStats.latestDistance > 0
                ? "本次行驶 " + Formats.km(fullStats.latestDistance) + " 公里" : "根据最近记录估算"), 11, Ui.SUB);
        bottom.addView(desc);
        bottom.addView(Ui.flexible(c));
        TextView cost = Ui.medium(c, Formats.price(fullStats.latestCostPerKm) + " 元/公里", 11.5f, Ui.GREEN_DARK);
        bottom.addView(cost);
        card.addView(bottom);
        Ui.clickable(card, v -> activity.openRecordList());
        return card;
    }

    private View statsCard() {
        Stats s = repo.stats(vehicle, statsRange);
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "统计", 15, Ui.INK));
        title.addView(Ui.flexible(c));
        title.addView(rangeLabel(statsRange, r -> statsRange = r));
        card.addView(title);
        card.addView(Ui.space(c, 12));

        LinearLayout row1 = Ui.horizontal(c);
        row1.addView(cell("综合能耗", Formats.num(s.combined, 2), "升/百公里", Ui.GREEN_DARK), weight());
        row1.addView(cell("平均油耗", Formats.num(s.avgFuel, 2), "升/百公里", Ui.INK), weight());
        row1.addView(cell("平均电耗", Formats.num(s.avgKwh, 2), "度/百公里", Ui.BLUE), weight());
        card.addView(row1);
        card.addView(Ui.space(c, 14));
        LinearLayout row2 = Ui.horizontal(c);
        row2.addView(cell("累计行程", Formats.num(s.distance, 0), "公里", Ui.INK), weight());
        row2.addView(cell("累计花费", Formats.num(s.energyCost, 2), "元", Ui.INK), weight());
        row2.addView(cell("平均能费", Formats.num(s.energyCostPerKm, 2), "元/公里", Ui.INK), weight());
        card.addView(row2);
        return card;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    }

    private View cell(String label, String value, String unit, int color) {
        LinearLayout box = Ui.vertical(c);
        box.setGravity(Gravity.CENTER);
        TextView l = Ui.regular(c, label, 10.5f, Ui.SUB);
        l.setGravity(Gravity.CENTER);
        box.addView(l);
        TextView v = Ui.bold(c, value, 16, color);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, Ui.dp(c, 4), 0, 0);
        box.addView(v);
        TextView u = Ui.regular(c, unit, 9.5f, Ui.MUTED);
        u.setGravity(Gravity.CENTER);
        box.addView(u);
        return box;
    }

    private View trendCard() {
        Stats s = repo.stats(vehicle, statsRange);
        LinearLayout card = whiteCard();
        String energyUnit = vehicle.isElectric() ? "度" : "升";
        LinearLayout title = Ui.horizontal(c);
        View bar = new View(c);
        bar.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 2)));
        title.addView(bar, new LinearLayout.LayoutParams(Ui.dp(c, 3), Ui.dp(c, 15)));
        title.addView(Ui.bold(c, " 能耗变化趋势", 14.5f, Ui.INK));
        title.addView(Ui.regular(c, "（" + energyUnit + "/100km）", 9.5f, Ui.SUB));
        title.addView(Ui.flexible(c));
        card.addView(title);

        LineChartView chart = new LineChartView(c);
        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        for (Stats.MonthPoint m : s.months) {
            labels.add(m.label);
            values.add((float) m.combined);
        }
        chart.setData(labels, values);
        chart.setUnit(energyUnit);
        chart.setDecimals(2);
        card.addView(chart, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 168)));
        chart.animateIn();
        return card;
    }

    private View batteryCard() {
        Stats s = repo.stats(vehicle, batteryRange);
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        View bar = new View(c);
        bar.setBackground(Ui.round(Ui.BLUE, Ui.dp(c, 2)));
        title.addView(bar, new LinearLayout.LayoutParams(Ui.dp(c, 3), Ui.dp(c, 15)));
        title.addView(Ui.bold(c, " 电池容量衰减趋势", 14.5f, Ui.INK));
        title.addView(Ui.regular(c, "（KWH）", 9.5f, Ui.SUB));
        title.addView(Ui.flexible(c));
        title.addView(rangeLabel(batteryRange, r -> batteryRange = r));
        card.addView(title);

        LineChartView chart = new LineChartView(c);
        List<String> labels = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        for (Stats.CapacityPoint p : s.capacities) {
            labels.add(Formats.dateSlash(p.ts).substring(5));
            values.add((float) p.kwh);
        }
        chart.setData(labels, values);
        chart.setLineColor(Ui.BLUE);
        chart.setUnit("kWh");
        chart.setDecimals(2);
        card.addView(chart, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 156)));
        chart.animateIn();

        LinearLayout foot = Ui.horizontal(c);
        foot.setPadding(0, Ui.dp(c, 4), 0, 0);
        foot.addView(Ui.regular(c, "当前估算容量 ", 10.5f, Ui.SUB));
        foot.addView(Ui.bold(c, Formats.num(s.batteryCapacity, 2) + " kWh", 11, Ui.INK));
        foot.addView(Ui.flexible(c));
        TextView health = Ui.chip(c, "健康度 " + Formats.num(s.batteryHealth, 1) + "%",
                s.batteryHealth >= 90 ? Ui.GREEN_LIGHT : 0xFFFFF1E8,
                s.batteryHealth >= 90 ? Ui.GREEN_DARK : 0xFFE06A10, 10);
        foot.addView(health);
        card.addView(foot);
        return card;
    }

    /** 定位结果回到首页时刷新左上角城市。 */
    public void setLocation(String name) {
        if (cityText != null && name != null && !name.trim().isEmpty()) {
            cityText.setText(name.trim());
        }
    }

    private View reportCard() {
        LinearLayout card = whiteCard();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = Ui.vertical(c);
        left.addView(Ui.bold(c, "更多详细统计报告", 15, Ui.INK));
        left.addView(Ui.regular(c, "行程 / 能耗 / 费用 / 电池健康", 10.5f, Ui.SUB));
        TextView btn = Ui.medium(c, "点击查看  →", 11.5f, Color.WHITE);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(Ui.dp(c, 14), Ui.dp(c, 7), Ui.dp(c, 14), Ui.dp(c, 7));
        btn.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 9)));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(c, 9);
        left.addView(btn, blp);
        card.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        IconView chartIcon = Ui.icon(c, IconView.CHART, 44, 0x2200C853);
        card.addView(chartIcon);
        Ui.clickable(btn, v -> activity.openTrip());
        Ui.clickable(card, v -> activity.openTrip());
        return card;
    }
}
