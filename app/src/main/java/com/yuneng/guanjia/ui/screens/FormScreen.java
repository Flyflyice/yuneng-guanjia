package com.yuneng.guanjia.ui.screens;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.model.EnergyRecord;
import com.yuneng.guanjia.model.FuelTypes;
import com.yuneng.guanjia.model.Vehicle;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.widgets.AppBar;
import com.yuneng.guanjia.ui.widgets.FormRow;
import com.yuneng.guanjia.ui.widgets.IconView;
import com.yuneng.guanjia.ui.widgets.PickerDialog;
import com.yuneng.guanjia.ui.widgets.Segmented;
import com.yuneng.guanjia.util.Formats;

import java.io.File;
import java.util.List;
import java.util.Locale;

/** 记录表单：充电 / 加油 / 收入 / 支出，新增与编辑共用。 */
public class FormScreen extends BaseScreen {
    public static final int REQ_PHOTO = 901;

    private String type;
    private final EnergyRecord editing;
    private long timestamp;
    private double odometer;
    private boolean previousRecorded = true;
    private String station = "";
    private String category = "";
    private String photoPath = "";
    private String fuelType;
    private String fuelGrade;

    private FormRow dateRow;
    private FormRow odometerRow;
    private FormRow amountRow;
    private FormRow quantityRow;
    private FormRow unitPriceRow;
    private FormRow listPriceRow;
    private FormRow listAmountRow;
    private FormRow discountRow;
    private FormRow socStartRow;
    private FormRow socEndRow;
    private FormRow oilBeforeRow;
    private FormRow oilAfterRow;
    private FormRow remainingOilRow;
    private FormRow remainingSocRow;
    private FormRow categoryRow;
    private FormRow stationRow;
    private FormRow fuelTypeRow;
    private EditText fuelPriceInput;
    private EditText fuelQtyInput;
    private EditText fuelListInput;
    private EditText fuelActualPriceInput;
    private EditText fuelDiscountInput;
    private EditText fuelAmountInput;
    private TextView previousOn;
    private TextView previousOff;
    private FrameLayout photoBox;
    private TextView noteCount;
    private EditText noteInput;
    private Segmented segmented;
    private boolean updating;
    /** 折合电价是否由用户手动填写：是则以电价为准，否则按「金额 ÷ 电量」重算电价。 */
    private boolean chargePriceManual;
    private boolean ready;

    public FormScreen(MainActivity activity, String type, EnergyRecord editing) {
        super(activity);
        this.type = type == null ? EnergyRecord.TYPE_CHARGE : type;
        // 纯电/燃油车只有一种补能方式，避免入口传错类型
        if (EnergyRecord.TYPE_CHARGE.equals(this.type) || EnergyRecord.TYPE_FUEL.equals(this.type)) {
            if (vehicle.isElectric()) this.type = EnergyRecord.TYPE_CHARGE;
            else if (vehicle.isFuelOnly()) this.type = EnergyRecord.TYPE_FUEL;
        }
        this.editing = editing;
        this.timestamp = editing != null ? editing.timestamp : System.currentTimeMillis();
        this.odometer = editing != null ? editing.odometer : lastOdometer();
        this.previousRecorded = editing == null || editing.previousRecorded;
        this.station = editing != null ? safe(editing.station) : "";
        this.category = editing != null ? safe(editing.category) : "";
        this.photoPath = editing != null ? safe(editing.photoPath) : "";
        this.fuelGrade = editing != null && !safe(editing.fuelGrade).isEmpty()
                ? safe(editing.fuelGrade) : safe(vehicle.fuelGrade);
        this.fuelType = FuelTypes.findType(editing != null ? safe(editing.fuelType) : "", this.fuelGrade);
        if (this.fuelGrade.isEmpty()) this.fuelGrade = FuelTypes.bestGrade(this.fuelType, "92#");
        ready = true;
        buildAgain();
    }

    /** 构造完成后重建一次，确保字段使用传入的初始值。 */
    private void buildAgain() {
        removeAllViews();
        build();
    }

