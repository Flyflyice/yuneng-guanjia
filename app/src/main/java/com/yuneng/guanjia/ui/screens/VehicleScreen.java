package com.yuneng.guanjia.ui.screens;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.model.Vehicle;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.widgets.AppBar;
import com.yuneng.guanjia.ui.widgets.IconView;

import java.util.List;

/** 车辆列表：切换当前车辆、新增或编辑车辆。 */
public class VehicleScreen extends BaseScreen {
    public VehicleScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected void build() {
        AppBar bar = new AppBar(c);
        bar.title("我的车辆").dark(true).background(Color.WHITE).divider(true);
        bar.back(v -> activity.pop());
        bar.actionIcon(IconView.PLUS, Ui.GREEN_DARK, v -> activity.push(new VehicleEditScreen(activity, null)));
        add(bar);

        scrollBody(24);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        body.setPadding(Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12), 0);

        TextView tip = Ui.regular(c, "选择车辆后，首页、费用与统计都会切换到该车的数据。", 10.5f, Ui.SUB);
        tip.setPadding(Ui.dp(c, 4), 0, Ui.dp(c, 4), Ui.dp(c, 10));
        body.addView(tip);

        List<Vehicle> vehicles = repo.getVehicles();
        boolean canDelete = vehicles.size() > 1;
        for (Vehicle v : vehicles) {
            body.addView(vehicleCard(v, canDelete));
            body.addView(Ui.space(c, 9));
        }

        TextView add = Ui.medium(c, "+ 添加车辆", 13, Ui.GREEN_DARK);
        add.setGravity(Gravity.CENTER);
        add.setPadding(0, Ui.dp(c, 14), 0, Ui.dp(c, 14));
        add.setBackground(Ui.roundStroke(Color.WHITE, Ui.dp(c, 16), 0x5500C853, Ui.dp(c, 1)));
        Ui.clickable(add, v -> activity.push(new VehicleEditScreen(activity, null)));
        body.addView(add);
        body.addView(Ui.space(c, 12));
    }

    private View vehicleCard(Vehicle v, boolean canDelete) {
        boolean current = vehicle != null && v.id == vehicle.id;
        LinearLayout card = whiteCard();
        card.setPadding(Ui.dp(c, 14), Ui.dp(c, 14), Ui.dp(c, 14), Ui.dp(c, 14));
        if (current) card.setBackground(Ui.roundStroke(Color.WHITE, Ui.dp(c, 18), 0x6600C853, Ui.dp(c, 1.5f)));

        LinearLayout top = Ui.horizontal(c);
        View iconBg = new View(c);
        iconBg.setBackground(Ui.circle(current ? Ui.GREEN_LIGHT : 0xFFF2F4F6, Ui.dp(c, 42)));
        FrameLayout iconBox = Ui.frame(c);
        iconBox.addView(iconBg, new LinearLayout.LayoutParams(Ui.dp(c, 42), Ui.dp(c, 42)));
        iconBox.addView(Ui.icon(c, IconView.CAR, 24, current ? Ui.GREEN_DARK : Ui.SUB),
                new android.widget.FrameLayout.LayoutParams(Ui.dp(c, 24), Ui.dp(c, 24), Gravity.CENTER));
        top.addView(iconBox, new LinearLayout.LayoutParams(Ui.dp(c, 42), Ui.dp(c, 42)));

        LinearLayout texts = Ui.vertical(c);
        texts.setPadding(Ui.dp(c, 10), 0, 0, 0);
        LinearLayout nameRow = Ui.horizontal(c);
        nameRow.addView(Ui.bold(c, v.shortName(), 15, Ui.INK));
        if (v.isDefault) {
            TextView def = Ui.chip(c, "默认", Ui.GREEN_LIGHT, Ui.GREEN_DARK, 8.5f);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            dlp.leftMargin = Ui.dp(c, 6);
            nameRow.addView(def, dlp);
        }
        texts.addView(nameRow);
        if (!v.specLine().isEmpty()) {
            texts.addView(Ui.regular(c, v.specLine(), 10.5f, Ui.SUB));
        }
        top.addView(texts, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        if (current) {
            top.addView(Ui.iconFilled(c, IconView.CHECK, 20, Ui.GREEN));
        } else {
            TextView choose = Ui.medium(c, "切换", 10, Ui.SUB);
            choose.setPadding(Ui.dp(c, 9), Ui.dp(c, 5), Ui.dp(c, 9), Ui.dp(c, 5));
            choose.setBackground(Ui.round(0xFFF2F4F6, Ui.dp(c, 8)));
            top.addView(choose);
        }
        card.addView(top);

        LinearLayout foot = Ui.horizontal(c);
        foot.setPadding(0, Ui.dp(c, 12), 0, 0);
        if (!v.typeLabel().isEmpty()) {
            foot.addView(Ui.regular(c, "动力类型 ", 9.5f, Ui.SUB));
            foot.addView(Ui.medium(c, v.typeLabel(), 9.5f, Ui.INK_2));
        }
        foot.addView(Ui.flexible(c));
        if (canDelete) {
            TextView delete = Ui.medium(c, "删除", 10, Ui.RED);
            delete.setPadding(Ui.dp(c, 9), Ui.dp(c, 5), Ui.dp(c, 9), Ui.dp(c, 5));
            delete.setBackground(Ui.round(0xFFFFF0F0, Ui.dp(c, 8)));
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            dlp.rightMargin = Ui.dp(c, 6);
            foot.addView(delete, dlp);
            Ui.clickable(delete, view -> confirmDelete(v));
        }
        TextView edit = Ui.medium(c, "编辑车辆", 10, Ui.GREEN_DARK);
        edit.setPadding(Ui.dp(c, 9), Ui.dp(c, 5), Ui.dp(c, 9), Ui.dp(c, 5));
        edit.setBackground(Ui.round(Ui.GREEN_LIGHT, Ui.dp(c, 8)));
        foot.addView(edit);
        card.addView(foot);

        Ui.clickable(card, view -> {
            repo.selectVehicle(v.id);
            Ui.toast(c, "已切换到 " + v.shortName());
            activity.onVehicleChanged();
            activity.pop();
        });
        Ui.clickable(edit, view -> activity.push(new VehicleEditScreen(activity, v)));
        return card;
    }

    private void confirmDelete(Vehicle v) {
        int count = repo.recordCount(v.id);
        String message = count > 0
                ? "删除“" + v.shortName() + "”后，该车的 " + count + " 条记录也会一并删除，且无法恢复。"
                : "删除“" + v.shortName() + "”后不可恢复，确定删除吗？";
        AlertDialog dialog = new AlertDialog.Builder(c)
                .setTitle("删除车辆")
                .setMessage(message)
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, which) -> {
                    if (!repo.deleteVehicle(v.id)) {
                        Ui.toast(c, "至少需要保留一辆车辆");
                        return;
                    }
                    Ui.toast(c, "车辆已删除");
                    activity.onVehicleChanged();
                    refresh();
                })
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Ui.RED));
        dialog.show();
    }
}
