package com.yuneng.guanjia;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.data.Repository;
import com.yuneng.guanjia.model.EnergyRecord;
import com.yuneng.guanjia.model.Vehicle;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.screens.BaseScreen;
import com.yuneng.guanjia.ui.screens.CostScreen;
import com.yuneng.guanjia.ui.screens.EnergyScreen;
import com.yuneng.guanjia.ui.screens.FormScreen;
import com.yuneng.guanjia.ui.screens.ProfileScreen;
import com.yuneng.guanjia.ui.screens.RecordListScreen;
import com.yuneng.guanjia.ui.screens.StatsScreen;
import com.yuneng.guanjia.ui.screens.TripScreen;
import com.yuneng.guanjia.ui.screens.VehicleScreen;
import com.yuneng.guanjia.ui.widgets.BottomBar;
import com.yuneng.guanjia.ui.widgets.IconView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements BottomBar.Listener {
    private Repository repository;
    private FrameLayout root;
    private FrameLayout content;
    private BottomBar bottomBar;
    private final BaseScreen[] tabs = new BaseScreen[4];
    private final List<BaseScreen> stack = new ArrayList<>();
    private int currentTab = 0;
    private View quickAddOverlay;

    private static final int REQ_LOCATION = 903;
    private static final int REQ_CSV = 904;
    private static final int REQ_SAVE = 905;
    private static final int REQ_DB = 906;
    private java.util.function.Consumer<Uri> csvCallback;
    private java.util.function.Consumer<Uri> saveCallback;
    private java.util.function.Consumer<Uri> dbCallback;
    private static final long LOCATION_TIMEOUT_MS = 8000;
    private String locationName = "定位中…";
    private LocationManager locationManager;
    private LocationListener locationListener;
    private final Handler locationHandler = new Handler(Looper.getMainLooper());
    private Runnable locationTimeout;
    private boolean locationResolved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyTheme(getSharedPreferences("yuneng_config", MODE_PRIVATE).getInt("theme_index", 0));
        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt("current_tab", 0);
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        Ui.setStatusBar(this, Color.TRANSPARENT, false);
        Ui.setNavigationBar(this, Color.WHITE, true);

        repository = new Repository(this);

        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        root.setClipChildren(false);
        root.setClipToPadding(false);

        content = new FrameLayout(this);
        content.setClipChildren(false);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        clp.bottomMargin = Ui.dp(this, 58);
        root.addView(content, clp);

        bottomBar = new BottomBar(this, this);
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 58));
        blp.gravity = Gravity.BOTTOM;
        root.addView(bottomBar, blp);
        bottomBar.setClipChildren(false);

        setContentView(root);
        switchTab(currentTab);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        root.post(this::requestLocation);
    }

    public Repository repo() {
        return repository;
    }

    public Vehicle vehicle() {
        return repository.getCurrentVehicle();
    }

    public String locationName() {
        return locationName;
    }

    /** 首页定位文字可点击刷新；未授权时发起系统权限请求。 */
    public void requestLocation() {
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQ_LOCATION);
            return;
        }
        locate();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void locate() {
        if (locationManager == null) return;
        stopLocationUpdates();
        locationResolved = false;
        if (locationName == null || locationName.isEmpty()) {
            locationName = "定位中…";
        } else {
            setLocation(locationName);
        }

        Location best = null;
        try {
            for (String provider : locationManager.getProviders(true)) {
                Location l = locationManager.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
        } catch (SecurityException ignored) {
        }
        if (best != null) resolveCity(best);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (locationResolved) return;
                resolveCity(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        0, 0, locationListener, Looper.getMainLooper());
            }
        } catch (SecurityException ignored) {
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        0, 0, locationListener, Looper.getMainLooper());
            }
        } catch (SecurityException ignored) {
        }

        locationHandler.removeCallbacks(locationTimeout);
        locationTimeout = () -> {
            if (!locationResolved && "定位中…".equals(locationName)) {
                setLocation("未知位置");
                stopLocationUpdates();
            }
        };
        locationHandler.postDelayed(locationTimeout, LOCATION_TIMEOUT_MS);
    }

    private void resolveCity(Location location) {
        if (location == null) return;
        locationResolved = true;
        final double lat = location.getLatitude();
        final double lng = location.getLongitude();
        new Thread(() -> {
            String city = null;
            try {
                if (Geocoder.isPresent()) {
                    List<Address> addresses = new Geocoder(this, Locale.CHINA)
                            .getFromLocation(lat, lng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address a = addresses.get(0);
                        city = firstNonEmpty(a.getLocality(), a.getSubAdminArea(),
                                a.getAdminArea(), a.getFeatureName());
                    }
                }
            } catch (Exception ignored) {
            }
            final String result = city == null || city.trim().isEmpty() ? "当前位置" : city;
            runOnUiThread(() -> {
                setLocation(result);
                stopLocationUpdates();
            });
        }, "reverse-geocode").start();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }

    private void setLocation(String name) {
        if (name == null || name.trim().isEmpty()) return;
        locationName = name.trim();
        if (tabs[0] instanceof EnergyScreen) {
            ((EnergyScreen) tabs[0]).setLocation(locationName);
        }
    }

    private void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception ignored) {
            }
            locationListener = null;
        }
        if (locationTimeout != null) {
            locationHandler.removeCallbacks(locationTimeout);
            locationTimeout = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (hasLocationPermission()) {
                locate();
            } else {
                setLocation("未开启定位");
                Ui.toast(this, "未获得定位权限，可点击左上角位置重试");
            }
        }
    }

    @Override
    public void onTab(int index) {
        if (stack.isEmpty() && currentTab == index) return;
        clearStack();
        switchTab(index);
    }

    private void switchTab(int index) {
        currentTab = index;
        content.removeAllViews();
        BaseScreen screen = tabs[index];
        if (screen == null) {
            switch (index) {
                case 0:
                    screen = new EnergyScreen(this);
                    break;
                case 1:
                    screen = new CostScreen(this);
                    break;
                case 2:
                    screen = new StatsScreen(this);
                    break;
                default:
                    screen = new ProfileScreen(this);
                    break;
            }
            tabs[index] = screen;
        }
        content.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentBottomInset(58);
        bottomBar.setVisibility(View.VISIBLE);
        bottomBar.setSelected(index);
    }

    @Override
    public void onAdd() {
        if (quickAddOverlay != null) {
            hideQuickAdd();
            return;
        }
        showQuickAdd();
    }

    /** 参考“记一笔”悬浮菜单：收入 / 记能耗 / 支出，点击遮罩或返回键关闭。 */
    private void showQuickAdd() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x99000000);
        overlay.setOnClickListener(v -> hideQuickAdd());

        LinearLayout panel = Ui.vertical(this);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setClickable(true);
        panel.setOnClickListener(v -> {
            // 消费面板内空白区域的点击，避免误关菜单
        });

        LinearLayout bubbles = Ui.horizontal(this);
        bubbles.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        bubbles.addView(quickItem("收入", IconView.INCOME, 0xFFFF3B30, EnergyRecord.TYPE_INCOME),
                new LinearLayout.LayoutParams(Ui.dp(this, 72), ViewGroup.LayoutParams.WRAP_CONTENT));
        bubbles.addView(Ui.spaceW(this, 26));
        bubbles.addView(quickItem("记能耗", IconView.BOLT, Ui.GREEN, EnergyRecord.TYPE_CHARGE),
                new LinearLayout.LayoutParams(Ui.dp(this, 72), ViewGroup.LayoutParams.WRAP_CONTENT));
        bubbles.addView(Ui.spaceW(this, 26));
        bubbles.addView(quickItem("支出", IconView.EXPENSE, 0xFF2196F3, EnergyRecord.TYPE_EXPENSE),
                new LinearLayout.LayoutParams(Ui.dp(this, 72), ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(bubbles, new LinearLayout.LayoutParams(Ui.dp(this, 280),
                ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout close = Ui.frame(this);
        close.setBackground(Ui.circle(0xCC1B1D1F, Ui.dp(this, 22)));
        close.addView(Ui.icon(this, IconView.CLOSE, 18, Color.WHITE),
                new FrameLayout.LayoutParams(Ui.dp(this, 18), Ui.dp(this, 18), Gravity.CENTER));
        close.setElevation(Ui.dp(this, 4));
        Ui.clickable(close, v -> hideQuickAdd());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(
                Ui.dp(this, 44), Ui.dp(this, 44));
        closeLp.gravity = Gravity.CENTER_HORIZONTAL;
        closeLp.topMargin = Ui.dp(this, 26);
        panel.addView(close, closeLp);

        FrameLayout.LayoutParams panelLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        panelLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        panelLp.bottomMargin = Ui.dp(this, 94);
        overlay.addView(panel, panelLp);
        root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(160).start();
        panel.setAlpha(0f);
        panel.setScaleX(0.82f);
        panel.setScaleY(0.82f);
        panel.setTranslationY(Ui.dp(this, 36));
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(230).setInterpolator(new OvershootInterpolator(0.9f)).start();
        quickAddOverlay = overlay;
    }

    private View quickItem(String label, int icon, int color, String type) {
        LinearLayout box = Ui.vertical(this);
        box.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout circle = Ui.frame(this);
        circle.setBackground(Ui.circle(color, Ui.dp(this, 34)));
        circle.setElevation(Ui.dp(this, 4));
        circle.addView(Ui.icon(this, icon, 30, Color.WHITE),
                new FrameLayout.LayoutParams(Ui.dp(this, 30), Ui.dp(this, 30), Gravity.CENTER));
        box.addView(circle, new LinearLayout.LayoutParams(Ui.dp(this, 68), Ui.dp(this, 68)));

        TextView text = Ui.medium(this, label, 12, Color.WHITE);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textLp.topMargin = Ui.dp(this, 9);
        box.addView(text, textLp);

        Ui.clickable(box, v -> {
            hideQuickAdd();
            openForm(type, null);
        });
        return box;
    }

    private void hideQuickAdd() {
        if (quickAddOverlay == null) return;
        View view = quickAddOverlay;
        quickAddOverlay = null;
        root.removeView(view);
    }

    public void openForm(String type, EnergyRecord record) {
        FormScreen form = new FormScreen(this, type, record);
        push(form);
    }

    public void openRecordList() {
        push(new RecordListScreen(this));
    }

    public void openTrip() {
        push(new TripScreen(this));
    }

    public void openVehicles() {
        push(new VehicleScreen(this));
    }

    public void push(BaseScreen screen) {
        hideQuickAdd();
        stack.add(screen);
        setContentBottomInset(0);
        content.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bottomBar.setVisibility(View.GONE);
        screen.setTranslationY(Ui.screenHeight(this));
        screen.animate().translationY(0).setDuration(230).start();
    }

    public void pop() {
        if (stack.isEmpty()) return;
        BaseScreen top = stack.remove(stack.size() - 1);
        content.removeView(top);
        if (stack.isEmpty()) {
            bottomBar.setVisibility(View.VISIBLE);
            switchTab(currentTab);
        } else {
            setContentBottomInset(0);
            stack.get(stack.size() - 1).refresh();
        }
    }

    public void clearStack() {
        while (!stack.isEmpty()) {
            BaseScreen top = stack.remove(stack.size() - 1);
            content.removeView(top);
        }
        setContentBottomInset(58);
        bottomBar.setVisibility(View.VISIBLE);
    }

    /** 有底部导航时给内容区留出 58dp；进入二级页面后铺满到底。 */
    private void setContentBottomInset(int dp) {
        ViewGroup.LayoutParams params = content.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) params;
            int target = Ui.dp(this, dp);
            if (lp.bottomMargin != target) {
                lp.bottomMargin = target;
                content.setLayoutParams(lp);
            }
        }
    }

    /** 数据变更后刷新所有缓存页面。 */
    public void onDataChanged() {
        for (int i = 0; i < tabs.length; i++) tabs[i] = null;
        if (stack.isEmpty()) {
            switchTab(currentTab);
        }
    }

    public void onVehicleChanged() {
        onDataChanged();
    }

    /** 切换全局配色后重建整个界面，让所有页面读到新颜色。 */
    public void onThemeChanged() {
        recreate();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_tab", currentTab);
    }

    /** 打开系统文件选择器读取 CSV，选中后回调结果。 */
    public void pickCsv(java.util.function.Consumer<Uri> callback) {
        csvCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/comma-separated-values",
                "text/plain", "application/csv", "application/vnd.ms-excel"});
        try {
            startActivityForResult(intent, REQ_CSV);
        } catch (Exception e) {
            csvCallback = null;
            Ui.toast(this, "没有可用的文件选择器");
        }
    }

    /** 打开系统保存对话框，让用户把备份文件存到手机任意位置。 */
    public void pickSaveFile(String defaultName, java.util.function.Consumer<Uri> callback) {
        saveCallback = callback;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, defaultName);
        try {
            startActivityForResult(intent, REQ_SAVE);
        } catch (Exception e) {
            saveCallback = null;
            Ui.toast(this, "没有可用的文件选择器");
        }
    }

    /** 打开系统文件选择器读取备份数据库（.db），选中后回调结果。 */
    public void pickDb(java.util.function.Consumer<Uri> callback) {
        dbCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/octet-stream", "application/x-sqlite3", "application/vnd.sqlite3",
                "application/x-database", "text/plain"});
        try {
            startActivityForResult(intent, REQ_DB);
        } catch (Exception e) {
            dbCallback = null;
            Ui.toast(this, "没有可用的文件选择器");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_DB) {
            java.util.function.Consumer<Uri> cb = dbCallback;
            dbCallback = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && cb != null) {
                cb.accept(data.getData());
            }
            return;
        }
        if (requestCode == REQ_CSV) {
            java.util.function.Consumer<Uri> cb = csvCallback;
            csvCallback = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && cb != null) {
                cb.accept(data.getData());
            }
            return;
        }
        if (requestCode == REQ_SAVE) {
            java.util.function.Consumer<Uri> cb = saveCallback;
            saveCallback = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && cb != null) {
                cb.accept(data.getData());
            }
            return;
        }
        if (requestCode == FormScreen.REQ_PHOTO && resultCode == RESULT_OK && data != null
                && !stack.isEmpty()) {
            BaseScreen top = stack.get(stack.size() - 1);
            if (top instanceof FormScreen) {
                ((FormScreen) top).onPhotoPicked(data.getData());
            }
        }
    }

    @Override
    protected void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (quickAddOverlay != null) {
            hideQuickAdd();
            return;
        }
        if (!stack.isEmpty()) {
            pop();
            return;
        }
        if (currentTab != 0) {
            switchTab(0);
            return;
        }
        super.onBackPressed();
    }
}
