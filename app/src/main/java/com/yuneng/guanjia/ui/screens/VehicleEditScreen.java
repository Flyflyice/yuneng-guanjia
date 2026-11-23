package com.yuneng.guanjia.ui.screens;

import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.model.Vehicle;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.widgets.AppBar;
import com.yuneng.guanjia.ui.widgets.FormRow;
import com.yuneng.guanjia.ui.widgets.PickerDialog;
import com.yuneng.guanjia.ui.widgets.Segmented;
import com.yuneng.guanjia.util.Formats;

import java.util.List;

/** 新增 / 编辑车辆。 */
public class VehicleEditScreen extends BaseScreen {
    private final Vehicle editing;
    private boolean ready;
    private String type;
    private String fuelGrade;
    private boolean makeDefault;
    private FormRow nameRow;
    private FormRow batteryRow;
    private FormRow tankRow;
    private FormRow thermalRow;
    private FormRow fuelRow;
    private LinearLayout batteryBlock;
    private LinearLayout tankBlock;
    private LinearLayout thermalBlock;
    private LinearLayout fuelBlock;
    private TextView tipLine1;
    private TextView tipLine2;
    private TextView defaultOn;
    private TextView defaultOff;

    public VehicleEditScreen(MainActivity activity, Vehicle editing) {
        super(activity);
        this.editing = editing;
        this.type = editing != null ? editing.type : Vehicle.TYPE_UNSET;
        this.fuelGrade = editing != null ? editing.fuelGrade : "92#";
        this.makeDefault = editing == null || editing.isDefault;
        ready = true;
        buildAgain();
    }

    private void buildAgain() {
        removeAllViews();
        build();
    }

