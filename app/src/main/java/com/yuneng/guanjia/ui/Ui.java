package com.yuneng.guanjia.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yuneng.guanjia.ui.widgets.IconView;

public final class Ui {
    /** 主题主色，随「全局配色」切换，因此不是 final。 */
    public static int GREEN = 0xFF00C853;
    public static int GREEN_MID = 0xFF00B84A;
    public static int GREEN_DARK = 0xFF00A844;
    public static int GREEN_DEEP = 0xFF079B46;
    public static int GREEN_LIGHT = 0xFFE9F9F0;
    public static int GREEN_TINT = 0xFFF3FBF6;
    public static final int INK = 0xFF1B1D1F;
    public static final int INK_2 = 0xFF3D4247;
    public static final int SUB = 0xFF8C939B;
    public static final int MUTED = 0xFFB3B9C0;
    public static final int LINE = 0xFFE9ECEF;
    public static final int BG = 0xFFF5F6F8;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int RED = 0xFFFF3B30;
    public static final int RED_LIGHT = 0xFFFFECEB;
    public static final int BLUE = 0xFF2196F3;
    public static final int BLUE_LIGHT = 0xFFEAF4FE;
    public static final int ORANGE = 0xFFFF9500;
    public static final int YELLOW = 0xFFFFC107;
    public static final int PURPLE = 0xFF7C5CFC;

