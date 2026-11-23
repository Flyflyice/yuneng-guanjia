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
 * 底部导航：能耗 / 费用 / 记一笔 / 统计 / 我的。
 */
public class BottomBar extends FrameLayout {
    public interface Listener {
        void onTab(int index);

        void onAdd();
    }

    private final TextView[] labels = new TextView[4];
    private final IconView[] icons = new IconView[4];
    private final Listener listener;
    private int selected = 0;

    public BottomBar(Context c, Listener l) {
        super(c);
        this.listener = l;
        setBackgroundColor(Color.WHITE);
        setElevation(Ui.dp(c, 10));

        View line = new View(c);
        line.setBackgroundColor(0xFFEDF0F2);
        addView(line, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, 1));

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        addView(row, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 58), Gravity.BOTTOM));

        int[] iconTypes = {IconView.LEAF, IconView.RECEIPT, IconView.CHART, IconView.PERSON};
        String[] names = {"能耗", "费用", "统计", "我的"};
        for (int i = 0; i < 4; i++) {
            final int index = i;
            LinearLayout item = Ui.vertical(c);
            item.setGravity(Gravity.CENTER);
            icons[i] = Ui.icon(c, iconTypes[i], 23, Ui.SUB);
            labels[i] = Ui.regular(c, names[i], 10, Ui.SUB);
            item.addView(icons[i]);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            tlp.topMargin = Ui.dp(c, 3);
            item.addView(labels[i], tlp);
            Ui.clickable(item, v -> {
                if (listener != null) listener.onTab(index);
            });
            row.addView(item, new LinearLayout.LayoutParams(0, Ui.dp(c, 58), 1f));
            // 中间留出“记一笔”的位置，使 5 个底部图标等距分布
            if (i == 1) {
                View centerGap = new View(c);
                row.addView(centerGap, new LinearLayout.LayoutParams(0, Ui.dp(c, 58), 1f));
            }
        }

        // 中间悬浮“记一笔”
        LinearLayout addBox = Ui.vertical(c);
        addBox.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView circle = new TextView(c);
        circle.setBackground(Ui.gradient(new int[]{Ui.GREEN, Ui.GREEN_MID}, Ui.dp(c, 26)));
        circle.setGravity(Gravity.CENTER);
        GradientDrawable shadow = Ui.round(0x3300A844, Ui.dp(c, 26));
        circle.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        IconView plus = Ui.icon(c, IconView.PLUS, 25, Color.WHITE);
        // 加号必须位于绿色圆之上，否则会被圆的阴影层盖住
        plus.setElevation(Ui.dp(c, 7));
        FrameLayout wrap = Ui.frame(c);
        wrap.setBackground(shadow);
        wrap.addView(circle, new FrameLayout.LayoutParams(Ui.dp(c, 52), Ui.dp(c, 52), Gravity.CENTER));
        wrap.addView(plus, new FrameLayout.LayoutParams(Ui.dp(c, 25), Ui.dp(c, 25), Gravity.CENTER));
        addBox.addView(wrap, new LinearLayout.LayoutParams(Ui.dp(c, 58), Ui.dp(c, 58)));
        Ui.clickable(wrap, v -> {
            if (listener != null) listener.onAdd();
        });

        TextView addLabel = Ui.medium(c, "记一笔", 9.5f, Ui.GREEN_DARK);
        addLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        labelLp.topMargin = Ui.dp(c, 1);
        addBox.addView(addLabel, labelLp);
        Ui.clickable(addLabel, v -> {
            if (listener != null) listener.onAdd();
        });

        FrameLayout.LayoutParams addLp = new FrameLayout.LayoutParams(Ui.dp(c, 58), Ui.dp(c, 72));
        addLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        addLp.bottomMargin = Ui.dp(c, 10);
        addView(addBox, addLp);
    }

    public void setSelected(int index) {
        selected = index;
        for (int i = 0; i < 4; i++) {
            boolean on = i == index;
            int color = on ? Ui.GREEN : Ui.SUB;
            icons[i].color(color);
            icons[i].filled(on && i == 0);
            labels[i].setTextColor(color);
            labels[i].setTypeface(on ? Ui.MEDIUM : Ui.REGULAR);
        }
    }
}
