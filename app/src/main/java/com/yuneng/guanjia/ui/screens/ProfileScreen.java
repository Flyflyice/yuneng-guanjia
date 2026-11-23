package com.yuneng.guanjia.ui.screens;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yuneng.guanjia.MainActivity;
import com.yuneng.guanjia.data.Repository;
import com.yuneng.guanjia.model.Stats;
import com.yuneng.guanjia.model.Vehicle;
import com.yuneng.guanjia.ui.Ui;
import com.yuneng.guanjia.ui.widgets.IconView;
import com.yuneng.guanjia.ui.widgets.PickerDialog;
import com.yuneng.guanjia.util.Formats;

import java.util.List;

/** 我的：车辆、数据备份/恢复与导出、外观、关于。 */
public class ProfileScreen extends BaseScreen {
    private Stats stats;

    public ProfileScreen(MainActivity activity) {
        super(activity);
    }

    @Override
    protected void build() {
        stats = repo.stats(vehicle);
        add(header());
        scrollBody(24);
        add(scroll, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.setLinearMargins(scroll, 0, -16, 0, 0);
        body.setPadding(Ui.dp(c, 12), Ui.dp(c, 0), Ui.dp(c, 12), 0);

        body.addView(vehicleCard());
        body.addView(Ui.space(c, 10));
        body.addView(recordCard());
        body.addView(Ui.space(c, 10));
        body.addView(appearanceCard());
        body.addView(Ui.space(c, 10));
        body.addView(dataCard());
        body.addView(Ui.space(c, 10));
        body.addView(aboutCard());
        body.addView(Ui.space(c, 14));
        TextView version = Ui.regular(c, "驭能管家 v1.2.0 · 数据仅保存在本机", 9.5f, Ui.MUTED);
        version.setGravity(Gravity.CENTER);
        body.addView(version);
        body.addView(Ui.space(c, 10));
    }

    private View header() {
        LinearLayout head = Ui.vertical(c);
        head.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Ui.GREEN, Ui.GREEN_DARK}));
        head.setPadding(Ui.dp(c, 18), Ui.statusBarHeight(c) + Ui.dp(c, 18), Ui.dp(c, 18), Ui.dp(c, 30));

        LinearLayout user = Ui.horizontal(c);
        View avatar = new View(c);
        avatar.setBackground(Ui.circle(0x33FFFFFF, Ui.dp(c, 54)));
        FrameLayout avatarBox = Ui.frame(c);
        avatarBox.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(c, 54), Ui.dp(c, 54)));
        avatarBox.addView(Ui.icon(c, IconView.CAR, 29, Color.WHITE),
                new android.widget.FrameLayout.LayoutParams(Ui.dp(c, 29), Ui.dp(c, 29), Gravity.CENTER));
        user.addView(avatarBox);
        LinearLayout info = Ui.vertical(c);
        info.setPadding(Ui.dp(c, 12), 0, 0, 0);
        info.addView(Ui.bold(c, vehicle.shortName(), 18, Color.WHITE));
        String typeText = vehicle.typeLabel();
        String energyText = vehicle.energyLabel();
        if (!energyText.isEmpty() && !energyText.equals(typeText)) {
            typeText = typeText + " · " + energyText;
        }
        if (!typeText.isEmpty()) {
            info.addView(Ui.regular(c, typeText, 11, 0xD9FFFFFF));
        }
        user.addView(info);
        user.addView(Ui.flexible(c));
        View edit = Ui.icon(c, IconView.EDIT, 20, Color.WHITE);
        user.addView(edit);
        Ui.clickable(edit, v -> activity.openVehicles());
        head.addView(user);

        LinearLayout nums = Ui.horizontal(c);
        nums.setPadding(0, Ui.dp(c, 18), 0, 0);
        nums.addView(headNum(Formats.num(stats.distance, 0), "累计公里"), weight());
        nums.addView(headNum(String.valueOf(stats.recordCount), "条记录"), weight());
        nums.addView(headNum(String.valueOf(stats.days), "天陪伴"), weight());
        head.addView(nums);
        return head;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    }

    private View headNum(String value, String label) {
        LinearLayout box = Ui.vertical(c);
        TextView v = Ui.bold(c, value, 19, Color.WHITE);
        v.setGravity(Gravity.CENTER);
        box.addView(v, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView l = Ui.regular(c, label, 9.5f, 0xCCFFFFFF);
        l.setGravity(Gravity.CENTER);
        box.addView(l, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private View vehicleCard() {
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "我的车辆", 15, Ui.INK));
        title.addView(Ui.flexible(c));
        TextView switchBtn = Ui.medium(c, "切换车辆", 10.5f, Ui.GREEN_DARK);
        title.addView(switchBtn);
        card.addView(title);

        LinearLayout row = Ui.horizontal(c);
        row.setPadding(0, Ui.dp(c, 12), 0, 0);
        View iconBg = new View(c);
        iconBg.setBackground(Ui.circle(Ui.GREEN_LIGHT, Ui.dp(c, 48)));
        FrameLayout iconBox = Ui.frame(c);
        iconBox.addView(iconBg, new LinearLayout.LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48)));
        iconBox.addView(Ui.icon(c, IconView.CAR, 26, Ui.GREEN_DARK),
                new android.widget.FrameLayout.LayoutParams(Ui.dp(c, 26), Ui.dp(c, 26), Gravity.CENTER));
        row.addView(iconBox);
        LinearLayout text = Ui.vertical(c);
        text.setPadding(Ui.dp(c, 10), 0, 0, 0);
        text.addView(Ui.medium(c, vehicle.shortName(), 14, Ui.INK));
        if (!vehicle.specLine().isEmpty()) {
            text.addView(Ui.regular(c, vehicle.specLine(), 10.5f, Ui.SUB));
        }
        if (!vehicle.isElectric() && vehicle.tankCapacity > 0) {
            text.addView(Ui.regular(c, "油箱 " + Formats.num(vehicle.tankCapacity, 0) + "L · "
                    + vehicle.fuelGrade, 9.5f, Ui.MUTED));
        }
        row.addView(text, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Ui.icon(c, IconView.CHEVRON_RIGHT, 18, Ui.MUTED));
        card.addView(row);
        Ui.clickable(row, v -> activity.openVehicles());
        Ui.clickable(switchBtn, v -> switchVehicle());
        return card;
    }

    private void switchVehicle() {
        List<Vehicle> vehicles = repo.getVehicles();
        String[] names = new String[vehicles.size()];
        int selected = 0;
        for (int i = 0; i < vehicles.size(); i++) {
            names[i] = vehicles.get(i).shortName();
            if (vehicles.get(i).id == vehicle.id) selected = i;
        }
        PickerDialog.choose(c, "切换车辆", names, selected, (index, value) -> {
            repo.selectVehicle(vehicles.get(index).id);
            activity.onVehicleChanged();
            switchScreenRefresh();
        });
    }

    private void switchScreenRefresh() {
        // 切换后由 MainActivity 重建所有缓存页面
    }

    private View recordCard() {
        LinearLayout card = whiteCard();
        LinearLayout title = Ui.horizontal(c);
        title.addView(Ui.bold(c, "记录概览", 15, Ui.INK));
        title.addView(Ui.flexible(c));
        TextView list = Ui.regular(c, "查看全部  >", 10.5f, Ui.SUB);
        title.addView(list);
        card.addView(title);

        LinearLayout row = Ui.horizontal(c);
        row.setPadding(0, Ui.dp(c, 12), 0, 0);
        row.addView(overview("充电", String.valueOf(stats.chargeCount), "次", Ui.GREEN_DARK), weight());
        row.addView(overview("加油", String.valueOf(stats.fuelCount), "次", 0xFFE06A10), weight());
        row.addView(overview("总花费", Formats.num(stats.totalCost, 0), "元", Ui.INK), weight());
        card.addView(row);
        Ui.clickable(list, v -> activity.openRecordList());
        return card;
    }

    private View overview(String label, String value, String unit, int color) {
        LinearLayout box = Ui.vertical(c);
        TextView l = Ui.regular(c, label, 10, Ui.SUB);
        l.setGravity(Gravity.CENTER);
        box.addView(l, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout row = Ui.horizontal(c);
        row.setGravity(Gravity.CENTER);
        row.addView(Ui.bold(c, value, 17, color));
        TextView u = Ui.regular(c, unit, 9.5f, Ui.SUB);
        u.setPadding(Ui.dp(c, 2), 0, 0, Ui.dp(c, 2));
        row.addView(u);
        box.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private View dataCard() {
        LinearLayout card = whiteCard();
        card.addView(Ui.bold(c, "数据管理", 15, Ui.INK));
        card.addView(Ui.space(c, 6));
        card.addView(item(IconView.BACKUP, "备份与恢复", "备份到手机任意位置，或从备份文件还原", v -> showBackupDialog()));
        card.addView(Ui.dashedDivider(c));
        card.addView(item(IconView.DOWNLOAD, "导出 CSV", "导出全部记录，可用 Excel 打开", v -> confirmExport()));
        card.addView(Ui.dashedDivider(c));
        card.addView(item(IconView.DOCUMENT, "导入 CSV", "从文件追加记录，不会覆盖已有数据", v -> confirmImport()));
        card.addView(Ui.dashedDivider(c));
        card.addView(item(IconView.TRASH, "清空全部数据", "删除所有车辆与记录，操作不可恢复", v -> confirmClear()));
        return card;
    }

    private View item(int icon, String title, String desc, View.OnClickListener l) {
        LinearLayout row = Ui.horizontal(c);
        row.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 10));
        View bg = new View(c);
        bg.setBackground(Ui.circle(0xFFF2F6F3, Ui.dp(c, 32)));
        FrameLayout box = Ui.frame(c);
        box.addView(bg, new LinearLayout.LayoutParams(Ui.dp(c, 32), Ui.dp(c, 32)));
        box.addView(Ui.icon(c, icon, 17, Ui.GREEN_DARK),
                new android.widget.FrameLayout.LayoutParams(Ui.dp(c, 17), Ui.dp(c, 17), Gravity.CENTER));
        row.addView(box);
        LinearLayout text = Ui.vertical(c);
        text.setPadding(Ui.dp(c, 9), 0, 0, 0);
        text.addView(Ui.medium(c, title, 12.5f, Ui.INK));
        text.addView(Ui.regular(c, desc, 9.5f, Ui.SUB));
        row.addView(text, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        row.addView(Ui.icon(c, IconView.CHEVRON_RIGHT, 15, Ui.MUTED));
        Ui.clickable(row, l);
        return row;
    }

    private View appearanceCard() {
        LinearLayout card = whiteCard();
        card.addView(Ui.bold(c, "外观", 15, Ui.INK));
        card.addView(Ui.space(c, 6));
        card.addView(item(IconView.STAR, "全局配色", "当前：" + Ui.themeLabel(), v -> pickTheme()));
        return card;
    }

    private void pickTheme() {
        PickerDialog.choose(c, "全局配色", Ui.THEME_LABELS, Ui.themeIndex(), (index, value) -> {
            Ui.applyTheme(index);
            activity.getSharedPreferences("yuneng_config", android.content.Context.MODE_PRIVATE)
                    .edit().putInt("theme_index", index).apply();
            activity.onThemeChanged();
        });
    }

    private void showBackupDialog() {
        new AlertDialog.Builder(c)
                .setTitle("备份与恢复")
                .setMessage("备份：把数据库保存到手机任意位置（下载、文档、网盘），文件名带时间戳。\n\n"
                        + "恢复：从之前备份的 .db 文件还原全部车辆与记录，会覆盖当前数据。")
                .setPositiveButton("备份", (d, w) -> confirmBackup())
                .setNeutralButton("恢复", (d, w) -> confirmRestoreDb())
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmRestoreDb() {
        new AlertDialog.Builder(c)
                .setTitle("恢复数据")
                .setMessage("将用所选备份文件覆盖当前全部车辆与记录，当前数据无法找回。\n\n继续选择备份文件？")
                .setPositiveButton("选择文件", (d, w) -> activity.pickDb(this::doRestoreDb))
                .setNegativeButton("取消", null)
                .show();
    }

    private void doRestoreDb(android.net.Uri uri) {
        boolean ok = repo.restoreFrom(uri);
        if (ok) {
            activity.onDataChanged();
            refresh();
            PickerDialog.info(c, "恢复完成", "备份已还原，车辆与记录都已替换为备份文件中的内容。");
        } else {
            PickerDialog.info(c, "恢复失败",
                    "该文件不是有效的备份数据库，或无法读取。\n\n请确认选择的是备份时保存的「驭能管家_时间戳.db」文件。");
        }
    }

    private void confirmBackup() {
        new AlertDialog.Builder(c)
                .setTitle("备份数据")
                .setMessage("下一步会让你选择保存位置（下载、文档、网盘等），文件名带时间戳。\n\n继续备份？")
                .setPositiveButton("选择位置", (d, w) -> {
                    String stamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",
                            java.util.Locale.CHINA).format(new java.util.Date());
                    activity.pickSaveFile("驭能管家_" + stamp + ".db", this::doBackup);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void doBackup(android.net.Uri uri) {
        boolean ok = repo.backupTo(uri);
        PickerDialog.info(c, ok ? "备份完成" : "备份失败",
                ok ? "数据库已保存到你选择的位置。\n\n把该文件复制到电脑或其他设备即可留档。"
                        : "无法写入所选位置，请换一个目录重试。");
    }

    private void confirmExport() {
        new AlertDialog.Builder(c)
                .setTitle("导出 CSV")
                .setMessage("将把全部记录导出为 CSV 文件，可用 Excel 打开，也可以直接分享给微信等应用。\n\n继续导出？")
                .setPositiveButton("导出", (d, w) -> {
                    String path = repo.exportCsv();
                    if (path == null) {
                        PickerDialog.info(c, "导出失败", "请检查存储权限。");
                        return;
                    }
                    new AlertDialog.Builder(c)
                            .setTitle("导出完成")
                            .setMessage("CSV 已生成，可以直接分享到微信、QQ、邮件等应用。")
                            .setPositiveButton("分享", (d2, w2) -> shareCsv(path))
                            .setNegativeButton("完成", null)
                            .show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void shareCsv(String path) {
        final android.net.Uri uri = repo.shareUri(path);
        if (uri == null) {
            Ui.toast(c, "文件不存在");
            return;
        }
        android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND);
        send.setType("text/csv");
        send.putExtra(android.content.Intent.EXTRA_STREAM, uri);
        send.putExtra(android.content.Intent.EXTRA_SUBJECT, "驭能管家 CSV 记录");
        send.putExtra(android.content.Intent.EXTRA_TEXT, "驭能管家导出的能耗与费用记录（CSV），可用 Excel 或 WPS 打开。");
        send.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            c.startActivity(android.content.Intent.createChooser(send, "分享 CSV"));
        } catch (Exception e) {
            Ui.toast(c, "没有可用的分享应用");
        }
    }

    private void confirmImport() {
        new AlertDialog.Builder(c)
                .setTitle("导入 CSV")
                .setMessage("将把所选 CSV 中的记录追加到对应车辆，不会删除已有数据。\n\n"
                        + "类型列填 充电 / 加油 / 收入 / 支出，时间列形如 2026-01-05 18:30。\n\n"
                        + "不确定格式？可先点「导出模板」获取一份带 3 行示例的样板。")
                .setPositiveButton("选择文件", (d, w) -> activity.pickCsv(this::doImport))
                .setNeutralButton("导出模板", (d, w) -> shareTemplate())
                .setNegativeButton("取消", null)
                .show();
    }

    private void doImport(android.net.Uri uri) {
        Repository.ImportResult result = repo.importCsv(uri);
        if (result.failed) {
            PickerDialog.info(c, "导入失败", result.message);
            return;
        }
        if (result.imported > 0) activity.onDataChanged();
        String msg = "成功导入 " + result.imported + " 条记录。";
        if (result.skipped > 0) msg += "\n跳过 " + result.skipped + " 行（类型或时间无法识别）。";
        PickerDialog.info(c, "导入完成", msg);
    }

    private void shareTemplate() {
        final String path = repo.exportTemplate();
        if (path == null) {
            Ui.toast(c, "模板生成失败");
            return;
        }
        final android.net.Uri uri = repo.shareUri(path);
        android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND);
        send.setType("text/csv");
        send.putExtra(android.content.Intent.EXTRA_STREAM, uri);
        send.putExtra(android.content.Intent.EXTRA_SUBJECT, "驭能管家 CSV 导入模板");
        send.putExtra(android.content.Intent.EXTRA_TEXT,
                "驭能管家 CSV 导入模板，用 Excel 或 WPS 填写后可在「我的 → 导入 CSV」中导入。");
        send.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            c.startActivity(android.content.Intent.createChooser(send, "分享 CSV 模板"));
        } catch (Exception e) {
            Ui.toast(c, "没有可用的分享应用");
        }
    }

    private void confirmClear() {
        new AlertDialog.Builder(c)
                .setTitle("清空全部数据")
                .setMessage("将删除所有车辆信息及其全部充电、加油、收入、支出记录，且无法恢复。\n\n是否继续？")
                .setPositiveButton("继续", (d, w) -> confirmClearFinal())
                .setNegativeButton("取消", null)
                .show();
    }

    /** 二次确认，避免误触造成不可恢复的数据丢失。 */
    private void confirmClearFinal() {
        new AlertDialog.Builder(c)
                .setTitle("最后确认")
                .setMessage("清空后全部数据将被永久删除，无法撤销，也没有自动备份。\n\n确定要清空吗？")
                .setPositiveButton("确认清空", (d, w) -> {
                    repo.clearAll();
                    activity.onDataChanged();
                    Ui.toast(c, "全部数据已清空");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private View aboutCard() {
        LinearLayout card = whiteCard();
        card.addView(Ui.bold(c, "关于", 15, Ui.INK));
        card.addView(Ui.space(c, 6));
        card.addView(item(IconView.INFO, "第五代 DM-i 技术说明", "46.06% 热效率与能耗折算口径", v -> PickerDialog.info(c,
                "第五代 DM-i",
                "骁云-插混专用 1.5L 发动机最高热效率 46.06%，配合 EHS 电混系统与 92% 电驱效率。\n\n"
                        + "本应用折算口径：\n"
                        + "1 kWh ≈ 0.31 L 汽油；\n"
                        + "1 L 汽油 ≈ 8.9 kWh 能量；\n"
                        + "每升油可发电 = 8.9 × 46.06% × 92% ≈ 3.77 kWh；\n"
                        + "等效油耗 = 平均油耗 + 平均电耗 ÷ 3.77。")));
        card.addView(Ui.dashedDivider(c));
        card.addView(item(IconView.LEAF, "驭能管家", "纯本地 · 无广告 · 无网络权限", v ->
                PickerDialog.info(c, "驭能管家", "一款为电车、油车、插混车设计的能耗与费用记录工具。\n\n"
                        + "所有数据保存在手机本地 SQLite 数据库中，不上传服务器。\n"
                        + "支持充电、加油、收入、支出四类记录，以及油耗、电耗、综合能耗、费用构成和电池健康统计。")));
        return card;
    }
}
