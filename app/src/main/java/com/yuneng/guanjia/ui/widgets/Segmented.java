package com.yuneng.guanjia.ui.widgets;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.ui.Ui;

/**
 * 二选一分段控件，例如“充电 / 加油”。
 */
public class Segmented extends LinearLayout {
    public interface Listener {
        void onChanged(int index);
    }

    private final TextView[] tabs;
    private int selected = 0;
    private Listener listener;

    public Segmented(Context c, String... names) {
        super(c);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackground(Ui.round(0xFFF4F6F7, Ui.dp(c, 10)));
        setPadding(Ui.dp(c, 3), Ui.dp(c, 3), Ui.dp(c, 3), Ui.dp(c, 3));
        tabs = new TextView[names.length];
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            TextView t = Ui.medium(c, names[i], 13, Ui.SUB);
            t.setGravity(Gravity.CENTER);
            t.setPadding(Ui.dp(c, 18), Ui.dp(c, 7), Ui.dp(c, 18), Ui.dp(c, 7));
            tabs[i] = t;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, Ui.dp(c, 34));
            lp.leftMargin = i == 0 ? 0 : Ui.dp(c, 4);
            addView(t, lp);
            t.setOnClickListener(v -> setSelected(index, true));
        }
        apply();
    }

    public void setSelected(int index, boolean notify) {
        selected = index;
        apply();
        if (notify && listener != null) listener.onChanged(index);
    }

    public void setSelected(int index) {
        setSelected(index, false);
    }

    public int getSelected() {
        return selected;
    }

    public Segmented listener(Listener l) {
        listener = l;
        return this;
    }

    private void apply() {
        Context c = getContext();
        for (int i = 0; i < tabs.length; i++) {
            boolean on = i == selected;
            tabs[i].setTextColor(on ? Color.WHITE : Ui.SUB);
            tabs[i].setBackground(on ? Ui.round(Ui.GREEN, Ui.dp(c, 8)) : null);
            tabs[i].setTypeface(on ? Ui.MEDIUM : Ui.REGULAR);
        }
    }
}