    @Override
    protected void build() {
        if (!ready) return;
        AppBar bar = new AppBar(c);
        bar.dark(true).background(Color.WHITE).divider(true);
        bar.title(editing != null ? "修改记录" : "添加记录");
        bar.back(v -> activity.pop());
        bar.customRight(actions());
        add(bar);

        scrollBody(26);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        body.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), 0);

        LinearLayout card = whiteCard();
        card.setPadding(Ui.dp(c, 16), Ui.dp(c, 4), Ui.dp(c, 16), Ui.dp(c, 12));
        card.addView(dateRow());
        card.addView(Ui.dashedDivider(c));

        if (isEnergy()) {
            card.addView(odometerRow());
            card.addView(Ui.dashedDivider(c));
            card.addView(segmentedRow());
            card.addView(Ui.dashedDivider(c));
        } else {
            card.addView(typeRow());
            card.addView(Ui.dashedDivider(c));
        }

        LinearLayout fields = Ui.vertical(c);
        card.addView(fields);
        if (EnergyRecord.TYPE_CHARGE.equals(type)) buildChargeFields(fields);
        else if (EnergyRecord.TYPE_FUEL.equals(type)) buildFuelFields(fields);
        else buildMoneyFields(fields);

        if (isEnergy()) {
            card.addView(Ui.dashedDivider(c));
            card.addView(stationRow());
            card.addView(Ui.dashedDivider(c));
            card.addView(previousRow());
        }
        card.addView(Ui.dashedDivider(c));
        card.addView(noteSection());
        card.addView(photoSection());
        body.addView(card);
        body.addView(Ui.space(c, 14));
        body.addView(saveButton());
        body.addView(Ui.space(c, 10));
    }

    private LinearLayout actions() {
        LinearLayout box = Ui.horizontal(c);
        if (editing != null) {
            TextView del = Ui.medium(c, "删除", 12, Ui.RED);
            del.setPadding(Ui.dp(c, 12), Ui.dp(c, 7), Ui.dp(c, 12), Ui.dp(c, 7));
            del.setBackground(Ui.round(Ui.RED_LIGHT, Ui.dp(c, 8)));
            box.addView(del);
            Ui.clickable(del, v -> confirmDelete());
            TextView save = Ui.medium(c, "修改", 12, Color.WHITE);
            save.setPadding(Ui.dp(c, 14), Ui.dp(c, 7), Ui.dp(c, 14), Ui.dp(c, 7));
            save.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 8)));
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            slp.leftMargin = Ui.dp(c, 6);
            box.addView(save, slp);
            Ui.clickable(save, v -> save());
        } else {
            TextView save = Ui.medium(c, "保存", 12, Color.WHITE);
            save.setPadding(Ui.dp(c, 16), Ui.dp(c, 7), Ui.dp(c, 16), Ui.dp(c, 7));
            save.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 8)));
            box.addView(save);
            Ui.clickable(save, v -> save());
        }
        return box;
    }

    private View dateRow() {
        dateRow = new FormRow(c).label("日期时间").required().value(Formats.dateTime(timestamp));
        dateRow.click(v -> PickerDialog.dateTime(c, timestamp, (index, value) -> {
            timestamp = Long.parseLong(value);
            dateRow.value(Formats.dateTime(timestamp));
        }));
        return dateRow;
    }

    private View odometerRow() {
        odometerRow = new FormRow(c).label("当前总里程").required()
                .input("请输入", format(odometer, 0), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                .unit("公里").noArrow();
        return odometerRow;
    }

    private View segmentedRow() {
        LinearLayout row = Ui.horizontal(c);
        row.setMinimumHeight(Ui.dp(c, 56));
        LinearLayout left = Ui.horizontal(c);
        left.addView(Ui.medium(c, "充能方式", 13.5f, Ui.INK_2));
        if (vehicle.isHybrid()) left.addView(Ui.regular(c, "（二选一）", 10, Ui.SUB));
        if (Vehicle.TYPE_UNSET.equals(vehicle.type)) left.addView(Ui.regular(c, "（二选一）", 10, Ui.SUB));
        TextView star = Ui.regular(c, " *", 13.5f, Ui.RED);
        left.addView(star);
        row.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        if (vehicle.isHybrid() || Vehicle.TYPE_UNSET.equals(vehicle.type)) {
            segmented = new Segmented(c, "充电", "加油");
            segmented.setSelected(EnergyRecord.TYPE_FUEL.equals(type) ? 1 : 0);
            segmented.listener(index -> {
                String next = index == 0 ? EnergyRecord.TYPE_CHARGE : EnergyRecord.TYPE_FUEL;
                if (!next.equals(type)) {
                    type = next;
                    removeAllViews();
                    build();
                }
            });
            row.addView(segmented);
        } else {
            // 纯电/燃油车只有一种补能方式，直接显示固定标签，避免误选
            TextView mode = Ui.medium(c, vehicle.isElectric() ? "充电" : "加油", 12, Color.WHITE);
            mode.setPadding(Ui.dp(c, 18), Ui.dp(c, 8), Ui.dp(c, 18), Ui.dp(c, 8));
            mode.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 8)));
            row.addView(mode);
        }
        return row;
    }

    private View typeRow() {
        categoryRow = new FormRow(c).label(EnergyRecord.TYPE_INCOME.equals(type) ? "收入类型" : "支出类型")
                .required().value(category.isEmpty() ? "请选择" : category);
        categoryRow.click(v -> {
            String[] options = EnergyRecord.TYPE_INCOME.equals(type)
                    ? new String[]{"工资", "报销", "红包", "兼职", "其他"}
                    : new String[]{"普通", "高速费", "停车费", "维修", "保险和交通规费", "过路费", "其他"};
            PickerDialog.choose(c, "选择类型", options, -1, (index, value) -> {
                category = value;
                categoryRow.value(value);
            });
        });
        return categoryRow;
    }

    private void buildChargeFields(LinearLayout box) {
        chargePriceManual = editing != null && editing.effectiveUnitPrice() > 0;
        amountRow = moneyRow("充电金额", editing == null ? "0.00" : format(editing.amount, 2), "元", true);
        box.addView(amountRow);
        box.addView(Ui.dashedDivider(c));
        quantityRow = moneyRow("充电电量", editing == null ? "0.00" : format(editing.quantity, 2), "度", true);
        box.addView(quantityRow);
        box.addView(Ui.dashedDivider(c));
        unitPriceRow = moneyRow("折合电价", editing == null ? "0.00" : format(editing.effectiveUnitPrice(), 2), "元/度", false);
        box.addView(unitPriceRow);
        box.addView(Ui.dashedDivider(c));
        socStartRow = moneyRow("开始充电时", editing == null ? "" : format(editing.socStart, 1), "%", true);
        box.addView(socStartRow);
        box.addView(Ui.dashedDivider(c));
        socEndRow = moneyRow("结束充电时", editing == null ? "" : format(editing.socEnd, 1), "%", true);
        box.addView(socEndRow);
        if (vehicle.isHybrid()) {
            box.addView(Ui.dashedDivider(c));
            remainingOilRow = moneyRow("剩余油量", editing == null ? "" : format(editing.remainingOil, 1), "%", true);
            box.addView(remainingOilRow);
        } else {
            remainingOilRow = null;
        }

        watchCharge(amountRow.input(), SOURCE_CHARGE_AMOUNT);
        watchCharge(quantityRow.input(), SOURCE_CHARGE_QTY);
        watchCharge(unitPriceRow.input(), SOURCE_CHARGE_PRICE);
    }

    private void buildFuelFields(LinearLayout box) {
        double price = editing == null ? 0 : (editing.listAmount > 0 && editing.quantity > 0
                ? editing.listAmount / editing.quantity : editing.unitPrice);
        fuelPriceInput = newFuelInput(editing == null ? "0.00" : format(price, 2));
        fuelQtyInput = newFuelInput(editing == null ? "0.00" : format(editing.quantity, 2));
        fuelListInput = newFuelInput(editing == null ? "0.00" : format(editing.listAmount, 2));
        fuelActualPriceInput = newFuelInput(editing == null ? "0.00" : format(editing.effectiveUnitPrice(), 2));
        fuelDiscountInput = newFuelInput(editing == null ? "0.00" : format(editing.discount, 2));
        fuelAmountInput = newFuelInput(editing == null ? "0.00" : format(editing.amount, 2));

        box.addView(fuelGridRow(
                new String[]{"机显单价", "加油量", "机显金额"},
                new boolean[]{true, true, true},
                new EditText[]{fuelPriceInput, fuelQtyInput, fuelListInput},
                new String[]{"元/升", "升", "元"},
                new String[]{"×", "="}));
        box.addView(Ui.dashedDivider(c));
        box.addView(fuelGridRow(
                new String[]{"实付单价", "优惠金额", "实付金额"},
                new boolean[]{false, false, false},
                new EditText[]{fuelActualPriceInput, fuelDiscountInput, fuelAmountInput},
                new String[]{"元/升", "元", "元"},
                new String[]{null, null}));
        box.addView(Ui.dashedDivider(c));
        fuelTypeRow = new FormRow(c).label("油品型号")
                .value(FuelTypes.display(fuelType, fuelGrade));
        fuelTypeRow.click(v -> chooseFuelType());
        box.addView(fuelTypeRow);
        box.addView(Ui.dashedDivider(c));
        oilBeforeRow = moneyRow("加油前剩余油量", editing == null ? "" : format(editing.oilBefore, 1), "%", true);
        box.addView(oilBeforeRow);
        box.addView(Ui.dashedDivider(c));
        oilAfterRow = moneyRow("加油后剩余油量", editing == null ? "" : format(editing.oilAfter, 1), "%", true);
        box.addView(oilAfterRow);
        if (vehicle.isHybrid()) {
            box.addView(Ui.dashedDivider(c));
            remainingSocRow = moneyRow("加油时剩余电量", editing == null ? "" : format(editing.remainingSoc, 1), "%", true);
            box.addView(remainingSocRow);
        } else {
            remainingSocRow = null;
        }

        watchFuel(fuelPriceInput, SOURCE_LIST_PRICE);
        watchFuel(fuelListInput, SOURCE_LIST_AMOUNT);
        watchFuel(fuelQtyInput, SOURCE_QTY);
        watchFuel(fuelDiscountInput, SOURCE_DISCOUNT);
        watchFuel(fuelAmountInput, SOURCE_AMOUNT);
        watchFuel(fuelActualPriceInput, SOURCE_ACTUAL_PRICE);
    }

    /** 加油表单的“标签一行 + 输入框一行”三列布局，对齐参考图。 */
    private LinearLayout fuelGridRow(String[] labels, boolean[] required, EditText[] inputs,
                                    String[] units, String[] symbols) {
        LinearLayout group = Ui.vertical(c);
        group.setPadding(0, Ui.dp(c, 9), 0, Ui.dp(c, 9));
        LinearLayout header = Ui.horizontal(c);
        header.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < labels.length; i++) {
            LinearLayout cell = Ui.horizontal(c);
            cell.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout labelBox = Ui.horizontal(c);
            labelBox.setGravity(Gravity.CENTER);
            // 星号占据标签右侧空间，左侧补同样一半宽度，使标签文字中心对准输入框
            labelBox.setPadding(Ui.dp(c, 8), 0, 0, 0);
            labelBox.addView(Ui.medium(c, labels[i], 11.5f, Ui.INK_2));
            if (required != null && required[i]) {
                labelBox.addView(Ui.regular(c, " *", 11.5f, Ui.RED));
            } else {
                // 用透明星号占位，保证上下两排文字中心对齐
                TextView ghost = Ui.regular(c, " *", 11.5f, Ui.RED);
                ghost.setAlpha(0f);
                labelBox.addView(ghost);
            }
            cell.addView(labelBox, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            // 透明单位与下方输入框右侧单位等宽，使标签中心对准输入框中心
            TextView unitGhost = Ui.regular(c, units[i], 10, Ui.SUB);
            unitGhost.setAlpha(0f);
            LinearLayout.LayoutParams uglp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            uglp.leftMargin = Ui.dp(c, 3);
            cell.addView(unitGhost, uglp);
            header.addView(cell, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            addFuelSymbol(header, symbols, i, false);
        }
        group.addView(header);

        LinearLayout values = Ui.horizontal(c);
        values.setGravity(Gravity.CENTER_VERTICAL);
        values.setPadding(0, Ui.dp(c, 6), 0, 0);
        for (int i = 0; i < inputs.length; i++) {
            LinearLayout cell = Ui.horizontal(c);
            cell.setGravity(Gravity.CENTER_VERTICAL);
            cell.addView(inputs[i], new LinearLayout.LayoutParams(0, Ui.dp(c, 42), 1f));
            TextView unit = Ui.regular(c, units[i], 10, Ui.SUB);
            LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT);
            ulp.leftMargin = Ui.dp(c, 3);
            cell.addView(unit, ulp);
            values.addView(cell, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            addFuelSymbol(values, symbols, i, true);
        }
        group.addView(values);
        return group;
    }

    private void addFuelSymbol(LinearLayout row, String[] symbols, int index, boolean spacer) {
        if (symbols == null || index >= symbols.length || symbols[index] == null) return;
        if (spacer) {
            row.addView(new View(c), new LinearLayout.LayoutParams(Ui.dp(c, 22), 1));
        } else {
            TextView sym = Ui.medium(c, symbols[index], 18, Ui.SUB);
            sym.setGravity(Gravity.CENTER);
            row.addView(sym, new LinearLayout.LayoutParams(Ui.dp(c, 22), LayoutParams.WRAP_CONTENT));
        }
    }

    private EditText newFuelInput(String value) {
        EditText e = Ui.edit(c, "0.00", 14);
        e.setText(value == null ? "" : value);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        e.setMinWidth(0);
        e.setMinimumWidth(0);
        e.setEms(0);
        e.setPadding(Ui.dp(c, 8), 0, Ui.dp(c, 8), 0);
        return e;
    }

    private void chooseFuelType() {
        int selected = Math.max(0, indexOf(FuelTypes.TYPES, fuelType));
        PickerDialog.choose(c, "选择油品类型", FuelTypes.TYPES, selected, (index, value) -> {
            fuelType = value;
            String[] grades = FuelTypes.grades(value);
            fuelGrade = FuelTypes.bestGrade(value, fuelGrade);
            int gradeIndex = Math.max(0, indexOf(grades, fuelGrade));
            updateFuelTypeRow();
            int finalGradeIndex = gradeIndex;
            // 等第一个弹窗关闭后再弹出标号列表
            fuelTypeRow.postDelayed(() -> PickerDialog.choose(c, "选择" + value + "标号",
                    grades, finalGradeIndex, (i, grade) -> {
                        fuelGrade = grade;
                        updateFuelTypeRow();
                    }), 180);
        });
    }

    private void updateFuelTypeRow() {
        if (fuelTypeRow != null) fuelTypeRow.value(FuelTypes.display(fuelType, fuelGrade));
    }

    private static int indexOf(String[] values, String value) {
        if (values == null || value == null) return -1;
        for (int i = 0; i < values.length; i++) {
            if (value.equals(values[i])) return i;
        }
        return -1;
    }

    private static final String SOURCE_CHARGE_AMOUNT = "chargeAmount";
    private static final String SOURCE_CHARGE_QTY = "chargeQty";
    private static final String SOURCE_CHARGE_PRICE = "chargePrice";
    private static final String SOURCE_LIST_PRICE = "listPrice";
    private static final String SOURCE_LIST_AMOUNT = "listAmount";
    private static final String SOURCE_QTY = "qty";
    private static final String SOURCE_DISCOUNT = "discount";
    private static final String SOURCE_AMOUNT = "amount";
    private static final String SOURCE_ACTUAL_PRICE = "actualPrice";

    private void watchFuel(EditText input, final String source) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalcFuel(source);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * 加油金额联动：机显单价 × 加油量 = 机显金额，机显金额 − 优惠金额 = 实付金额，实付金额 ÷ 加油量 = 实付单价。
     * 用户改动任何一个字段，其余相关字段立即重算；source 表示当前正在编辑的字段。
     */
    private void recalcFuel(String source) {
        if (updating) return;
        updating = true;
        try {
            double price = Formats.parse(fuelPriceInput.getText().toString(), 0);
            double qty = Formats.parse(fuelQtyInput.getText().toString(), 0);
            // 机显金额以输入框里的值为准，避免「量 × 单价」的两位小数舍入误差累积。
            double typedList = Formats.parse(fuelListInput.getText().toString(), 0);
            double list = typedList > 0.001 ? typedList : price * qty;
            if (SOURCE_LIST_PRICE.equals(source) || SOURCE_QTY.equals(source)) {
                list = price * qty;
                // 单价为空但已有机显金额时，按金额和加油量反推单价，避免把金额清成 0。
                if (price <= 0.001 && typedList > 0.001 && qty > 0.001) {
                    price = typedList / qty;
                    fuelPriceInput.setText(format(price, 2));
                    list = typedList;
                } else if (qty <= 0.001 && typedList > 0.001 && price > 0.001) {
                    // 先填了机显金额、后填单价：以金额为准反推加油量。
                    qty = typedList / price;
                    fuelQtyInput.setText(format(qty, 2));
                    list = typedList;
                } else if (price <= 0.001 && qty <= 0.001) {
                    list = typedList;
                }
                fuelListInput.setText(format(list, 2));
                double discount = Formats.parse(fuelDiscountInput.getText().toString(), 0);
                double actual = Math.max(0, list - discount);
                fuelAmountInput.setText(format(actual, 2));
                if (qty > 0.001) fuelActualPriceInput.setText(format(actual / qty, 2));
            } else if (SOURCE_LIST_AMOUNT.equals(source)) {
                // 改动机显金额时，按已有单价反推加油量；没有单价则按已有加油量反推单价。
                list = Formats.parse(fuelListInput.getText().toString(), 0);
                if (price > 0.001) {
                    qty = list / price;
                    fuelQtyInput.setText(format(qty, 2));
                } else if (qty > 0.001) {
                    price = list / qty;
                    fuelPriceInput.setText(format(price, 2));
                }
                double discount = Formats.parse(fuelDiscountInput.getText().toString(), 0);
                double actual = Math.max(0, list - discount);
                fuelAmountInput.setText(format(actual, 2));
                if (qty > 0.001) fuelActualPriceInput.setText(format(actual / qty, 2));
            } else if (SOURCE_DISCOUNT.equals(source)) {
                double discount = Formats.parse(fuelDiscountInput.getText().toString(), 0);
                double actual = Math.max(0, list - discount);
                fuelAmountInput.setText(format(actual, 2));
                if (qty > 0.001) fuelActualPriceInput.setText(format(actual / qty, 2));
            } else if (SOURCE_AMOUNT.equals(source)) {
                double actual = Formats.parse(fuelAmountInput.getText().toString(), 0);
                fuelDiscountInput.setText(format(Math.max(0, list - actual), 2));
                if (qty > 0.001) fuelActualPriceInput.setText(format(actual / qty, 2));
            } else if (SOURCE_ACTUAL_PRICE.equals(source)) {
                if (qty > 0.001) {
                    double actual = Formats.parse(fuelActualPriceInput.getText().toString(), 0) * qty;
                    fuelAmountInput.setText(format(actual, 2));
                    fuelDiscountInput.setText(format(Math.max(0, list - actual), 2));
                }
            }
        } finally {
            updating = false;
        }
    }

    private void buildMoneyFields(LinearLayout box) {
        amountRow = moneyRow(EnergyRecord.TYPE_INCOME.equals(type) ? "收入" : "支出",
                editing == null ? "" : format(editing.amount, 2), "元", true);
        box.addView(amountRow);
        box.addView(Ui.dashedDivider(c));
        LinearLayout hint = Ui.horizontal(c);
        hint.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 4));
        hint.addView(Ui.icon(c, IconView.INFO, 13, Ui.MUTED));
        hint.addView(Ui.regular(c, " 记录日常收入与支出，不计入能耗统计", 9.5f, Ui.MUTED));
        box.addView(hint);
    }

    private FormRow moneyRow(String label, String value, String unit, boolean required) {
        FormRow row = new FormRow(c).label(label).input("请输入", value,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL).unit(unit).noArrow();
        if (required) row.required();
        return row;
    }

    private void watchCharge(EditText input, final String source) {
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalcCharge(source);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * 充电金额联动：充电金额 = 充电电量 × 折合电价。
     * 电价由用户手动填写时，以电价为准重算金额或电量；
     * 电价是自动算出来的（或为空）时，只按「金额 ÷ 电量」刷新电价，不覆盖用户正在填的金额或电量。
     */
    private void recalcCharge(String source) {
        if (updating) return;
        updating = true;
        try {
            EditText amountInput = amountRow.input();
            EditText qtyInput = quantityRow.input();
            EditText priceInput = unitPriceRow.input();
            double amount = Formats.parse(amountInput.getText().toString(), 0);
            double qty = Formats.parse(qtyInput.getText().toString(), 0);
            double price = Formats.parse(priceInput.getText().toString(), 0);
            if (SOURCE_CHARGE_PRICE.equals(source)) {
                chargePriceManual = price > 0.0001;
                if (qty > 0.0001) {
                    amountInput.setText(format(qty * price, 2));
                } else if (amount > 0.0001 && price > 0.0001) {
                    // 先填了充电金额、后填折合电价：以金额为准反推充电电量。
                    qtyInput.setText(format(amount / price, 2));
                }
            } else if (SOURCE_CHARGE_QTY.equals(source)) {
                if (chargePriceManual && price > 0.0001) {
                    amountInput.setText(format(qty * price, 2));
                } else if (qty > 0.0001) {
                    priceInput.setText(format(amount / qty, 2));
                    chargePriceManual = false;
                }
            } else if (SOURCE_CHARGE_AMOUNT.equals(source)) {
                if (chargePriceManual && price > 0.0001) {
                    qtyInput.setText(format(amount / price, 2));
                } else if (qty > 0.0001) {
                    priceInput.setText(format(amount / qty, 2));
                    chargePriceManual = false;
                }
            }
        } finally {
            updating = false;
        }
    }

    private View stationRow() {
        stationRow = new FormRow(c).label("充电站 / 加油站")
                .value(station.isEmpty() ? "请选择" : station)
                .valueColor(station.isEmpty() ? Ui.MUTED : Ui.INK);
        stationRow.click(v -> {
            String[] options = {"车库充", "国家电网", "特来电", "星星充电", "中石化", "中石油", "壳牌", "其他"};
            PickerDialog.choose(c, "选择站点", options, -1, (index, value) -> {
                if ("其他".equals(value)) {
                    PickerDialog.input(c, "自定义站点", "输入站点名称", station, false, text -> {
                        station = text;
                        stationRow.value(station.isEmpty() ? "请选择" : station);
                        stationRow.valueColor(station.isEmpty() ? Ui.MUTED : Ui.INK);
                    });
                } else {
                    station = value;
                    stationRow.value(value);
                    stationRow.valueColor(Ui.INK);
                }
            });
        });
        return stationRow;
    }

    private View previousRow() {
        LinearLayout row = Ui.horizontal(c);
        row.setMinimumHeight(Ui.dp(c, 56));
        LinearLayout left = Ui.horizontal(c);
        left.addView(Ui.medium(c, "上次记录了吗？", 13.5f, Ui.INK_2));
        left.addView(Ui.icon(c, IconView.QUESTION, 14, Ui.MUTED));
        row.addView(left, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout toggle = Ui.horizontal(c);
        previousOff = Ui.medium(c, "没记录", 11.5f, Ui.SUB);
        previousOff.setGravity(Gravity.CENTER);
        previousOff.setPadding(Ui.dp(c, 13), Ui.dp(c, 7), Ui.dp(c, 13), Ui.dp(c, 7));
        previousOn = Ui.medium(c, "记录了", 11.5f, Color.WHITE);
        previousOn.setGravity(Gravity.CENTER);
        previousOn.setPadding(Ui.dp(c, 13), Ui.dp(c, 7), Ui.dp(c, 13), Ui.dp(c, 7));
        toggle.addView(previousOff);
        LinearLayout.LayoutParams onLp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        onLp.leftMargin = Ui.dp(c, 6);
        toggle.addView(previousOn, onLp);
        row.addView(toggle);
        applyPrevious();
        Ui.clickable(previousOff, v -> {
            previousRecorded = false;
            applyPrevious();
        });
        Ui.clickable(previousOn, v -> {
            previousRecorded = true;
            applyPrevious();
        });
        return row;
    }

    private void applyPrevious() {
        previousOff.setTextColor(previousRecorded ? Ui.SUB : Color.WHITE);
        previousOff.setBackground(previousRecorded ? Ui.round(0xFFF4F6F7, Ui.dp(c, 9)) : Ui.round(Ui.GREEN, Ui.dp(c, 9)));
        previousOn.setTextColor(previousRecorded ? Color.WHITE : Ui.SUB);
        previousOn.setBackground(previousRecorded ? Ui.round(Ui.GREEN, Ui.dp(c, 9)) : Ui.round(0xFFF4F6F7, Ui.dp(c, 9)));
    }

    private View noteSection() {
        LinearLayout box = Ui.vertical(c);
        box.setPadding(0, Ui.dp(c, 12), 0, 0);
        noteInput = new EditText(c);
        noteInput.setHint("请编辑备注");
        noteInput.setText(editing == null ? "" : safe(editing.note));
        noteInput.setTextSize(12.5f);
        noteInput.setTextColor(Ui.INK);
        noteInput.setHintTextColor(Ui.MUTED);
        noteInput.setTypeface(Ui.REGULAR);
        noteInput.setBackground(Ui.round(0xFFF4F6F7, Ui.dp(c, 10)));
        noteInput.setPadding(Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
        noteInput.setGravity(Gravity.TOP | Gravity.START);
        noteInput.setMinLines(3);
        noteInput.setMaxLines(6);
        box.addView(noteInput, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, Ui.dp(c, 100)));
        noteCount = Ui.regular(c, (noteInput.getText().length()) + "/1000", 9.5f, Ui.MUTED);
        noteCount.setGravity(Gravity.END);
        noteCount.setPadding(0, Ui.dp(c, 3), Ui.dp(c, 2), 0);
        box.addView(noteCount);
        noteInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 1000) {
                    noteInput.setText(s.subSequence(0, 1000));
                    noteInput.setSelection(1000);
                }
                noteCount.setText(noteInput.getText().length() + "/1000");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        return box;
    }

    private View photoSection() {
        LinearLayout box = Ui.vertical(c);
        box.setPadding(0, Ui.dp(c, 12), 0, 0);
        photoBox = Ui.frame(c);
        photoBox.setBackground(Ui.round(0xFFF4F6F7, Ui.dp(c, 12)));
        photoBox.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(c, 86), Ui.dp(c, 86)));
        box.addView(photoBox);
        Ui.clickable(photoBox, v -> pickPhoto());
        renderPhoto();
        return box;
    }

    private void renderPhoto() {
        if (photoBox == null) return;
        photoBox.removeAllViews();
        File f = new File(photoPath == null ? "" : photoPath);
        if (photoPath != null && !photoPath.isEmpty() && f.exists()) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bm = decodeSampled(f.getAbsolutePath(), 480);
            if (bm != null) iv.setImageBitmap(bm);
            photoBox.addView(iv, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            View close = Ui.icon(c, IconView.CLOSE, 16, Color.WHITE);
            close.setBackground(Ui.circle(0x99000000, Ui.dp(c, 22)));
            close.setPadding(Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4), Ui.dp(c, 4));
            FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(Ui.dp(c, 22), Ui.dp(c, 22));
            clp.gravity = Gravity.TOP | Gravity.END;
            clp.topMargin = Ui.dp(c, 4);
            clp.rightMargin = Ui.dp(c, 4);
            photoBox.addView(close, clp);
            Ui.clickable(close, v -> {
                photoPath = "";
                renderPhoto();
            });
        } else {
            LinearLayout inner = Ui.vertical(c);
            inner.setGravity(Gravity.CENTER);
            inner.addView(Ui.icon(c, IconView.CAMERA, 26, Ui.MUTED));
            TextView photoLabel = Ui.regular(c, "拍照备忘", 9.5f, Ui.MUTED);
            photoLabel.setGravity(Gravity.CENTER);
            photoLabel.setSingleLine(true);
            inner.addView(photoLabel, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            ilp.gravity = Gravity.CENTER;
            photoBox.addView(inner, ilp);
        }
    }

    private void pickPhoto() {
        try {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.setType("image/*");
            activity.startActivityForResult(i, REQ_PHOTO);
        } catch (Exception e) {
            Ui.toast(c, "无法打开相册，请检查系统权限");
        }
    }

    public void onPhotoPicked(Uri uri) {
        if (uri == null) return;
        String path = repo.importPhoto(uri);
        if (path == null) {
            Ui.toast(c, "图片导入失败");
            return;
        }
        photoPath = path;
        renderPhoto();
        Ui.toast(c, "照片已添加");
    }

    private View saveButton() {
        TextView save = Ui.medium(c, editing == null ? "保存记录" : "保存修改", 15, Color.WHITE);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.round(Ui.GREEN, Ui.dp(c, 14)));
        save.setPadding(0, Ui.dp(c, 14), 0, Ui.dp(c, 14));
        Ui.clickable(save, v -> save());
        return save;
    }

    private boolean isEnergy() {
        return EnergyRecord.TYPE_CHARGE.equals(type) || EnergyRecord.TYPE_FUEL.equals(type);
    }

    private void save() {
        if (isEnergy() && odometerRow != null) {
            odometer = Formats.parse(odometerRow.input().getText().toString(), 0);
            if (odometer <= 0) {
                Ui.toast(c, "请填写当前总里程");
                return;
            }
        }
        boolean fuel = EnergyRecord.TYPE_FUEL.equals(type);
        double amount = fuel
                ? (fuelAmountInput == null ? 0 : Formats.parse(fuelAmountInput.getText().toString(), 0))
                : (amountRow == null ? 0 : Formats.parse(amountRow.input().getText().toString(), 0));
        if (amount < 0 || (amount == 0 && !EnergyRecord.TYPE_CHARGE.equals(type))) {
            Ui.toast(c, EnergyRecord.TYPE_INCOME.equals(type) ? "请填写收入金额" : "请填写金额");
            return;
        }
        if (isEnergy()) {
            double qty = fuel
                    ? (fuelQtyInput == null ? 0 : Formats.parse(fuelQtyInput.getText().toString(), 0))
                    : (quantityRow == null ? 0 : Formats.parse(quantityRow.input().getText().toString(), 0));
            if (qty <= 0) {
                Ui.toast(c, EnergyRecord.TYPE_CHARGE.equals(type) ? "请填写充电电量" : "请填写加油量");
                return;
            }
        }
        // 先复制再赋值：校验失败时不会把编辑中的原对象改脏。
        EnergyRecord r = editing != null ? editing.copy() : new EnergyRecord();
        r.vehicleId = vehicle.id;
        r.type = type;
        r.timestamp = timestamp;
        r.note = noteInput == null ? "" : noteInput.getText().toString().trim();
        r.photoPath = photoPath == null ? "" : photoPath;
        r.previousRecorded = previousRecorded;
        r.amount = amount;

        if (EnergyRecord.TYPE_CHARGE.equals(type)) {
            r.odometer = odometer;
            r.quantity = Formats.parse(quantityRow.input().getText().toString(), 0);
            r.unitPrice = Formats.parse(unitPriceRow.input().getText().toString(), 0);
            r.socStart = Formats.parse(socStartRow.input().getText().toString(), 0);
            r.socEnd = Formats.parse(socEndRow.input().getText().toString(), 0);
            if (remainingOilRow == null) {
                r.remainingOil = 0;
            } else {
                String oilText = remainingOilRow.input().getText().toString().trim();
                if (oilText.isEmpty()) {
                    Ui.toast(c, "请填写充电时的剩余油量（油箱余量百分比）");
                    return;
                }
                r.remainingOil = Formats.parse(oilText, 0);
                if (r.remainingOil < 0 || r.remainingOil > 100) {
                    Ui.toast(c, "剩余油量应在 0-100% 之间");
                    return;
                }
            }
            r.station = station;
            if (r.socEnd > 0 && r.socStart >= 0 && r.socEnd <= r.socStart) {
                Ui.toast(c, "结束电量应大于开始电量");
                return;
            }
            if (r.socEnd > 100 || r.socStart > 100) {
                Ui.toast(c, "电量百分比不能超过 100%");
                return;
            }
        } else if (EnergyRecord.TYPE_FUEL.equals(type)) {
            r.odometer = odometer;
            r.quantity = Formats.parse(fuelQtyInput.getText().toString(), 0);
            r.listAmount = Formats.parse(fuelListInput.getText().toString(), 0);
            r.discount = Formats.parse(fuelDiscountInput.getText().toString(), 0);
            r.unitPrice = Formats.parse(fuelActualPriceInput.getText().toString(), 0);
            r.oilBefore = Formats.parse(oilBeforeRow.input().getText().toString(), 0);
            r.oilAfter = Formats.parse(oilAfterRow.input().getText().toString(), 0);
            if (remainingSocRow == null) {
                r.remainingSoc = 0;
            } else {
                String socText = remainingSocRow.input().getText().toString().trim();
                if (socText.isEmpty()) {
                    Ui.toast(c, "请填写加油时的剩余电量（电池余量百分比）");
                    return;
                }
                r.remainingSoc = Formats.parse(socText, 0);
                if (r.remainingSoc < 0 || r.remainingSoc > 100) {
                    Ui.toast(c, "剩余电量应在 0-100% 之间");
                    return;
                }
            }
            if (safe(fuelGrade).isEmpty()) {
                Ui.toast(c, "请选择油品型号");
                return;
            }
            r.fuelType = fuelType;
            r.fuelGrade = fuelGrade;
            r.station = station;
            if (r.oilAfter > 100 || r.oilBefore > 100) {
                Ui.toast(c, "油量百分比不能超过 100%");
                return;
            }
        } else {
            r.category = category;
            if (r.category.isEmpty()) {
                Ui.toast(c, "请选择" + (EnergyRecord.TYPE_INCOME.equals(type) ? "收入" : "支出") + "类型");
                return;
            }
        }
        long savedId = repo.saveRecord(r);
        boolean showTrip = editing == null && isEnergy() && savedId > 0;
        Ui.toast(c, editing == null ? "记录已保存" : "修改已保存");
        activity.onDataChanged();
        activity.pop();
        if (showTrip) {
            // 新增一条充电/加油记录后，直接展示这一段的行程报告。
            activity.push(new TripDetailScreen(activity, savedId));
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(c)
                .setTitle("删除记录")
                .setMessage("删除后不可恢复，确定删除这条记录吗？")
                .setPositiveButton("删除", (d, w) -> {
                    repo.deleteRecord(editing.id);
                    Ui.toast(c, "记录已删除");
                    activity.onDataChanged();
                    activity.pop();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private double lastOdometer() {
        List<EnergyRecord> list = repo.getRecords(vehicle.id);
        double max = 0;
        for (EnergyRecord r : list) if (r.odometer > max) max = r.odometer;
        return max;
    }

    private String format(double v, int digits) {
        if (digits == 0) return String.format(Locale.CHINA, "%.0f", v);
        if (digits == 1) return String.format(Locale.CHINA, "%.1f", v);
        return String.format(Locale.CHINA, "%.2f", v);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    /** 相册原图可能很大，按目标尺寸采样解码，避免 OOM。 */
    private static Bitmap decodeSampled(String path, int targetPx) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bounds);
        int sample = 1;
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        while (longest / sample > targetPx) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(path, opts);
    }
}