    @Override
    protected void build() {
        if (!ready) return;
        AppBar bar = new AppBar(c);
        bar.title(editing == null ? "添加车辆" : "编辑车辆").dark(true).background(Color.WHITE).divider(true);
        bar.back(v -> activity.pop());
        bar.action("保存", v -> save());
        add(bar);

        scrollBody(24);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        body.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), 0);

        LinearLayout card = whiteCard();
        card.setPadding(Ui.dp(c, 16), Ui.dp(c, 4), Ui.dp(c, 16), Ui.dp(c, 12));

        nameRow = new FormRow(c).label("车辆名称").required()
                .input("例如：秦L 120KM", editing == null ? "" : editing.name, InputType.TYPE_CLASS_TEXT)
                .noArrow();
        nameRow.inputWidth(180);
        card.addView(nameRow);
        card.addView(Ui.dashedDivider(c));

        LinearLayout typeBox = Ui.vertical(c);
        typeBox.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 6));
        LinearLayout typeLabel = Ui.horizontal(c);
        typeLabel.addView(Ui.medium(c, "动力类型", 13.5f, Ui.INK_2));
        typeLabel.addView(Ui.regular(c, " *", 13.5f, Ui.RED));
        typeBox.addView(typeLabel);
        Segmented seg = new Segmented(c, "纯电", "燃油", "插电混动", "增程");
        seg.setSelected(typeIndex(type));
        seg.listener(index -> {
            type = index == 0 ? Vehicle.TYPE_EV : index == 1 ? Vehicle.TYPE_ICE
                    : index == 2 ? Vehicle.TYPE_PHEV : Vehicle.TYPE_EREV;
            updateVisibility();
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(c, 7);
        typeBox.addView(seg, slp);
        card.addView(typeBox);
        card.addView(Ui.dashedDivider(c));

        batteryRow = numberRow("电池容量", editing == null ? "15.87" : Formats.trim(editing.batteryCapacity, 2), "度");
        batteryBlock = rowBlock(batteryRow);
        card.addView(batteryBlock);
        tankRow = numberRow("油箱容量", editing == null ? "65" : Formats.trim(editing.tankCapacity, 2), "L");
        tankBlock = rowBlock(tankRow);
        card.addView(tankBlock);
        thermalRow = numberRow("发动机热效率", editing == null ? "46.06" : Formats.num(editing.thermalEfficiency * 100, 2), "%");
        thermalBlock = rowBlock(thermalRow);
        card.addView(thermalBlock);

        fuelRow = new FormRow(c).label("燃油标号").value(fuelGrade);
        fuelRow.click(v -> PickerDialog.choose(c, "选择燃油标号",
                new String[]{"92#", "95#", "98#", "0# 柴油"}, -1, (index, value) -> {
                    fuelGrade = value;
                    fuelRow.value(value);
                }));
        fuelBlock = rowBlock(fuelRow);
        card.addView(fuelBlock);

        LinearLayout defaultRow = Ui.horizontal(c);
        defaultRow.setMinimumHeight(Ui.dp(c, 56));
        LinearLayout left = Ui.vertical(c);
        left.addView(Ui.medium(c, "设为默认车辆", 13.5f, Ui.INK_2));
        left.addView(Ui.regular(c, "默认车辆会优先展示在列表顶部", 9.5f, Ui.MUTED));
        defaultRow.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout toggle = Ui.horizontal(c);
        defaultOff = Ui.medium(c, "否", 11.5f, Ui.SUB);
        defaultOff.setGravity(Gravity.CENTER);
        defaultOff.setPadding(Ui.dp(c, 15), Ui.dp(c, 7), Ui.dp(c, 15), Ui.dp(c, 7));
        defaultOn = Ui.medium(c, "是", 11.5f, Color.WHITE);
        defaultOn.setGravity(Gravity.CENTER);
        defaultOn.setPadding(Ui.dp(c, 15), Ui.dp(c, 7), Ui.dp(c, 15), Ui.dp(c, 7));
        toggle.addView(defaultOff);
        LinearLayout.LayoutParams onLp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        onLp.leftMargin = Ui.dp(c, 6);
        toggle.addView(defaultOn, onLp);
        defaultRow.addView(toggle);
        card.addView(defaultRow);
        applyDefault();
        Ui.clickable(defaultOff, v -> {
            makeDefault = false;
            applyDefault();
        });
        Ui.clickable(defaultOn, v -> {
            makeDefault = true;
            applyDefault();
        });
        body.addView(card);

        LinearLayout tip = Ui.vertical(c);
        tip.setPadding(Ui.dp(c, 6), Ui.dp(c, 12), Ui.dp(c, 6), 0);
        tipLine1 = Ui.regular(c, "", 10, Ui.SUB);
        tipLine2 = Ui.regular(c, "", 10, Ui.SUB);
        tip.addView(tipLine1);
        tip.addView(tipLine2);
        body.addView(tip);

        TextView save = Ui.medium(c, editing == null ? "添加车辆" : "保存修改", 15, Color.WHITE);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 14)));
        save.setPadding(0, Ui.dp(c, 14), 0, Ui.dp(c, 14));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        saveLp.topMargin = Ui.dp(c, 18);
        body.addView(save, saveLp);
        Ui.clickable(save, v -> save());
        body.addView(Ui.space(c, 16));

        updateVisibility();
    }

    private FormRow numberRow(String label, String value, String unit) {
        return new FormRow(c).label(label).required()
                .input("请输入", value, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .unit(unit).noArrow();
    }

    private int typeIndex(String t) {
        if (Vehicle.TYPE_EV.equals(t)) return 0;
        if (Vehicle.TYPE_ICE.equals(t)) return 1;
        if (Vehicle.TYPE_PHEV.equals(t)) return 2;
        if (Vehicle.TYPE_EREV.equals(t)) return 3;
        return -1;
    }

    private void updateVisibility() {
        if (batteryBlock == null || tankBlock == null || thermalBlock == null) return;
        boolean unset = type == null || Vehicle.TYPE_UNSET.equals(type);
        boolean ev = Vehicle.TYPE_EV.equals(type);
        boolean ice = Vehicle.TYPE_ICE.equals(type);
        boolean erev = Vehicle.TYPE_EREV.equals(type);
        boolean hybrid = Vehicle.TYPE_PHEV.equals(type) || erev;
        batteryBlock.setVisibility(unset || ice ? View.GONE : View.VISIBLE);
        tankBlock.setVisibility(unset || ev ? View.GONE : View.VISIBLE);
        thermalBlock.setVisibility(hybrid ? View.VISIBLE : View.GONE);
        if (fuelBlock != null) fuelBlock.setVisibility(unset || ev ? View.GONE : View.VISIBLE);

        if (tipLine1 != null && tipLine2 != null) {
            if (unset) {
                tipLine1.setText("· 请选择动力类型，再填写对应的车辆信息。");
                tipLine2.setText("");
            } else if (ev) {
                tipLine1.setText("· 纯电车型按充电电量与行驶里程计算平均电耗。");
                tipLine2.setText("· 电池容量用于估算充电电量与电池健康度。");
            } else if (ice) {
                tipLine1.setText("· 燃油车型按加油量与行驶里程计算平均油耗。");
                tipLine2.setText("· 油箱容量用于校验单次加油量是否合理。");
            } else if (erev) {
                tipLine1.setText("· 增程车型以电驱为主，记录剩余油量与加油时剩余电量可拆分纯电 / 燃油里程。");
                tipLine2.setText("· 电池容量用于估算充电电量与电池健康度。");
            } else {
                tipLine1.setText("· 插电混动默认按比亚迪第五代 DM-i 46.06% 热效率折算。");
                tipLine2.setText("· 电池容量用于估算充电电量与电池健康度。");
            }
        }
    }

    private LinearLayout rowBlock(View row) {
        LinearLayout block = Ui.vertical(c);
        block.addView(row);
        block.addView(Ui.dashedDivider(c));
        return block;
    }

    private void applyDefault() {
        if (defaultOn == null) return;
        defaultOff.setTextColor(makeDefault ? Ui.SUB : Color.WHITE);
        defaultOff.setBackground(makeDefault ? Ui.round(0xFFF4F6F7, Ui.dp(c, 9)) : Ui.round(Ui.GREEN, Ui.dp(c, 9)));
        defaultOn.setTextColor(makeDefault ? Color.WHITE : Ui.SUB);
        defaultOn.setBackground(makeDefault ? Ui.round(Ui.GREEN, Ui.dp(c, 9)) : Ui.round(0xFFF4F6F7, Ui.dp(c, 9)));
    }

    private void save() {
        String name = nameRow.text().trim();
        if (name.isEmpty()) {
            Ui.toast(c, "请填写车辆名称");
            return;
        }
        if (type == null || Vehicle.TYPE_UNSET.equals(type)) {
            Ui.toast(c, "请选择动力类型");
            return;
        }
        double battery = Formats.parse(batteryRow.text(), 15.87);
        double tank = Formats.parse(tankRow.text(), 65);
        double thermal = Formats.parse(thermalRow.text(), 46.06) / 100.0;
        if (battery <= 0) battery = 15.87;
        if (tank <= 0) tank = 65;
        if (thermal <= 0) thermal = 0.4606;
        if (thermal < 0.25 || thermal > 0.6) {
            Ui.toast(c, "热效率请填写 25% - 60% 之间的数值");
            return;
        }

        Vehicle v = editing != null ? editing : new Vehicle();
        boolean wasDefault = editing != null && editing.isDefault;
        v.name = name;
        v.type = type;
        v.batteryCapacity = battery;
        v.tankCapacity = tank;
        v.thermalEfficiency = thermal;
        v.fuelGrade = fuelGrade;
        v.isDefault = makeDefault;

        if (makeDefault) {
            List<Vehicle> all = repo.getVehicles();
            for (Vehicle other : all) {
                if (editing != null && other.id == editing.id) continue;
                if (other.isDefault) {
                    other.isDefault = false;
                    repo.updateVehicle(other);
                }
            }
        } else if (wasDefault) {
            // 取消默认时把默认身份交给另一辆车；只有一辆车时仍保持默认。
            List<Vehicle> all = repo.getVehicles();
            Vehicle successor = null;
            for (Vehicle other : all) {
                if (other.id != editing.id) {
                    successor = other;
                    break;
                }
            }
            if (successor == null) {
                v.isDefault = true;
            } else {
                successor.isDefault = true;
                repo.updateVehicle(successor);
            }
        }
        if (editing == null) repo.addVehicle(v);
        else repo.updateVehicle(v);
        Ui.toast(c, editing == null ? "车辆已添加" : "车辆已保存");
        activity.onVehicleChanged();
        activity.pop();
    }
}
