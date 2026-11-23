package com.yuneng.guanjia.ui.widgets;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.ui.Ui;

/**
 * 通用顶部栏：支持返回、标题、右侧操作。自动处理状态栏高度。
 */
public class AppBar extends FrameLayout {
    private final TextView title;
    private final TextView subtitle;
    private final LinearLayout leftBox;
    private final LinearLayout rightBox;
    private final View bottomLine;
    private int barColor = Ui.WHITE;
    private int textColor = Ui.INK;

    public AppBar(Context c) {
        super(c);
        int status = Ui.statusBarHeight(c);
        setPadding(0, status, 0, 0);
        setBackgroundColor(barColor);

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(c, 8), 0, Ui.dp(c, 8), 0);
        addView(row, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 54)));

        leftBox = new LinearLayout(c);
        leftBox.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(leftBox, new LinearLayout.LayoutParams(Ui.dp(c, 76), Ui.dp(c, 54)));

        LinearLayout center = Ui.vertical(c);
        center.setGravity(Gravity.CENTER);
        title = Ui.bold(c, "", 17, textColor);
        title.setGravity(Gravity.CENTER);
        subtitle = Ui.regular(c, "", 10.5f, Ui.SUB);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setVisibility(GONE);
        center.addView(title);
        center.addView(subtitle);
        row.addView(center, new LinearLayout.LayoutParams(0, Ui.dp(c, 54), 1f));

        rightBox = new LinearLayout(c);
        rightBox.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        rightBox.setMinimumWidth(Ui.dp(c, 76));
        row.addView(rightBox, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, Ui.dp(c, 54)));

        bottomLine = new View(c);
        bottomLine.setBackgroundColor(0x11000000);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1);
        blp.gravity = Gravity.BOTTOM;
        addView(bottomLine, blp);
        bottomLine.setVisibility(GONE);
    }

    public AppBar title(String s) {
        title.setText(s == null ? "" : s);
        return this;
    }

    public AppBar subtitle(String s) {
        subtitle.setText(s == null ? "" : s);
        subtitle.setVisibility(s == null || s.isEmpty() ? GONE : VISIBLE);
        return this;
    }

    public AppBar back(View.OnClickListener l) {
        leftBox.removeAllViews();
        View icon = Ui.icon(getContext(), com.yuneng.guanjia.ui.widgets.IconView.CHEVRON_LEFT, 24, textColor);
        icon.setPadding(Ui.dp(getContext(), 8), Ui.dp(getContext(), 8), Ui.dp(getContext(), 8), Ui.dp(getContext(), 8));
        leftBox.addView(icon);
        Ui.clickable(icon, l);
        return this;
    }

    public AppBar leftText(String s, View.OnClickListener l) {
        leftBox.removeAllViews();
        TextView t = Ui.medium(getContext(), s, 13, textColor);
        t.setPadding(Ui.dp(getContext(), 8), 0, 0, 0);
        leftBox.addView(t);
        if (l != null) Ui.clickable(t, l);
        return this;
    }

    public AppBar action(String s, View.OnClickListener l) {
        rightBox.removeAllViews();
        TextView t = Ui.medium(getContext(), s, 13, Ui.GREEN);
        t.setPadding(Ui.dp(getContext(), 8), Ui.dp(getContext(), 6), Ui.dp(getContext(), 8), Ui.dp(getContext(), 6));
        t.setBackground(Ui.round(0x1400C853, Ui.dp(getContext(), 8)));
        rightBox.addView(t);
        Ui.clickable(t, l);
        return this;
    }

    public AppBar actionIcon(int icon, int color, View.OnClickListener l) {
        rightBox.removeAllViews();
        View v = Ui.icon(getContext(), icon, 23, color);
        v.setPadding(Ui.dp(getContext(), 8), Ui.dp(getContext(), 8), Ui.dp(getContext(), 8), Ui.dp(getContext(), 8));
        rightBox.addView(v);
        Ui.clickable(v, l);
        return this;
    }

    public AppBar customRight(View v) {
        rightBox.removeAllViews();
        rightBox.addView(v);
        return this;
    }

    public AppBar background(int color) {
        this.barColor = color;
        setBackgroundColor(color);
        return this;
    }

    public AppBar gradient(int[] colors) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        setBackground(d);
        return this;
    }

    public AppBar dark(boolean darkText) {
        this.textColor = darkText ? Ui.INK : Color.WHITE;
        title.setTextColor(textColor);
        subtitle.setTextColor(darkText ? Ui.SUB : 0xCCFFFFFF);
        return this;
    }

    public AppBar divider(boolean show) {
        bottomLine.setVisibility(show ? VISIBLE : GONE);
        return this;
    }
}
