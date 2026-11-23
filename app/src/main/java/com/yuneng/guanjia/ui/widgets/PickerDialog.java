package com.yuneng.guanjia.ui.widgets;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.ui.Ui;

import java.util.Calendar;

/** 轻量弹窗工具：列表选择、日期时间、文本输入。 */
public final class PickerDialog {
    private PickerDialog() {
    }

    public interface OnPick {
        void onPick(int index, String value);
    }

    public interface OnText {
        void onText(String text);
    }

    public static void choose(Context c, String title, String[] options, int selected, OnPick pick) {
        AlertDialog.Builder b = new AlertDialog.Builder(c);
        b.setTitle(title);
        b.setSingleChoiceItems(options, selected, null);
        b.setNegativeButton("取消", null);
        AlertDialog d = b.create();
        d.setOnShowListener(dialog -> {
            d.getListView().setOnItemClickListener((parent, view, position, id) -> {
                if (pick != null) pick.onPick(position, options[position]);
                d.dismiss();
            });
        });
        d.show();
        style(d);
    }

    public static void dateTime(Context c, long ts, OnPick pick) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts);
        DatePickerDialog dp = new DatePickerDialog(c, (view, year, month, day) -> {
            Calendar next = Calendar.getInstance();
            next.set(year, month, day, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), 0);
            TimePickerDialog tp = new TimePickerDialog(c, (tv, hour, minute) -> {
                Calendar fin = Calendar.getInstance();
                fin.set(year, month, day, hour, minute, 0);
                if (pick != null) pick.onPick(0, String.valueOf(fin.getTimeInMillis()));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);
            tp.show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    /** 只选日期（不选时间），回调毫秒时间戳（当天 00:00:00）。 */
    public static void date(Context c, String title, long ts, OnPick pick) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts);
        DatePickerDialog dp = new DatePickerDialog(c, (view, year, month, day) -> {
            Calendar fin = Calendar.getInstance();
            fin.set(year, month, day, 0, 0, 0);
            fin.set(Calendar.MILLISECOND, 0);
            if (pick != null) pick.onPick(0, String.valueOf(fin.getTimeInMillis()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        if (title != null && !title.isEmpty()) dp.setTitle(title);
        dp.show();
    }

    public static void input(Context c, String title, String hint, String value, boolean number, OnText done) {
        LinearLayout box = Ui.vertical(c);
        box.setPadding(Ui.dp(c, 22), Ui.dp(c, 10), Ui.dp(c, 22), 0);
        EditText e = Ui.edit(c, hint, 15);
        e.setText(value == null ? "" : value);
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        e.setPadding(Ui.dp(c, 12), 0, Ui.dp(c, 12), 0);
        e.setInputType(number ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT);
        box.addView(e, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 46)));
        AlertDialog d = new AlertDialog.Builder(c)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("确定", (dialog, which) -> {
                    if (done != null) done.onText(e.getText().toString().trim());
                })
                .setNegativeButton("取消", null)
                .create();
        d.setOnShowListener(dialog -> {
            e.requestFocus();
            Window w = d.getWindow();
            if (w != null) w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        });
        d.show();
        style(d);
    }

    private static void style(AlertDialog d) {
        Window w = d.getWindow();
        if (w == null) return;
        // 保留系统对话框的白色面板，只调整遮罩浓度
        w.setDimAmount(0.45f);
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    /** 显示一个简单的信息对话框。 */
    public static void info(Context c, String title, String message) {
        AlertDialog d = new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .create();
        d.show();
        style(d);
    }
}
