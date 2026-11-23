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

public class LineChartView extends View {
    private final List<String> labels = new ArrayList<>();
    private final List<Float> values = new ArrayList<>();
    private int lineColor = Ui.GREEN;
    private String unit = "";
    private int decimals = 2;
    private float progress = 1f;
    private int selected = -1;
    private float density;
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubble = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Path areaPath = new Path();
    private final RectF tmp = new RectF();

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        grid.setColor(0xFFE8ECEF);
        grid.setStrokeWidth(dp(1));
        grid.setStyle(Paint.Style.STROKE);
        grid.setPathEffect(new DashPathEffect(new float[]{dp(3), dp(4)}, 0));
        text.setColor(0xFF9AA1A9);
        text.setTextSize(sp(9.5f));
        text.setTypeface(Ui.REGULAR);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(dp(2.2f));
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setColor(lineColor);
        point.setStyle(Paint.Style.FILL);
        point.setColor(Color.WHITE);
        bubble.setStyle(Paint.Style.FILL);
        bubble.setColor(0xFF25302A);
        bubble.setShadowLayer(dp(5), 0, dp(2), 0x33000000);
    }

    public void setData(List<String> labels, List<Float> values) {
        this.labels.clear();
        this.values.clear();
        if (labels != null) this.labels.addAll(labels);
        if (values != null) this.values.addAll(values);
        this.selected = this.values.isEmpty() ? -1 : this.values.size() - 1;
        invalidate();
    }

    public void setLineColor(int color) {
        lineColor = color;
        line.setColor(color);
        invalidate();
    }

    public void setUnit(String unit) {
        this.unit = unit == null ? "" : unit;
        invalidate();
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
        invalidate();
    }

    public void animateIn() {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(650);
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
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (values.size() < 2) {
            text.setTextAlign(Paint.Align.CENTER);
            text.setTextSize(sp(12));
            canvas.drawText("暂无趋势数据", w / 2f, h / 2f, text);
            return;
        }
        float left = dp(36);
        float right = w - dp(8);
        float top = dp(12);
        float bottom = h - dp(22);

        float max = 0;
        for (Float v : values) if (v != null && v > max) max = v;
        if (max <= 0) max = 1;
        max = niceMax(max);
        int gridLines = 4;
        text.setTextAlign(Paint.Align.RIGHT);
        text.setTextSize(sp(9.5f));
        for (int i = 0; i <= gridLines; i++) {
            float y = bottom - (bottom - top) * i / gridLines;
            if (i > 0) canvas.drawLine(left, y, right, y, grid);
            float val = max * i / gridLines;
            String s = format(val);
            canvas.drawText(s, left - dp(6), y + dp(3), text);
        }

        // 折线坐标
        int n = values.size();
        float[] xs = new float[n];
        float[] ys = new float[n];
        float step = n > 1 ? (right - left) / (n - 1) : 0;
        for (int i = 0; i < n; i++) {
            xs[i] = left + step * i;
            float v = values.get(i) == null ? 0 : values.get(i);
            ys[i] = bottom - (bottom - top) * (v / max) * progress;
        }

        path.reset();
        areaPath.reset();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            float midX = (xs[i - 1] + xs[i]) / 2f;
            path.cubicTo(midX, ys[i - 1], midX, ys[i], xs[i], ys[i]);
        }
        areaPath.addPath(path);
        areaPath.lineTo(xs[n - 1], bottom);
        areaPath.lineTo(xs[0], bottom);
        areaPath.close();
        fill.setShader(new LinearGradient(0, top, 0, bottom,
                withAlpha(lineColor, 56), withAlpha(lineColor, 2), Shader.TileMode.CLAMP));
        fill.setStyle(Paint.Style.FILL);
        canvas.drawPath(areaPath, fill);
        canvas.drawPath(path, line);

        // x 轴标签
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(sp(9.2f));
        int labelCount = n <= 7 ? n : 6;
        for (int i = 0; i < labelCount; i++) {
            int idx = Math.round(i * (n - 1f) / Math.max(1, labelCount - 1));
            String label = idx < labels.size() ? labels.get(idx) : "";
            if (label == null || label.isEmpty()) continue;
            float tw = text.measureText(label);
            float cx = xs[idx];
            if (cx - tw / 2f < dp(2)) cx = dp(2) + tw / 2f;
            if (cx + tw / 2f > w - dp(2)) cx = w - dp(2) - tw / 2f;
            canvas.drawText(label, cx, h - dp(4), text);
        }

        // 数据点（只画最后、首和选中点）
        drawDot(canvas, xs[n - 1], ys[n - 1], lineColor);
        if (n > 2) drawDot(canvas, xs[0], ys[0], withAlpha(lineColor, 190));
        if (selected >= 0 && selected < n) {
            drawDot(canvas, xs[selected], ys[selected], lineColor);
            drawBubble(canvas, xs[selected], ys[selected], values.get(selected), w);
        }
    }

    private void drawDot(Canvas c, float x, float y, int color) {
        point.setColor(Color.WHITE);
        c.drawCircle(x, y, dp(4.2f), point);
        point.setColor(color);
        c.drawCircle(x, y, dp(2.5f), point);
    }

    private void drawBubble(Canvas c, float x, float y, float value, int width) {
        String val = format(value) + (unit.isEmpty() ? "" : " " + unit);
        float pad = dp(9);
        float tw = text.measureText(val);
        float bw = tw + pad * 2;
        float bh = dp(26);
        float bx = Math.max(dp(4), Math.min(width - bw - dp(4), x - bw / 2));
        float by = y - bh - dp(12);
        if (by < dp(2)) by = y + dp(12);
        tmp.set(bx, by, bx + bw, by + bh);
        c.drawRoundRect(tmp, dp(8), dp(8), bubble);
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(sp(10));
        c.drawText(val, bx + bw / 2, by + bh / 2 + dp(3.4f), text);
        text.setColor(0xFF9AA1A9);
    }

    private String format(float v) {
        if (decimals <= 0) return String.format(Locale.CHINA, "%.0f", v);
        if (decimals == 1) return String.format(Locale.CHINA, "%.1f", v);
        return String.format(Locale.CHINA, "%.2f", v);
    }

    private float niceMax(float v) {
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
        if (values.size() < 2) return super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            // 不要拦截父 ScrollView 的纵向滚动；仅轻触时显示气泡
            selected = nearest(event.getX());
            invalidate();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            getParent().requestDisallowInterceptTouchEvent(false);
            return true;
        }
        return super.onTouchEvent(event);
    }

    private int nearest(float x) {
        float left = dp(36);
        float right = getWidth() - dp(8);
        int n = values.size();
        float step = n > 1 ? (right - left) / (n - 1) : 0;
        if (step <= 0) return n - 1;
        int i = Math.round((x - left) / step);
        return Math.max(0, Math.min(n - 1, i));
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float v) {
        return v * density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }
}