    public static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    public static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    public static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);
    public static final Typeface BLACK = Typeface.create("sans-serif-black", Typeface.NORMAL);

    private Ui() {
    }

    public static final String[] THEME_LABELS = {
            "翡翠绿", "天空蓝", "紫罗兰", "日落橙", "玫瑰红", "石墨灰", "青碧", "珊瑚"
    };

    /** 每套：主色、中间色、深色、更深色、浅底、更浅底。 */
    private static final int[][] THEME_PALETTES = {
            {0xFF00C853, 0xFF00B84A, 0xFF00A844, 0xFF079B46, 0xFFE9F9F0, 0xFFF3FBF6},
            {0xFF2F7BFF, 0xFF2868EB, 0xFF1E66E6, 0xFF1B5CD1, 0xFFEAF1FF, 0xFFF4F8FF},
            {0xFF7C4DFF, 0xFF7043E8, 0xFF6A3DE8, 0xFF5E33D6, 0xFFF1ECFF, 0xFFF8F5FF},
            {0xFFFF7A1A, 0xFFF06F10, 0xFFE96A0E, 0xFFD65E08, 0xFFFFF1E6, 0xFFFFF8F2},
            {0xFFF2456B, 0xFFE03A5E, 0xFFD93558, 0xFFC22C4C, 0xFFFFEBF0, 0xFFFFF5F7},
            {0xFF3D4753, 0xFF333C47, 0xFF2F3843, 0xFF252C35, 0xFFEDEFF2, 0xFFF5F6F8},
            {0xFF00BFA5, 0xFF00AE97, 0xFF00A08B, 0xFF00897B, 0xFFE0F7F4, 0xFFF0FBF9},
            {0xFFFF5C4D, 0xFFE85042, 0xFFD94A3B, 0xFFC2402F, 0xFFFFEDEA, 0xFFFFF6F4}
    };

    private static int themeIndex = 0;

    public static int themeIndex() {
        return themeIndex;
    }

    public static String themeLabel() {
        return THEME_LABELS[themeIndex];
    }

    public static int themeCount() {
        return THEME_LABELS.length;
    }

    /** 切换全局配色；调用后需要重建界面才能全部生效。 */
    public static void applyTheme(int index) {
        if (index < 0 || index >= THEME_PALETTES.length) index = 0;
        themeIndex = index;
        int[] p = THEME_PALETTES[index];
        GREEN = p[0];
        GREEN_MID = p[1];
        GREEN_DARK = p[2];
        GREEN_DEEP = p[3];
        GREEN_LIGHT = p[4];
        GREEN_TINT = p[5];
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int screenWidth(Context c) {
        return c.getResources().getDisplayMetrics().widthPixels;
    }

    public static int screenHeight(Context c) {
        return c.getResources().getDisplayMetrics().heightPixels;
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, Typeface tf) {
        TextView v = new TextView(c);
        v.setText(s);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(color);
        v.setTypeface(tf);
        v.setIncludeFontPadding(false);
        v.setLineSpacing(dp(c, 2), 1f);
        return v;
    }

    public static TextView regular(Context c, CharSequence s, float sp, int color) {
        return text(c, s, sp, color, REGULAR);
    }

    public static TextView medium(Context c, CharSequence s, float sp, int color) {
        return text(c, s, sp, color, MEDIUM);
    }

    public static TextView bold(Context c, CharSequence s, float sp, int color) {
        return text(c, s, sp, color, BOLD);
    }

    public static TextView black(Context c, CharSequence s, float sp, int color) {
        return text(c, s, sp, color, BLACK);
    }

    public static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout horizontal(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout card(Context c) {
        LinearLayout l = vertical(c);
        l.setBackground(round(WHITE, dp(c, 18)));
        l.setElevation(dp(c, 1.2f));
        l.setClipToPadding(false);
        int p = dp(c, 17);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static FrameLayout frame(Context c) {
        return new FrameLayout(c);
    }

    public static View space(Context c, float h) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, h)));
        return v;
    }

    public static View spaceW(Context c, float w) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, w), 1));
        return v;
    }

    public static void margins(View v, float l, float t, float r, float b) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            Context c = v.getContext();
            ((ViewGroup.MarginLayoutParams) lp).setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
            v.setLayoutParams(lp);
        }
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lp(Context c, float wDp, float hDp) {
        return new LinearLayout.LayoutParams(dp(c, wDp), dp(c, hDp));
    }

    public static LinearLayout.LayoutParams weight(Context c, float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.weight = weight;
        return lp;
    }

    public static FrameLayout.LayoutParams flp(int w, int h) {
        return new FrameLayout.LayoutParams(w, h);
    }

    public static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable roundStroke(int fill, float radiusPx, int stroke, float strokePx) {
        GradientDrawable d = round(fill, radiusPx);
        d.setStroke(Math.max(1, Math.round(strokePx)), stroke);
        return d;
    }

    public static GradientDrawable gradient(int[] colors, float radiusPx) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable circle(int color, int sizePx) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        d.setSize(sizePx, sizePx);
        return d;
    }

    public static Drawable inputBackground(Context c) {
        GradientDrawable normal = round(0xFFF3F5F7, dp(c, 10));
        GradientDrawable focused = roundStroke(WHITE, dp(c, 10), GREEN, dp(c, 1.4f));
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, focused);
        s.addState(new int[]{android.R.attr.state_pressed}, focused);
        s.addState(new int[]{}, normal);
        return s;
    }

    public static Drawable inputBackgroundReadOnly(Context c) {
        return round(0xFFF3F5F7, dp(c, 10));
    }

    public static EditText edit(Context c, String hint, float sp) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setTypeface(MEDIUM);
        e.setBackground(inputBackground(c));
        e.setPadding(dp(c, 10), 0, dp(c, 10), 0);
        e.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        e.setSingleLine(true);
        e.setIncludeFontPadding(false);
        e.setSelectAllOnFocus(true);
        return e;
    }

    public static void ripple(View v, int color) {
        if (v == null) return;
        Drawable content = v.getBackground();
        if (content == null) content = new ColorDrawable(Color.TRANSPARENT);
        RippleDrawable r = new RippleDrawable(ColorStateList.valueOf(color), content, content);
        v.setBackground(r);
    }

    public static void clickable(View v, View.OnClickListener l) {
        v.setClickable(true);
        v.setFocusable(true);
        v.setOnClickListener(l);
        ripple(v, 0x22000000);
    }

    public static IconView icon(Context c, int type, float sizeDp, int color) {
        IconView v = new IconView(c);
        v.icon(type).color(color);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return v;
    }

    public static IconView iconFilled(Context c, int type, float sizeDp, int color) {
        IconView v = icon(c, type, sizeDp, color);
        v.filled(true);
        return v;
    }

    public static TextView chip(Context c, String text, int bg, int fg, float sp) {
        TextView v = regular(c, text, sp, fg);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        v.setBackground(round(bg, dp(c, 20)));
        return v;
    }

    public static View dashedDivider(Context c) {
        return new DashDivider(c);
    }

    public static int statusBarHeight(Context c) {
        int id = c.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) return c.getResources().getDimensionPixelSize(id);
        return dp(c, 24);
    }

    public static void setStatusBar(Activity a, int color, boolean darkIcons) {
        Window w = a.getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(color);
        View decor = w.getDecorView();
        int flags = decor.getSystemUiVisibility();
        flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= 23) {
            if (darkIcons) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decor.setSystemUiVisibility(flags);
    }

    public static void setNavigationBar(Activity a, int color, boolean darkIcons) {
        Window w = a.getWindow();
        w.setNavigationBarColor(color);
        if (Build.VERSION.SDK_INT >= 26) {
            View decor = w.getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (darkIcons) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            decor.setSystemUiVisibility(flags);
        }
    }

    public static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    public static void hideKeyboard(View v) {
        if (v == null) return;
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static void showKeyboard(View v) {
        if (v == null) return;
        v.requestFocus();
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    public static String ellipsize(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    public static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams matchMatch() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public static void setBg(View v, Drawable d) {
        v.setBackground(d);
    }

    public static void addTopPadding(View v, int extraDp) {
        v.setPadding(v.getPaddingLeft(), v.getPaddingTop() + dp(v.getContext(), extraDp),
                v.getPaddingRight(), v.getPaddingBottom());
    }

    public static void setMargins(View v, int l, int t, int r, int b) {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).setMargins(l, t, r, b);
        }
    }

    public static void setLinearMargins(View v, float l, float t, float r, float b) {
        LinearLayout.LayoutParams lp;
        if (v.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        } else {
            lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        Context c = v.getContext();
        lp.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        v.setLayoutParams(lp);
    }

    public static TextView valueWithUnit(Context c, String value, String unit, float valueSp, int valueColor) {
        TextView v = bold(c, value, valueSp, valueColor);
        v.setGravity(Gravity.END);
        return v;
    }

    public static LinearLayout unitRow(Context c, TextView value, String unit, int unitColor) {
        LinearLayout row = horizontal(c);
        row.setGravity(Gravity.BOTTOM);
        row.addView(value, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView u = regular(c, unit, 11, unitColor);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ulp.leftMargin = dp(c, 3);
        ulp.bottomMargin = dp(c, 3);
        row.addView(u, ulp);
        return row;
    }

    /** 撑满剩余空间的透明 View。 */
    public static View flexible(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    public static void setSelectable(LinearLayout container) {
        container.setClickable(true);
        container.setFocusable(true);
        ripple(container, 0x16000000);
    }

    public static int blend(int a, int b, float f) {
        f = Math.max(0, Math.min(1, f));
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b);
        return Color.rgb((int) (ar + (br - ar) * f), (int) (ag + (bg - ag) * f), (int) (ab + (bb - ab) * f));
    }

    public static class DashDivider extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        public DashDivider(Context c) {
            super(c);
            paint.setColor(LINE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(c, 1));
            paint.setPathEffect(new DashPathEffect(new float[]{dp(c, 3), dp(c, 3)}, 0));
            setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1)));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            path.reset();
            path.moveTo(0, getHeight() / 2f);
            path.lineTo(getWidth(), getHeight() / 2f);
            canvas.drawPath(path, paint);
        }
    }
}
