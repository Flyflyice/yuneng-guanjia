package com.yuneng.guanjia.ui.charts;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.yuneng.guanjia.ui.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarChartView extends View {
    private final List<String> labels = new ArrayList<>();
    private final List<Float> values = new ArrayList<>();
    private float progress = 1f;
    private int selected = -1;
    private float density;
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barBack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint avg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubble = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        grid.setColor(0xFFEDF0F2);
        grid.setStrokeWidth(dp(1));
        grid.setStyle(Paint.Style.STROKE);
        grid.setPathEffect(new DashPathEffect(new float[]{dp(3), dp(4)}, 0));
        text.setColor(0xFF9AA1A9);
        text.setTextSize(sp(9.5f));
        text.setTypeface(Ui.REGULAR);
        bar.setStyle(Paint.Style.FILL);
        barBack.setStyle(Paint.Style.FILL);
        barBack.setColor(0xFFF0F4F1);
        avg.setColor(0xFFFF6B62);
        avg.setStyle(Paint.Style.STROKE);
        avg.setStrokeWidth(dp(1));
        avg.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));
        bubble.setStyle(Paint.Style.FILL);
        bubble.setColor(0xFF25302A);
        bubble.setShadowLayer(dp(5), 0, dp(2), 0x33000000);
    }

    public void setData(List<String> labels, List<Float> values) {
        this.labels.clear();
        this.values.clear();
        if (labels != null) this.labels.addAll(labels);
        if (values != null) this.values.addAll(values);
        selected = -1;
        invalidate();
    }

    public void animateIn() {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(700);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(v -> {
            progress = (float) v.getAnimatedValue();
            invalidate();
        });
        a.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (values.isEmpty()) {
            text.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("暂无支出数据", w / 2f, h / 2f, text);
            return;
        }
        float left = dp(38), right = w - dp(8), top = dp(8), bottom = h - dp(23);
        float max = 0;
        double total = 0;
        for (Float v : values) {
            if (v != null && v > max) max = v;
            if (v != null) total += v;
        }
        if (max <= 0) max = 1;
        max = niceMax(max);
        int lines = 4;
        text.setTextAlign(Paint.Align.RIGHT);
        text.setTextSize(sp(9.2f));
        for (int i = 0; i <= lines; i++) {
            float y = bottom - (bottom - top) * i / lines;
            if (i > 0) canvas.drawLine(left, y, right, y, grid);
            float v = max * i / lines;
            canvas.drawText(shortMoney(v), left - dp(5), y + dp(3), text);
        }

        int n = values.size();
        float slot = (right - left) / n;
        float bw = Math.min(dp(15), slot * 0.56f);
        for (int i = 0; i < n; i++) {
            float cx = left + slot * i + slot / 2f;
            rect.set(cx - bw / 2f, top, cx + bw / 2f, bottom);
            canvas.drawRoundRect(rect, bw / 2f, bw / 2f, barBack);
            float v = values.get(i) == null ? 0 : values.get(i);
            float bh = (bottom - top) * (v / max) * progress;
            if (bh > dp(1)) {
                rect.set(cx - bw / 2f, bottom - bh, cx + bw / 2f, bottom);
                bar.setShader(new LinearGradient(0, bottom - bh, 0, bottom,
                        Ui.GREEN, Ui.GREEN_MID, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(rect, bw / 2f, bw / 2f, bar);
            }
        }

        float avgValue = (float) (total / n);
        if (avgValue > 0) {
            float y = bottom - (bottom - top) * (avgValue / max) * progress;
            path.reset();
            path.moveTo(left, y);
            path.lineTo(right, y);
            canvas.drawPath(path, avg);
            text.setTextAlign(Paint.Align.RIGHT);
            text.setColor(0xFFFF6B62);
            canvas.drawText("均 " + shortMoney(avgValue), right, y - dp(4), text);
            text.setColor(0xFF9AA1A9);
        }

        text.setTextAlign(Paint.Align.CENTER);
        // 柱子较多时均匀抽稀横轴标签（约 6 个），首尾一定保留
        int labelCount = Math.min(n, 6);
        for (int i = 0; i < labelCount; i++) {
            int idx = Math.round(i * (n - 1f) / Math.max(1, labelCount - 1));
            String label = idx < labels.size() ? labels.get(idx) : "";
            float cx = left + slot * idx + slot / 2f;
            canvas.drawText(label == null ? "" : label, cx, h - dp(5), text);
        }

        if (selected >= 0 && selected < n) {
            float cx = left + slot * selected + slot / 2f;
            float v = values.get(selected) == null ? 0 : values.get(selected);
            float bh = (bottom - top) * (v / max);
            drawBubble(canvas, cx, bottom - bh, v, w);
        }
    }

    private void drawBubble(Canvas c, float x, float y, float value, int width) {
        String s = "¥" + String.format(Locale.CHINA, "%.2f", value);
        float pad = dp(9);
        float tw = text.measureText(s);
        float bw = tw + pad * 2;
        float bh = dp(26);
        float bx = Math.max(dp(3), Math.min(width - bw - dp(3), x - bw / 2));
        float by = y - bh - dp(9);
        if (by < dp(2)) by = y + dp(8);
        rect.set(bx, by, bx + bw, by + bh);
        c.drawRoundRect(rect, dp(8), dp(8), bubble);
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        c.drawText(s, bx + bw / 2, by + bh / 2 + dp(3.4f), text);
        text.setColor(0xFF9AA1A9);
        text.setTextSize(sp(9.2f));
    }

    private String shortMoney(float v) {
        if (v >= 10000) return String.format(Locale.CHINA, "%.1f万", v / 10000);
        if (v >= 1000) return String.format(Locale.CHINA, "%.0f", v);
        return String.format(Locale.CHINA, "%.0f", v);
    }

    private float niceMax(float v) {
        if (v <= 10) return 10;
        double exp = Math.floor(Math.log10(v));
        double base = Math.pow(10, exp);
        double f = v / base;
        double nice;
        if (f <= 1) nice = 1;
        else if (f <= 2) nice = 2;
        else if (f <= 2.5) nice = 2.5;
        else if (f <= 5) nice = 5;
        else nice = 10;
        return (float) (nice * base);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (values.isEmpty()) return super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            // 让父 ScrollView 可以接管纵向滑动，轻触仍显示数值气泡
            float left = dp(38), right = getWidth() - dp(8);
            float slot = (right - left) / values.size();
            int i = (int) ((event.getX() - left) / slot);
            selected = Math.max(0, Math.min(values.size() - 1, i));
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        return super.onTouchEvent(event);
    }

    private float dp(float v) {
        return v * density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }
}
