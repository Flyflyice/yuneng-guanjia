package com.yuneng.guanjia.ui.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 轻量矢量图标。全部用 Canvas 绘制，避免依赖网络与第三方图标库。
 * 坐标系统一为 24x24，自适应 View 尺寸。
 */
public class IconView extends View {
    public static final int LEAF = 1;
    public static final int RECEIPT = 2;
    public static final int CHART = 3;
    public static final int PERSON = 4;
    public static final int PLUS = 5;
    public static final int CLOSE = 6;
    public static final int BELL = 7;
    public static final int BOOK = 8;
    public static final int CHEVRON_LEFT = 9;
    public static final int CHEVRON_RIGHT = 10;
    public static final int ARROW_RIGHT = 11;
    public static final int CAR = 12;
    public static final int BOLT = 13;
    public static final int FUEL = 14;
    public static final int CAMERA = 15;
    public static final int LOCATION = 16;
    public static final int CALENDAR = 17;
    public static final int EDIT = 18;
    public static final int TRASH = 19;
    public static final int INCOME = 20;
    public static final int EXPENSE = 21;
    public static final int CHECK = 22;
    public static final int SETTINGS = 23;
    public static final int SHARE = 24;
    public static final int DOCUMENT = 25;
    public static final int BATTERY = 26;
    public static final int MONEY = 27;
    public static final int SPEED = 28;
    public static final int DROP = 29;
    public static final int QUESTION = 30;
    public static final int BACKUP = 31;
    public static final int DOWNLOAD = 32;
    public static final int INFO = 33;
    public static final int STAR = 34;
    public static final int FILTER = 35;
    public static final int SWAP = 36;
    public static final int MORE = 37;
    public static final int TANK = 38;
    public static final int ROAD = 39;

    private int icon = LEAF;
    private int color = Color.BLACK;
    private boolean filled = false;
    private float stroke = 2.0f;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();

    public IconView(Context context) {
        super(context);
    }

    public IconView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IconView icon(int icon) {
        this.icon = icon;
        invalidate();
        return this;
    }

    public IconView color(int color) {
        this.color = color;
        invalidate();
        return this;
    }

    public IconView filled(boolean filled) {
        this.filled = filled;
        invalidate();
        return this;
    }

    public IconView stroke(float stroke) {
        this.stroke = stroke;
        invalidate();
        return this;
    }

    public int getIcon() {
        return icon;
    }

    public int getColor() {
        return color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        if (size <= 0) return;
        float s = size / 24f;
        canvas.save();
        canvas.translate((getWidth() - size) / 2f, (getHeight() - size) / 2f);
        canvas.scale(s, s);
        paint.setColor(color);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(stroke);
        paint.setStyle(filled ? Paint.Style.FILL : Paint.Style.STROKE);
        path.reset();
        drawIcon(canvas);
        canvas.restore();
    }

