package com.yuneng.guanjia.ui.screens;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.model.EnergyRecord;
import com.yuneng.guanjia.model.Stats;
import com.yuneng.guanjia.model.TripSlice;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.widgets.AppBar;
import com.yuneng.guanjia.ui.widgets.IconView;
import com.yuneng.guanjia.ui.widgets.PickerDialog;
import com.yuneng.guanjia.util.Formats;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** 加油 / 充电 / 收支流水，按时间倒序展示，点击可修改。 */
public class RecordListScreen extends BaseScreen {
    private static final int FILTER_ALL = 0;
    private static final int FILTER_CHARGE = 1;
    private static final int FILTER_FUEL = 2;
    private static final int FILTER_MONEY = 3;

    private int filter = FILTER_ALL;
    private Stats stats;

    public RecordListScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected void build() {
        stats = repo.stats(vehicle);
        AppBar bar = new AppBar(c);
        bar.title("加油充电记录").dark(true).background(Color.WHITE).divider(true);
        bar.back(v -> activity.pop());
        bar.actionIcon(IconView.PLUS, Ui.GREEN_DARK, v -> chooseAdd());
        add(bar);

        scrollBody(24);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        body.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), 0);

        body.addView(summaryCard());
        body.addView(Ui.space(c, 10));
        body.addView(filterBar());

        List<EnergyRecord> all = repo.getRecords(vehicle.id);
        List<EnergyRecord> list = new ArrayList<>();
        for (EnergyRecord r : all) {
            if (filter == FILTER_CHARGE && !r.isCharge()) continue;
            if (filter == FILTER_FUEL && !r.isFuel()) continue;
            if (filter == FILTER_MONEY && !(r.isIncome() || r.isExpense())) continue;
            list.add(r);
        }

        if (list.isEmpty()) {
            LinearLayout empty = whiteCard();
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(c, 36), 0, Ui.dp(c, 36));
            empty.addView(Ui.icon(c, IconView.RECEIPT, 40, Ui.MUTED));
            TextView t = Ui.regular(c, "还没有记录，点右上角 + 添加一笔", 12, Ui.MUTED);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(c, 8), 0, 0);
            empty.addView(t, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            body.addView(empty);
        } else {
            String lastMonth = "";
            SimpleDateFormat month = new SimpleDateFormat("yyyy年M月", Locale.CHINA);
            for (EnergyRecord r : list) {
                String m = month.format(new Date(r.timestamp));
                if (!m.equals(lastMonth)) {
                    body.addView(monthHeader(m, list, r));
                    lastMonth = m;
                }
                body.addView(recordCard(r));
                body.addView(Ui.space(c, 8));
            }
        }
        body.addView(Ui.space(c, 12));
    }

    private void chooseAdd() {
        PickerDialog.choose(c, "记一笔", new String[]{"充电", "加油", "支出", "收入"}, 0, (index, value) -> {
            switch (index) {
                case 0:
                    activity.openForm(EnergyRecord.TYPE_CHARGE, null);
                    break;
                case 1:
                    activity.openForm(EnergyRecord.TYPE_FUEL, null);
                    break;
                case 2:
                    activity.openForm(EnergyRecord.TYPE_EXPENSE, null);
                    break;
                default:
                    activity.openForm(EnergyRecord.TYPE_INCOME, null);
                    break;
            }
        });
    }

    private View summaryCard() {
        LinearLayout card = whiteCard();
        card.setBackground(Ui.gradient(new int[]{Ui.GREEN, Ui.GREEN_DARK}, Ui.dp(c, 18)));
        LinearLayout top = Ui.horizontal(c);
        top.addView(Ui.regular(c, "全部记录", 13, Color.WHITE));
        top.addView(Ui.flexible(c));
        top.addView(Ui.icon(c, IconView.CHEVRON_RIGHT, 16, 0xCCFFFFFF));
        card.addView(top);
        LinearLayout valueRow = Ui.horizontal(c);
        valueRow.setGravity(Gravity.BOTTOM);
        valueRow.setPadding(0, Ui.dp(c, 2), 0, 0);
        valueRow.addView(Ui.black(c, Formats.num(stats.energyCost, 2), 31, Color.WHITE));
        TextView unit = Ui.regular(c, "元", 12, 0xD9FFFFFF);
        unit.setPadding(Ui.dp(c, 5), 0, 0, Ui.dp(c, 6));
        valueRow.addView(unit);
        card.addView(valueRow);

        LinearLayout row = Ui.horizontal(c);
        row.setPadding(0, Ui.dp(c, 12), 0, 0);
        row.addView(mini("累计里程", Formats.num(stats.distance, 0), "公里"), weight());
        row.addView(mini("综合能耗", Formats.num(stats.combined, 2), "升/百公里"), weight());
        row.addView(mini("记录条数", String.valueOf(stats.recordCount), "条"), weight());
        card.addView(row);
        return card;
    }

    private View mini(String label, String value, String unit) {
        LinearLayout box = Ui.vertical(c);
        box.addView(Ui.regular(c, label, 10, 0xCCFFFFFF));
        LinearLayout row = Ui.horizontal(c);
        row.setGravity(Gravity.BOTTOM);
        row.addView(Ui.bold(c, value, 15, Color.WHITE));
        TextView u = Ui.regular(c, unit, 8.5f, 0xCCFFFFFF);
        u.setPadding(Ui.dp(c, 2), 0, 0, Ui.dp(c, 2));
        row.addView(u);
        box.addView(row);
        return box;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    }

    private View filterBar() {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = Ui.horizontal(c);
        row.setPadding(0, Ui.dp(c, 2), 0, Ui.dp(c, 4));
        addFilter(row, "全部", FILTER_ALL);
        addFilter(row, "充电", FILTER_CHARGE);
        addFilter(row, "加油", FILTER_FUEL);
        addFilter(row, "收支", FILTER_MONEY);
        hs.addView(row);
        return hs;
    }

    private void addFilter(LinearLayout row, String text, int value) {
        boolean on = filter == value;
        TextView chip = Ui.medium(c, text, 11.5f, on ? Color.WHITE : Ui.SUB);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(Ui.dp(c, 16), Ui.dp(c, 7), Ui.dp(c, 16), Ui.dp(c, 7));
        chip.setBackground(on ? Ui.round(Ui.GREEN, Ui.dp(c, 16)) : Ui.round(Color.WHITE, Ui.dp(c, 16)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(c, 8);
        row.addView(chip, lp);
        Ui.clickable(chip, v -> {
            if (filter == value) return;
            filter = value;
            refresh();
        });
    }

    private View monthHeader(String month, List<EnergyRecord> list, EnergyRecord first) {
        LinearLayout row = Ui.horizontal(c);
        row.setPadding(Ui.dp(c, 4), Ui.dp(c, 12), Ui.dp(c, 4), Ui.dp(c, 6));
        row.addView(Ui.bold(c, month, 13, Ui.INK_2));
        row.addView(Ui.flexible(c));
        int count = 0;
        double amount = 0;
        for (EnergyRecord r : list) {
            if (new SimpleDateFormat("yyyy年M月", Locale.CHINA).format(new Date(r.timestamp)).equals(month)) {
                count++;
                if (r.isEnergy()) amount += r.amount;
            }
        }
        row.addView(Ui.regular(c, count + " 条 · 能费 " + Formats.num(amount, 2) + " 元", 9.5f, Ui.MUTED));
        return row;
    }

    private View recordCard(EnergyRecord r) {
        LinearLayout card = whiteCard();
        card.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));

        LinearLayout top = Ui.horizontal(c);
        TextView date = Ui.bold(c, Formats.monthDay(r.timestamp), 15, Ui.INK);
        top.addView(date);
        TextView time = Ui.regular(c, Formats.time(r.timestamp), 9.5f, Ui.MUTED);
        time.setPadding(Ui.dp(c, 5), Ui.dp(c, 3), 0, 0);
        top.addView(time);
        TextView tag = Ui.chip(c, r.typeLabel(), typeBg(r), typeColor(r), 9.5f);
        LinearLayout.LayoutParams tagLp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        tagLp.leftMargin = Ui.dp(c, 7);
        top.addView(tag, tagLp);
        top.addView(Ui.flexible(c));
        String sign = r.isExpense() ? "-" : r.isIncome() ? "+" : "";
        top.addView(Ui.bold(c, sign + Formats.num(r.amount, 2), 18, r.isIncome() ? Ui.GREEN_DARK : Ui.INK));
        TextView yuan = Ui.regular(c, "元", 9.5f, Ui.SUB);
        yuan.setPadding(Ui.dp(c, 2), 0, 0, Ui.dp(c, 2));
        top.addView(yuan);
        card.addView(top);

        LinearLayout mid = Ui.horizontal(c);
        mid.setPadding(0, Ui.dp(c, 8), 0, 0);
        View iconBg = new View(c);
        iconBg.setBackground(Ui.circle(typeBg(r), Ui.dp(c, 30)));
        FrameLayout iconBox = Ui.frame(c);
        iconBox.addView(iconBg, new LinearLayout.LayoutParams(Ui.dp(c, 30), Ui.dp(c, 30)));
        iconBox.addView(Ui.icon(c, typeIcon(r), 17, typeColor(r)),
                new android.widget.FrameLayout.LayoutParams(Ui.dp(c, 17), Ui.dp(c, 17), Gravity.CENTER));
        mid.addView(iconBox, new LinearLayout.LayoutParams(Ui.dp(c, 30), Ui.dp(c, 30)));

        LinearLayout texts = Ui.vertical(c);
        texts.setPadding(Ui.dp(c, 9), 0, 0, 0);
        texts.addView(Ui.medium(c, title(r), 12, Ui.INK));
        texts.addView(Ui.regular(c, subtitle(r), 9.5f, Ui.SUB));
        mid.addView(texts, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        View edit = Ui.icon(c, IconView.EDIT, 16, Ui.MUTED);
        mid.addView(edit);
        card.addView(mid);

        if (r.isCharge() && r.socEnd > 0) {
            LinearLayout soc = Ui.horizontal(c);
            soc.setPadding(0, Ui.dp(c, 9), 0, 0);
            soc.addView(Ui.regular(c, "电量 ", 9.5f, Ui.SUB));
            soc.addView(Ui.medium(c, Formats.num(r.socStart, 0) + "% → " + Formats.num(r.socEnd, 0) + "%",
                    10, Ui.GREEN_DARK));
            if (r.remainingOil > 0) {
                soc.addView(Ui.regular(c, "   剩余油量 ", 9.5f, Ui.SUB));
                soc.addView(Ui.medium(c, Formats.num(r.remainingOil, 0) + "%", 10, Ui.ORANGE));
            }
            card.addView(soc);
        } else if (r.isFuel() && r.oilAfter > 0) {
            LinearLayout soc = Ui.horizontal(c);
            soc.setPadding(0, Ui.dp(c, 9), 0, 0);
            soc.addView(Ui.regular(c, "加油前 ", 9.5f, Ui.SUB));
            soc.addView(Ui.medium(c, Formats.num(r.oilBefore, 0) + "%", 10, Ui.INK_2));
            soc.addView(Ui.regular(c, "  加油后 ", 9.5f, Ui.SUB));
            soc.addView(Ui.medium(c, Formats.num(r.oilAfter, 0) + "%", 10, Ui.ORANGE));
            if (r.remainingSoc > 0) {
                soc.addView(Ui.regular(c, "   剩余电量 ", 9.5f, Ui.SUB));
                soc.addView(Ui.medium(c, Formats.num(r.remainingSoc, 0) + "%", 10, Ui.GREEN_DARK));
            }
            card.addView(soc);
        }

        if (!safe(r.note).isEmpty() || !safe(r.station).isEmpty()) {
            LinearLayout foot = Ui.horizontal(c);
            foot.setPadding(0, Ui.dp(c, 8), 0, 0);
            foot.addView(Ui.icon(c, IconView.LOCATION, 12, Ui.MUTED));
            String text = (safe(r.station).isEmpty() ? "" : r.station)
                    + (safe(r.note).isEmpty() ? "" : (safe(r.station).isEmpty() ? "" : " · ") + r.note);
            TextView t = Ui.regular(c, text, 9.5f, Ui.MUTED);
            t.setMaxLines(1);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            t.setPadding(Ui.dp(c, 3), 0, 0, 0);
            foot.addView(t, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            card.addView(foot);
        }

        if (r.isEnergy()) {
            card.addView(summaryStrip(r));
        }

        Ui.clickable(card, v -> activity.openForm(r.type, r));
        return card;
    }

    /** 记录卡片底部的本段摘要：元/公里 · 升/百公里 · 度/百公里，点击进入行程报告。 */
    private View summaryStrip(EnergyRecord r) {
        TripSlice t = repo.tripSlice(r);
        LinearLayout row = Ui.horizontal(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Ui.roundStroke(Color.WHITE, Ui.dp(c, 12), Ui.GREEN, Ui.dp(c, 1)));
        row.setPadding(Ui.dp(c, 10), Ui.dp(c, 8), Ui.dp(c, 8), Ui.dp(c, 8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(c, 10);
        row.setLayoutParams(lp);
        row.addView(stripText(t.hasDistance() ? Formats.num(t.costPerKm, 2) + " 元/公里" : "-- 元/公里"), weight());
        row.addView(stripText(r.isFuel() && t.hasDistance()
                ? Formats.num(t.lPer100, 2) + " 升/百公里" : "-- 升/百公里"), weight());
        row.addView(stripText(r.isCharge() && t.hasDistance()
                ? Formats.num(t.kwhPer100, 2) + " 度/百公里" : "-- 度/百公里"), weight());
        row.addView(Ui.icon(c, IconView.CHEVRON_RIGHT, 15, Ui.GREEN));
        Ui.clickable(row, v -> activity.push(new TripDetailScreen(activity, r.id)));
        return row;
    }

    private TextView stripText(String text) {
        TextView tv = Ui.medium(c, text, 9.5f, Ui.GREEN_DEEP);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        return tv;
    }

    private String title(EnergyRecord r) {
        if (r.isCharge()) return "充电 " + Formats.num(r.quantity, 2) + " 度";
        if (r.isFuel()) return "加油 " + Formats.num(r.quantity, 2) + " 升";
        return r.category == null || r.category.isEmpty() ? r.typeLabel() : r.category;
    }

    private String subtitle(EnergyRecord r) {
        StringBuilder sb = new StringBuilder();
        if (r.isCharge()) {
            sb.append(Formats.num(r.effectiveUnitPrice(), 2)).append(" 元/度");
        } else if (r.isFuel()) {
            sb.append(r.fuelLabel()).append("  ·  ");
            sb.append(Formats.num(r.effectiveUnitPrice(), 2)).append(" 元/升");
        }
        if (r.odometer > 0) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append("总里程 ").append(Formats.num(r.odometer, 0)).append(" 公里");
        }
        if (sb.length() == 0) sb.append(r.isIncome() ? "收入记录" : "支出记录");
        return sb.toString();
    }

    private int typeBg(EnergyRecord r) {
        if (r.isCharge()) return Ui.GREEN_LIGHT;
        if (r.isFuel()) return 0xFFFFF1E8;
        if (r.isIncome()) return Ui.GREEN_LIGHT;
        return 0xFFFFECEB;
    }

    private int typeColor(EnergyRecord r) {
        if (r.isCharge()) return Ui.GREEN_DARK;
        if (r.isFuel()) return 0xFFE06A10;
        if (r.isIncome()) return Ui.GREEN_DARK;
        return 0xFFFF3B30;
    }

    private int typeIcon(EnergyRecord r) {
        if (r.isCharge()) return IconView.BOLT;
        if (r.isFuel()) return IconView.FUEL;
        if (r.isIncome()) return IconView.INCOME;
        return IconView.EXPENSE;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
