package com.yuneng.guanjia.ui.screens;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.data.Repository;
import com.yuneng.guanjia.model.TimeRange;
import com.yuneng.guanjia.model.Vehicle;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.widgets.RangePicker;

/** 页面基类：统一背景、滚动容器与刷新逻辑。 */
public abstract class BaseScreen extends LinearLayout {
    protected final MainActivity activity;
    protected final Context c;
    protected final Repository repo;
    protected Vehicle vehicle;
    protected LinearLayout body;
    protected ScrollView scroll;
    /** 二级页面携带的记录 id，普通页面为 0。 */
    protected long recordId;

    public BaseScreen(MainActivity activity) {
        this(activity, 0L);
    }

    public BaseScreen(MainActivity activity, long recordId) {
        super(activity);
        this.activity = activity;
        this.c = activity;
        this.recordId = recordId;
        this.repo = activity.repo();
        this.vehicle = activity.vehicle();
        setOrientation(VERTICAL);
        setBackgroundColor(Ui.BG);
        build();
    }

    protected abstract void build();

    /** 创建一个可滚动的白色页面主体。 */
    protected LinearLayout scrollBody(int bottomPaddingDp) {
        scroll = new ScrollView(c);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, Ui.dp(c, bottomPaddingDp));
        body = Ui.vertical(c);
        scroll.addView(body, new ScrollView.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return body;
    }

    public void refresh() {
        int y = scroll == null ? 0 : scroll.getScrollY();
        vehicle = activity.vehicle();
        removeAllViews();
        build();
        if (scroll != null && y > 0) {
            final ScrollView target = scroll;
            target.post(() -> target.scrollTo(0, y));
        }
    }

    protected void add(View v) {
        addView(v, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    protected void add(View v, LinearLayout.LayoutParams lp) {
        addView(v, lp);
    }

    protected void addBody(View v) {
        body.addView(v, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    protected void addBody(View v, LinearLayout.LayoutParams lp) {
        body.addView(v, lp);
    }

    protected View sectionTitle(String title, String action, View.OnClickListener listener) {
        LinearLayout row = Ui.horizontal(c);
        row.setPadding(Ui.dp(c, 4), Ui.dp(c, 18), Ui.dp(c, 4), Ui.dp(c, 10));
        View bar = new View(c);
        bar.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 2)));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(c, 3), Ui.dp(c, 15));
        blp.rightMargin = Ui.dp(c, 7);
        row.addView(bar, blp);
        row.addView(Ui.bold(c, title, 15, Ui.INK));
        row.addView(Ui.flexible(c));
        if (action != null) {
            android.widget.TextView t = Ui.regular(c, action, 11.5f, Ui.SUB);
            row.addView(t);
            if (listener != null) Ui.clickable(t, listener);
        }
        return row;
    }

    protected View divider() {
        View v = new View(c);
        v.setBackgroundColor(Ui.LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 1)));
        return v;
    }

    protected View dot(int color, int sizeDp) {
        View v = new View(c);
        v.setBackground(Ui.circle(color, Ui.dp(c, sizeDp)));
        return v;
    }

    protected LinearLayout whiteCard() {
        LinearLayout card = Ui.card(c);
        card.setBackground(Ui.round(Color.WHITE, Ui.dp(c, 18)));
        return card;
    }

    /** 卡片标题旁的可点击时间范围标签：点击弹出 近一年/年度/月度/自定义。 */
    protected android.widget.TextView rangeLabel(TimeRange current, RangePicker.Listener apply) {
        return rangeLabel(current, Ui.SUB, apply);
    }

    protected android.widget.TextView rangeLabel(TimeRange current, int color, RangePicker.Listener apply) {
        TimeRange range = current == null ? TimeRange.lastYear() : current;
        android.widget.TextView tv = Ui.regular(c, range.shortLabel() + " ▾", 10.5f, color);
        tv.setPadding(Ui.dp(c, 4), Ui.dp(c, 2), Ui.dp(c, 2), Ui.dp(c, 2));
        Ui.clickable(tv, v -> RangePicker.show(c, range, r -> {
            apply.onRange(r);
            refresh();
        }));
        return tv;
    }
}
