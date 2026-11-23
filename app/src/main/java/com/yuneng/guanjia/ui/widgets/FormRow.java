package com.yuneng.guanjia.ui.widgets;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.ui.Ui;

/**
 * 表单行：左侧标题（可带必填星号），右侧值 / 输入框 / 单位 / 箭头。
 */
public class FormRow extends LinearLayout {
    private final TextView label;
    private final TextView value;
    private final TextView unit;
    private final IconView arrow;
    private final LinearLayout right;
    private EditText input;
    private View custom;
    private boolean required;

    public FormRow(Context c) {
        super(c);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setMinimumHeight(Ui.dp(c, 56));
        setPadding(0, Ui.dp(c, 4), 0, Ui.dp(c, 4));

        LinearLayout left = Ui.horizontal(c);
        label = Ui.medium(c, "", 13.5f, Ui.INK_2);
        left.addView(label);
        TextView star = Ui.regular(c, " *", 13.5f, Ui.RED);
        star.setVisibility(GONE);
        left.addView(star);
        addView(left, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        right = Ui.horizontal(c);
        right.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        value = Ui.medium(c, "", 13.5f, Ui.INK);
        value.setGravity(Gravity.END);
        right.addView(value);
        unit = Ui.regular(c, "", 11, Ui.SUB);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        ulp.leftMargin = Ui.dp(c, 4);
        right.addView(unit, ulp);
        arrow = Ui.icon(c, IconView.CHEVRON_RIGHT, 17, Ui.MUTED);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(Ui.dp(c, 17), Ui.dp(c, 17));
        alp.leftMargin = Ui.dp(c, 2);
        right.addView(arrow, alp);
        addView(right, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        this.star = star;
    }

    private final TextView star;

    public FormRow label(String s) {
        label.setText(s);
        return this;
    }

    public FormRow required() {
        required = true;
        star.setVisibility(VISIBLE);
        return this;
    }

    public FormRow value(String s) {
        value.setText(s == null ? "" : s);
        value.setVisibility(VISIBLE);
        return this;
    }

    public FormRow valueColor(int color) {
        value.setTextColor(color);
        return this;
    }

    public FormRow unit(String s) {
        unit.setText(s == null ? "" : s);
        return this;
    }

    public FormRow noArrow() {
        arrow.setVisibility(GONE);
        return this;
    }

    public FormRow input(String hint, String text, int inputType) {
        value.setVisibility(GONE);
        input = Ui.edit(getContext(), hint, 13.5f);
        input.setText(text == null ? "" : text);
        input.setInputType(inputType);
        input.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(getContext(), 122), Ui.dp(getContext(), 40));
        right.removeView(unit);
        right.removeView(arrow);
        right.addView(input, lp);
        right.addView(unit);
        right.addView(arrow);
        return this;
    }

    public FormRow inputWidth(float dp) {
        if (input != null) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) input.getLayoutParams();
            lp.width = Ui.dp(getContext(), dp);
            input.setLayoutParams(lp);
        }
        return this;
    }

    public FormRow readOnly(String text) {
        value.setVisibility(GONE);
        if (input == null) input = Ui.edit(getContext(), "", 13.5f);
        input.setText(text == null ? "" : text);
        input.setEnabled(false);
        input.setTextColor(Ui.INK);
        input.setBackground(Ui.inputBackgroundReadOnly(getContext()));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(getContext(), 122), Ui.dp(getContext(), 40));
        right.removeView(unit);
        right.removeView(arrow);
        right.addView(input, lp);
        right.addView(unit);
        right.addView(arrow);
        return this;
    }

    public FormRow custom(View v) {
        value.setVisibility(GONE);
        custom = v;
        right.addView(v, right.indexOfChild(arrow));
        return this;
    }

    public FormRow click(View.OnClickListener l) {
        setClickable(true);
        setFocusable(true);
        setOnClickListener(l);
        Ui.ripple(this, 0x10000000);
        return this;
    }

    public EditText input() {
        return input;
    }

    public String text() {
        if (input != null) return input.getText().toString().trim();
        return value.getText().toString().trim();
    }

    public TextView valueView() {
        return value;
    }

    public boolean isRequired() {
        return required;
    }
}
