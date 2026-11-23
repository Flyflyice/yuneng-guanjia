package com.yuneng.guanjia.ui.screens;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.model.EnergyRecord;
import com.yuneng.guanjia.model.TripSlice;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.widgets.AppBar;
import com.yuneng.guanjia.ui.widgets.IconView;
import com.yuneng.guanjia.ui.widgets.PickerDialog;
import com.yuneng.guanjia.util.Formats;

/** 单次补能的行程报告：本段距离、能耗与花费。 */
public class TripDetailScreen extends BaseScreen {
    private EnergyRecord record;
    private TripSlice trip;

    public TripDetailScreen(MainActivity activity, long recordId) {
        super(activity, recordId);
    }

    @Override
    protected void build() {
        record = repo.getRecord(recordId);
        trip = repo.tripSlice(record);
        AppBar bar = new AppBar(c);
        bar.title("行程报告").background(Ui.GREEN).dark(false);
        bar.back(v -> activity.pop());
        bar.actionIcon(IconView.SHARE, Color.WHITE, v -> share());
        add(bar);

        scrollBody(24);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        body.setPadding(Ui.dp(c, 12), 0, Ui.dp(c, 12), 0);

        if (record == null) {
            LinearLayout empty = whiteCard();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(c, 40), 0, Ui.dp(c, 40));
            empty.addView(Ui.icon(c, IconView.RECEIPT, 36, Ui.MUTED));
            TextView t = Ui.regular(c, "记录不存在或已被删除", 12, Ui.MUTED);
            t.setPadding(0, Ui.dp(c, 10), 0, 0);
            empty.addView(t);
            body.addView(empty);
            return;
        }

        body.addView(hero());
        body.addView(Ui.space(c, 10));
        body.addView(overviewCard());
        body.addView(Ui.space(c, 10));
        body.addView(energyCard());
        body.addView(Ui.space(c, 10));
        body.addView(compareCard());
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
        badge.addView(Ui.icon(c, IconView.BOLT, 13, Color.WHITE));
        badge.addView(Ui.regular(c, " 本次补能 · " + Formats.dateTime(record.timestamp), 10.5f, Color.WHITE));
        LinearLayout badgeRow = Ui.horizontal(c);
        badgeRow.addView(badge);
        badgeRow.addView(Ui.flexible(c));
        badgeRow.addView(Ui.icon(c, IconView.STAR, 20, 0xFFFFD54F));
        card.addView(badgeRow);

