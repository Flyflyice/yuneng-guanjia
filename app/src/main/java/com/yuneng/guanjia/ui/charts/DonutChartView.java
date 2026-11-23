package com.yuneng.guanjia.ui.charts;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.yuneng.guanjia.ui.Ui;

import java.util.ArrayList;
import java.util.List;

public class DonutChartView extends View {
    private final List<Float> values = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private String centerTop = "";
    private String centerBottom = "费用总计";
    private float progress = 1f;
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint center = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public DonutChartView(Context context) {
        super(context);
        init();
    }

    public DonutChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeCap(Paint.Cap.BUTT);
        arc.setStrokeWidth(dp(22));
        center.setColor(Ui.INK);
        center.setTextAlign(Paint.Align.CENTER);
        center.setTypeface(Ui.BLACK);
        center.setTextSize(sp(25));
        sub.setColor(Ui.SUB);
        sub.setTextAlign(Paint.Align.CENTER);
        sub.setTypeface(Ui.REGULAR);
        sub.setTextSize(sp(10.5f));
    }

    public void setData(List<Float> values, List<Integer> colors, String centerTop) {
        this.values.clear();
        this.colors.clear();
        if (values != null) this.values.addAll(values);
        if (colors != null) this.colors.addAll(colors);
        this.centerTop = centerTop == null ? "" : centerTop;
        invalidate();
    }

    public void animateIn() {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(750);
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
        float size = Math.min(w, h) - dp(20);
        float cx = w / 2f, cy = h / 2f;
        float radius = size / 2f;
        arc.setStrokeWidth(Math.min(dp(22), radius * 0.30f));
        rect.set(cx - radius + arc.getStrokeWidth() / 2f, cy - radius + arc.getStrokeWidth() / 2f,
                cx + radius - arc.getStrokeWidth() / 2f, cy + radius - arc.getStrokeWidth() / 2f);

        float total = 0;
        for (Float v : values) total += v == null ? 0 : v;
        if (total <= 0) {
            arc.setColor(0xFFEEF2EF);
            canvas.drawArc(rect, 0, 360, false, arc);
        } else {
            float start = -90;
            for (int i = 0; i < values.size(); i++) {
                float v = values.get(i) == null ? 0 : values.get(i);
                if (v <= 0) continue;
                float sweep = 360f * v / total * progress;
                arc.setColor(colors.size() > i ? colors.get(i) : Ui.GREEN);
                canvas.drawArc(rect, start + dp(1.6f), Math.max(0, sweep - dp(2.2f)), false, arc);
                start += sweep;
            }
        }

        canvas.drawText(centerTop, cx, cy + dp(4), center);
        canvas.drawText(centerBottom, cx, cy + dp(22), sub);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }
}