    private void drawIcon(Canvas c) {
        switch (icon) {
            case LEAF:
                path.moveTo(20, 4);
                path.cubicTo(12, 3, 5, 7, 4, 14);
                path.cubicTo(3.4f, 18.5f, 7, 21, 11, 20.4f);
                path.cubicTo(18, 19.4f, 21, 12, 20, 4);
                path.close();
                if (filled) {
                    paint.setStyle(Paint.Style.FILL);
                    c.drawPath(path, paint);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.WHITE);
                    paint.setStrokeWidth(1.5f);
                    line(c, 7, 18, 17, 7);
                } else {
                    c.drawPath(path, paint);
                    line(c, 7, 18, 17, 7);
                }
                break;
            case RECEIPT:
                path.moveTo(6, 3);
                path.lineTo(18, 3);
                path.lineTo(18, 21);
                path.lineTo(15, 19.4f);
                path.lineTo(12, 21);
                path.lineTo(9, 19.4f);
                path.lineTo(6, 21);
                path.close();
                c.drawPath(path, paint);
                line(c, 9, 8, 15, 8);
                line(c, 9, 12, 15, 12);
                line(c, 9, 16, 13, 16);
                break;
            case CHART:
                line(c, 4, 3.5f, 4, 20);
                line(c, 4, 20, 20.5f, 20);
                bar(c, 7, 13, 9.5f, 20);
                bar(c, 11.5f, 9, 14, 20);
                bar(c, 16, 5, 18.5f, 20);
                break;
            case PERSON:
                c.drawCircle(12, 8, 3.6f, paint);
                path.moveTo(4.5f, 20.5f);
                path.cubicTo(5, 15.4f, 8, 13.2f, 12, 13.2f);
                path.cubicTo(16, 13.2f, 19, 15.4f, 19.5f, 20.5f);
                c.drawPath(path, paint);
                break;
            case PLUS:
                line(c, 12, 5, 12, 19);
                line(c, 5, 12, 19, 12);
                break;
            case CLOSE:
                line(c, 6, 6, 18, 18);
                line(c, 18, 6, 6, 18);
                break;
            case BELL:
                path.moveTo(6, 17);
                path.lineTo(6, 11);
                path.cubicTo(6, 7.2f, 8.5f, 4.5f, 12, 4.5f);
                path.cubicTo(15.5f, 4.5f, 18, 7.2f, 18, 11);
                path.lineTo(18, 17);
                path.lineTo(20, 18.5f);
                path.lineTo(4, 18.5f);
                path.close();
                c.drawPath(path, paint);
                path.moveTo(10, 21);
                path.cubicTo(10.5f, 22, 13.5f, 22, 14, 21);
                c.drawPath(path, paint);
                break;
            case BOOK:
                path.moveTo(4, 5);
                path.cubicTo(7, 4, 10, 5, 12, 7);
                path.lineTo(12, 20);
                path.cubicTo(10, 18, 7, 17, 4, 18);
                path.close();
                c.drawPath(path, paint);
                path.moveTo(20, 5);
                path.cubicTo(17, 4, 14, 5, 12, 7);
                path.lineTo(12, 20);
                path.cubicTo(14, 18, 17, 17, 20, 18);
                path.close();
                c.drawPath(path, paint);
                break;
            case CHEVRON_LEFT:
                line(c, 15, 5, 8, 12);
                line(c, 8, 12, 15, 19);
                break;
            case CHEVRON_RIGHT:
                line(c, 9, 5, 16, 12);
                line(c, 16, 12, 9, 19);
                break;
            case ARROW_RIGHT:
                line(c, 4, 12, 19, 12);
                line(c, 14, 7, 19, 12);
                line(c, 19, 12, 14, 17);
                break;
            case CAR:
                path.moveTo(4, 15);
                path.lineTo(5, 10.5f);
                path.cubicTo(5.5f, 8.4f, 7, 7.5f, 9, 7.5f);
                path.lineTo(15, 7.5f);
                path.cubicTo(17, 7.5f, 18.5f, 8.4f, 19, 10.5f);
                path.lineTo(20, 15);
                path.lineTo(20, 18);
                path.lineTo(18, 18);
                path.lineTo(18, 16.5f);
                path.lineTo(6, 16.5f);
                path.lineTo(6, 18);
                path.lineTo(4, 18);
                path.close();
                c.drawPath(path, paint);
                c.drawCircle(7.5f, 16.5f, 1.4f, paint);
                c.drawCircle(16.5f, 16.5f, 1.4f, paint);
                line(c, 6, 12.5f, 18, 12.5f);
                break;
            case BOLT:
                path.moveTo(13, 2.5f);
                path.lineTo(5.5f, 13);
                path.lineTo(11, 13);
                path.lineTo(10, 21.5f);
                path.lineTo(18.5f, 10.5f);
                path.lineTo(12.8f, 10.5f);
                path.close();
                paint.setStyle(filled ? Paint.Style.FILL : Paint.Style.FILL);
                c.drawPath(path, paint);
                break;
            case FUEL:
                rect.set(4.5f, 4, 13, 20);
                c.drawRoundRect(rect, 1.2f, 1.2f, paint);
                line(c, 7, 7, 10.5f, 7);
                line(c, 7, 10, 10.5f, 10);
                path.moveTo(13, 7);
                path.lineTo(17, 7);
                path.lineTo(17, 17);
                path.cubicTo(17, 19, 19, 19, 19.5f, 17.5f);
                path.lineTo(20, 11);
                c.drawPath(path, paint);
                line(c, 17, 11, 20, 11);
                break;
            case CAMERA:
                rect.set(3.5f, 7, 20.5f, 20);
                c.drawRoundRect(rect, 2.5f, 2.5f, paint);
                path.moveTo(8, 7);
                path.lineTo(9.5f, 4.5f);
                path.lineTo(14.5f, 4.5f);
                path.lineTo(16, 7);
                c.drawPath(path, paint);
                c.drawCircle(12, 13.5f, 3.6f, paint);
                break;
            case LOCATION:
                path.moveTo(12, 21);
                path.cubicTo(8, 16, 5.5f, 13, 5.5f, 9.5f);
                path.cubicTo(5.5f, 5.8f, 8.4f, 3, 12, 3);
                path.cubicTo(15.6f, 3, 18.5f, 5.8f, 18.5f, 9.5f);
                path.cubicTo(18.5f, 13, 16, 16, 12, 21);
                path.close();
                c.drawPath(path, paint);
                c.drawCircle(12, 9.5f, 2.3f, paint);
                break;
            case CALENDAR:
                rect.set(4, 5.5f, 20, 20.5f);
                c.drawRoundRect(rect, 2, 2, paint);
                line(c, 4, 10, 20, 10);
                line(c, 8, 3.5f, 8, 7);
                line(c, 16, 3.5f, 16, 7);
                c.drawCircle(9, 14, 0.9f, fillPaint());
                c.drawCircle(12, 14, 0.9f, fillPaint());
                c.drawCircle(15, 14, 0.9f, fillPaint());
                c.drawCircle(9, 17.5f, 0.9f, fillPaint());
                c.drawCircle(12, 17.5f, 0.9f, fillPaint());
                break;
            case EDIT:
                path.moveTo(4, 20);
                path.lineTo(5.5f, 15);
                path.lineTo(16, 4.5f);
                path.cubicTo(17, 3.5f, 18.5f, 3.5f, 19.5f, 4.5f);
                path.cubicTo(20.5f, 5.5f, 20.5f, 7, 19.5f, 8);
                path.lineTo(9, 18.5f);
                path.close();
                c.drawPath(path, paint);
                line(c, 15, 5.5f, 18.5f, 9);
                break;
            case TRASH:
                line(c, 4, 7, 20, 7);
                line(c, 9, 7, 9.5f, 4);
                line(c, 9.5f, 4, 14.5f, 4);
                line(c, 14.5f, 4, 15, 7);
                path.moveTo(6, 7);
                path.lineTo(7, 20);
                path.lineTo(17, 20);
                path.lineTo(18, 7);
                c.drawPath(path, paint);
                line(c, 10, 11, 10, 17);
                line(c, 14, 11, 14, 17);
                break;
            case INCOME:
            case EXPENSE:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(1.6f);
                c.drawCircle(12, 12, 8.8f, paint);
                line(c, 8, 9, 12, 13);
                line(c, 12, 13, 16, 9);
                line(c, 12, 12, 12, 19);
                if (icon == INCOME) {
                    line(c, 9, 15, 15, 15);
                    line(c, 9, 18, 15, 18);
                    line(c, 18.5f, 5.5f, 21.5f, 5.5f);
                    line(c, 20, 4, 20, 7);
                } else {
                    line(c, 9, 15, 15, 15);
                    line(c, 18.5f, 5.5f, 21.5f, 5.5f);
                }
                paint.setColor(color);
                break;
            case CHECK:
                line(c, 4.5f, 12.5f, 9.5f, 17.5f);
                line(c, 9.5f, 17.5f, 20, 6.5f);
                break;
            case SETTINGS:
                c.drawCircle(12, 12, 3.2f, paint);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * 2 * i / 8;
                    float x1 = (float) (12 + Math.cos(a) * 6.6);
                    float y1 = (float) (12 + Math.sin(a) * 6.6);
                    float x2 = (float) (12 + Math.cos(a) * 9);
                    float y2 = (float) (12 + Math.sin(a) * 9);
                    line(c, x1, y1, x2, y2);
                }
                break;
            case SHARE:
                c.drawCircle(18, 5.5f, 2.4f, paint);
                c.drawCircle(6, 12, 2.4f, paint);
                c.drawCircle(18, 18.5f, 2.4f, paint);
                line(c, 8.2f, 10.8f, 15.8f, 6.7f);
                line(c, 8.2f, 13.2f, 15.8f, 17.3f);
                break;
            case DOCUMENT:
                path.moveTo(6, 3);
                path.lineTo(14, 3);
                path.lineTo(19, 8);
                path.lineTo(19, 21);
                path.lineTo(6, 21);
                path.close();
                c.drawPath(path, paint);
                line(c, 14, 3, 14, 8);
                line(c, 14, 8, 19, 8);
                line(c, 9, 12, 16, 12);
                line(c, 9, 16, 16, 16);
                break;
            case BATTERY:
                rect.set(3, 7, 19, 18);
                c.drawRoundRect(rect, 2, 2, paint);
                rect.set(19, 10.5f, 21.5f, 14.5f);
                c.drawRoundRect(rect, 1, 1, paint);
                paint.setStyle(Paint.Style.FILL);
                rect.set(5.5f, 9.3f, 13, 15.7f);
                c.drawRoundRect(rect, 1, 1, paint);
                break;
            case MONEY:
                c.drawCircle(12, 12, 9.2f, paint);
                line(c, 8, 8.5f, 12, 12);
                line(c, 12, 12, 16, 8.5f);
                line(c, 12, 11.5f, 12, 18);
                line(c, 9, 14, 15, 14);
                line(c, 9, 16.8f, 15, 16.8f);
                break;
            case SPEED:
                path.addArc(3.5f, 5, 20.5f, 22, 150, 240);
                c.drawPath(path, paint);
                line(c, 12, 13, 16.5f, 8.5f);
                c.drawCircle(12, 13, 1.6f, fillPaint());
                break;
            case DROP:
                path.moveTo(12, 3);
                path.cubicTo(9, 7, 6, 10.5f, 6, 14);
                path.cubicTo(6, 17.6f, 8.7f, 20.5f, 12, 20.5f);
                path.cubicTo(15.3f, 20.5f, 18, 17.6f, 18, 14);
                path.cubicTo(18, 10.5f, 15, 7, 12, 3);
                c.drawPath(path, paint);
                break;
            case QUESTION:
                c.drawCircle(12, 12, 9.2f, paint);
                path.moveTo(9, 9.2f);
                path.cubicTo(9, 6.3f, 14.8f, 6.0f, 15, 9.6f);
                path.cubicTo(15.1f, 11.7f, 12, 11.8f, 12, 14.2f);
                c.drawPath(path, paint);
                c.drawCircle(12, 17.2f, 1, fillPaint());
                break;
            case BACKUP:
                path.moveTo(7.5f, 18);
                path.cubicTo(4, 18, 2.5f, 15.5f, 3, 13);
                path.cubicTo(3.5f, 10.5f, 5.5f, 9.4f, 7.4f, 9.6f);
                path.cubicTo(8.2f, 5.8f, 11.2f, 3.5f, 14.2f, 4.2f);
                path.cubicTo(17.2f, 4.9f, 19.2f, 7.4f, 19.2f, 10.2f);
                path.cubicTo(21.5f, 10.5f, 22, 13.4f, 20.7f, 15.2f);
                path.cubicTo(19.8f, 16.5f, 18.4f, 17, 17, 17);
                c.drawPath(path, paint);
                line(c, 12, 19.5f, 12, 12.5f);
                line(c, 9.2f, 15.2f, 12, 12.5f);
                line(c, 14.8f, 15.2f, 12, 12.5f);
                break;
            case DOWNLOAD:
                line(c, 12, 3, 12, 15);
                line(c, 7.5f, 10.5f, 12, 15);
                line(c, 12, 15, 16.5f, 10.5f);
                line(c, 4, 18, 4, 21);
                line(c, 4, 21, 20, 21);
                line(c, 20, 21, 20, 18);
                break;
            case INFO:
                c.drawCircle(12, 12, 9.2f, paint);
                line(c, 12, 10.5f, 12, 17);
                c.drawCircle(12, 7.2f, 1.05f, fillPaint());
                break;
            case STAR:
                path.moveTo(12, 3);
                for (int i = 1; i < 10; i++) {
                    double a = -Math.PI / 2 + Math.PI * 2 * i / 10;
                    double rad = i % 2 == 0 ? 4.3 : 9.2;
                    path.lineTo((float) (12 + Math.cos(a) * rad), (float) (12 + Math.sin(a) * rad));
                }
                path.close();
                c.drawPath(path, paint);
                break;
            case FILTER:
                line(c, 4, 7, 20, 7);
                line(c, 4, 12, 20, 12);
                line(c, 4, 17, 20, 17);
                c.drawCircle(8, 7, 1.9f, fillPaint());
                c.drawCircle(15, 12, 1.9f, fillPaint());
                c.drawCircle(10, 17, 1.9f, fillPaint());
                break;
            case SWAP:
                line(c, 4, 8, 19, 8);
                line(c, 15, 4, 19, 8);
                line(c, 19, 8, 15, 12);
                line(c, 20, 16, 5, 16);
                line(c, 9, 12, 5, 16);
                line(c, 5, 16, 9, 20);
                break;
            case MORE:
                c.drawCircle(5, 12, 1.6f, fillPaint());
                c.drawCircle(12, 12, 1.6f, fillPaint());
                c.drawCircle(19, 12, 1.6f, fillPaint());
                break;
            case TANK:
                rect.set(4, 8, 20, 20);
                c.drawRoundRect(rect, 2.5f, 2.5f, paint);
                path.moveTo(8, 8);
                path.lineTo(10, 3.5f);
                path.lineTo(14, 3.5f);
                path.lineTo(16, 8);
                c.drawPath(path, paint);
                line(c, 7, 13, 17, 13);
                break;
            case ROAD:
                path.moveTo(8, 21);
                path.lineTo(10, 3);
                path.lineTo(14, 3);
                path.lineTo(16, 21);
                c.drawPath(path, paint);
                line(c, 12, 6, 12, 9);
                line(c, 12, 12, 12, 15);
                break;
            default:
                c.drawCircle(12, 12, 8, paint);
                break;
        }
    }

    private Paint fillPaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        return p;
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2) {
        Paint.Style old = paint.getStyle();
        paint.setStyle(Paint.Style.STROKE);
        c.drawLine(x1, y1, x2, y2, paint);
        paint.setStyle(old);
    }

    private void bar(Canvas c, float left, float top, float right, float bottom) {
        Paint.Style old = paint.getStyle();
        paint.setStyle(Paint.Style.FILL);
        rect.set(left, top, right, bottom);
        c.drawRoundRect(rect, 1, 1, paint);
        paint.setStyle(old);
    }
}