        card.addView(Ui.regular(c, vehicle.shortName(), 12.5f, 0xE6FFFFFF));
        String title = trip.hasDistance() ? "本次行程" : "里程数据不足";
        TextView big = Ui.black(c, title, 25, Color.WHITE);
        big.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 10));
        card.addView(big);

        LinearLayout row = Ui.horizontal(c);
        row.addView(heroMetric("本次能耗", energyText(), energyUnit()), weight());
        row.addView(heroMetric("本次能费", trip.hasDistance() ? Formats.num(trip.costPerKm, 3) : "--", "元/公里"), weight());
        card.addView(row);

        if (!trip.hasDistance()) {
            TextView tip = Ui.regular(c, "· 需要上一条带总里程的能耗记录，才能算出本次行程的能耗与每公里花费。", 9.5f, 0xFFFFF3CD);
            tip.setPadding(0, Ui.dp(c, 9), 0, 0);
            card.addView(tip);
        }
        return card;
    }

    private String energyText() {
        if (!trip.hasDistance()) return "--";
        return Formats.num(record.isCharge() ? trip.kwhPer100 : trip.lPer100, 2);
    }

    private String energyUnit() {
        return record.isCharge() ? "度/百公里" : "升/百公里";
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
        card.addView(cardTitle("行驶概览", "本次行程"));
        LinearLayout row = Ui.horizontal(c);
        row.addView(metric("行驶天数", String.valueOf(trip.days), "天", Ui.INK), weight());
        row.addView(metric("行驶距离", trip.hasDistance() ? Formats.num(trip.distance, 0) : "--", "公里", Ui.INK), weight());
        row.addView(metric("日均行程", trip.hasDistance() ? Formats.num(trip.dailyDistance(), 1) : "--", "公里", Ui.GREEN_DARK), weight());
        card.addView(row);
        return card;
    }

    private View energyCard() {
        LinearLayout card = whiteCard();
        card.addView(cardTitle("能耗与花费", "本次"));
        if (!vehicle.isElectric()) {
            LinearLayout row = Ui.horizontal(c);
            row.addView(metric("燃油消耗", Formats.num(trip.fuel, 2), "升", 0xFFE06A10), weight());
            row.addView(metric("油费支出", record.isFuel() ? Formats.num(trip.cost, 2) : "0.00", "元", Ui.INK), weight());
            row.addView(metric("每公里油费", record.isFuel() && trip.hasDistance() ? Formats.num(trip.costPerKm, 3) : "--", "元", Ui.INK), weight());
            card.addView(row);
            if (!vehicle.isFuelOnly()) card.addView(Ui.space(c, 12));
        }
        if (!vehicle.isFuelOnly()) {
            LinearLayout row2 = Ui.horizontal(c);
            row2.addView(metric("电量消耗", Formats.num(trip.kwh, 2), "度", Ui.BLUE), weight());
            row2.addView(metric("电费支出", record.isCharge() ? Formats.num(trip.cost, 2) : "0.00", "元", Ui.INK), weight());
            row2.addView(metric("每公里电费", record.isCharge() && trip.hasDistance() ? Formats.num(trip.costPerKm, 3) : "--", "元", Ui.INK), weight());
            card.addView(row2);
        }
        LinearLayout foot = Ui.horizontal(c);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, Ui.dp(c, 12), 0, 0);
        foot.addView(Ui.regular(c, record.isCharge() ? "本次电耗 " : "本次油耗 ", 10, Ui.SUB));
        foot.addView(Ui.medium(c, energyText() + " " + energyUnit(), 10,
                record.isCharge() ? Ui.BLUE : 0xFFE06A10));
        card.addView(foot);
        return card;
    }

    private View compareCard() {
        LinearLayout card = whiteCard();
        card.addView(cardTitle("对比上一条", ""));
        if (trip.previous == null) {
            TextView t = Ui.regular(c, "这是最早的一条能耗记录，没有可对比的上一条。", 10.5f, Ui.SUB);
            t.setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 2));
            card.addView(t);
            return card;
        }
        LinearLayout row = Ui.horizontal(c);
        row.addView(metric("上一条时间", Formats.monthDay(trip.previous.timestamp), "", Ui.INK), weight());
        row.addView(metric("上一条里程", Formats.num(trip.previous.odometer, 0), "公里", Ui.INK), weight());
        row.addView(metric("本条里程", Formats.num(record.odometer, 0), "公里", Ui.INK), weight());
        card.addView(row);
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
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView l = Ui.regular(c, label, 9.5f, Ui.SUB);
        l.setGravity(Gravity.CENTER);
        l.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        box.addView(l);
        TextView v = Ui.bold(c, value, 17, color);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, Ui.dp(c, 1), 0, 0);
        v.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        box.addView(v);
        if (unit != null && !unit.isEmpty()) {
            TextView u = Ui.regular(c, unit, 8.5f, Ui.MUTED);
            u.setGravity(Gravity.CENTER);
            u.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            box.addView(u);
        }
        return box;
    }

    private void share() {
        if (record == null) return;
        StringBuilder text = new StringBuilder("【驭能管家 · 行程报告】\n")
                .append(vehicle.shortName()).append("\n")
                .append(Formats.dateTime(record.timestamp)).append("\n")
                .append("本段行驶 ").append(Formats.num(trip.distance, 0)).append(" 公里 · ")
                .append(trip.days).append(" 天\n");
        if (trip.hasDistance()) {
            text.append("本次").append(record.isCharge() ? "电耗 " : "油耗 ")
                    .append(Formats.num(record.isCharge() ? trip.kwhPer100 : trip.lPer100, 2))
                    .append(record.isCharge() ? " 度/百公里\n" : " 升/百公里\n")
                    .append("本次能费 ").append(Formats.num(trip.costPerKm, 3)).append(" 元/公里\n");
        }
        text.append("本次花费 ").append(Formats.num(trip.cost, 2)).append(" 元");
        PickerDialog.info(c, "行程报告", text.toString().trim());
    }
}
