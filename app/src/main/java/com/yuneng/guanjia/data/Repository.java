package com.yuneng.guanjia.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.yuneng.guanjia.model.EnergyRecord;
import com.yuneng.guanjia.model.FuelTypes;
import com.yuneng.guanjia.model.Stats;
import com.yuneng.guanjia.model.StatsCalculator;
import com.yuneng.guanjia.model.TimeRange;
import com.yuneng.guanjia.model.TripSlice;
import com.yuneng.guanjia.model.Vehicle;
import com.yuneng.guanjia.util.Formats;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Repository {
    private static final String PREFS = "yunengguanjia_prefs";
    private static final String KEY_VEHICLE = "current_vehicle";
    private static final String KEY_SEEDED = "seeded_v1";
    private static final String KEY_CHANGE_VERSION = "change_version";
    private static final String KEY_BACKUP_VERSION = "backup_version";

    public static final String CSV_HEADER = "车辆,类型,油品,时间,总里程(km),金额(元),数量,单价,"
            + "开始电量%,结束电量%,加油前油量%,加油后油量%,充电站/加油站,备注,分类,"
            + "充电时剩余油量%,加油时剩余电量%,实付金额(元),优惠金额(元),实付单价(元/升)\n";

    /** 表头关键字段，前缀匹配，允许用户保留括号说明。 */
    private static final String[] CSV_COLUMNS = {
            "车辆", "类型", "油品", "时间", "总里程", "金额", "数量", "单价",
            "开始电量", "结束电量", "加油前油量", "加油后油量", "充电站", "备注", "分类",
            "充电时剩余油量", "加油时剩余电量", "实付金额", "优惠金额", "实付单价"
    };

    private static final String[] TIME_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm", "yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd", "yyyy/MM/dd",
            "yyyy.MM.dd HH:mm"
    };

    private final Context context;
    private final DbHelper helper;
    private final SharedPreferences prefs;

    public Repository(Context context) {
        this.context = context.getApplicationContext();
        this.helper = new DbHelper(this.context);
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** 每次数据写入递增，用于判断备份后是否又有新记录。 */
    private void markChanged() {
        prefs.edit().putInt(KEY_CHANGE_VERSION,
                prefs.getInt(KEY_CHANGE_VERSION, 0) + 1).apply();
    }

    /** 当前数据版本是否还没有备份过。 */
    public boolean hasUnbackedChanges() {
        return prefs.getInt(KEY_CHANGE_VERSION, 0)
                != prefs.getInt(KEY_BACKUP_VERSION, -1);
    }

    // ------------------------------------------------------------------ 车辆

    public List<Vehicle> getVehicles() {
        List<Vehicle> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM vehicles ORDER BY is_default DESC, id ASC", null);
        try {
            while (c.moveToNext()) list.add(readVehicle(c));
        } finally {
            c.close();
        }
        return list;
    }

    public Vehicle getVehicle(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM vehicles WHERE id=?", new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) return readVehicle(c);
        } finally {
            c.close();
        }
        return null;
    }

    public Vehicle getCurrentVehicle() {
        long id = prefs.getLong(KEY_VEHICLE, -1);
        Vehicle v = id > 0 ? getVehicle(id) : null;
        if (v == null) {
            List<Vehicle> vehicles = getVehicles();
            if (vehicles.isEmpty()) {
                v = defaultVehicle();
                long newId = addVehicle(v);
                v.id = newId;
                // 空库初始化只创建一辆默认车辆，不提示“有未备份数据”
                prefs.edit().putInt(KEY_BACKUP_VERSION, prefs.getInt(KEY_CHANGE_VERSION, 0)).apply();
            } else {
                v = vehicles.get(0);
            }
            prefs.edit().putLong(KEY_VEHICLE, v.id).apply();
        }
        return v;
    }

    public void selectVehicle(long id) {
        prefs.edit().putLong(KEY_VEHICLE, id).apply();
    }

    public long addVehicle(Vehicle v) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", v.name == null || v.name.trim().isEmpty() ? "我的爱车" : v.name.trim());
        cv.put("type", v.type);
        cv.put("battery", v.batteryCapacity);
        cv.put("tank", v.tankCapacity);
        cv.put("thermal", v.thermalEfficiency);
        cv.put("fuel_grade", v.fuelGrade);
        cv.put("is_default", v.isDefault ? 1 : 0);
        cv.put("created_at", System.currentTimeMillis());
        long id = db.insert("vehicles", null, cv);
        v.id = id;
        if (prefs.getLong(KEY_VEHICLE, -1) <= 0) prefs.edit().putLong(KEY_VEHICLE, id).apply();
        markChanged();
        return id;
    }

    public void updateVehicle(Vehicle v) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", v.name);
        cv.put("type", v.type);
        cv.put("battery", v.batteryCapacity);
        cv.put("tank", v.tankCapacity);
        cv.put("thermal", v.thermalEfficiency);
        cv.put("fuel_grade", v.fuelGrade);
        cv.put("is_default", v.isDefault ? 1 : 0);
        db.update("vehicles", cv, "id=?", new String[]{String.valueOf(v.id)});
        markChanged();
    }

    /**
     * 删除车辆，并级联删除该车全部记录。
     * 至少保留一辆车；删除当前车辆后自动切到默认车（没有默认车则取第一辆）。
     */
    public boolean deleteVehicle(long id) {
        List<Vehicle> all = getVehicles();
        if (all.size() <= 1) return false;
        Vehicle target = getVehicle(id);
        if (target == null) return false;

        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 外键 ON DELETE CASCADE 会同步删除该车的全部记录
            db.delete("vehicles", "id=?", new String[]{String.valueOf(id)});
            if (target.isDefault) {
                List<Vehicle> remain = getVehicles();
                if (!remain.isEmpty()) {
                    ContentValues cv = new ContentValues();
                    cv.put("is_default", 1);
                    db.update("vehicles", cv, "id=?", new String[]{String.valueOf(remain.get(0).id)});
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        if (prefs.getLong(KEY_VEHICLE, -1) == id) {
            List<Vehicle> remain = getVehicles();
            if (!remain.isEmpty()) {
                Vehicle next = remain.get(0);
                for (Vehicle v : remain) {
                    if (v.isDefault) {
                        next = v;
                        break;
                    }
                }
                prefs.edit().putLong(KEY_VEHICLE, next.id).apply();
            }
        }
        markChanged();
        return true;
    }

    private Vehicle defaultVehicle() {
        Vehicle v = new Vehicle();
        v.name = "我的爱车";
        v.type = Vehicle.TYPE_UNSET;
        v.batteryCapacity = 0;
        v.tankCapacity = 0;
        v.thermalEfficiency = 0.4606;
        v.fuelGrade = "";
        v.isDefault = true;
        v.createdAt = System.currentTimeMillis();
        return v;
    }

    private Vehicle readVehicle(Cursor c) {
        Vehicle v = new Vehicle();
        v.id = c.getLong(c.getColumnIndexOrThrow("id"));
        v.name = c.getString(c.getColumnIndexOrThrow("name"));
        v.type = c.getString(c.getColumnIndexOrThrow("type"));
        v.batteryCapacity = c.getDouble(c.getColumnIndexOrThrow("battery"));
        v.tankCapacity = c.getDouble(c.getColumnIndexOrThrow("tank"));
        v.thermalEfficiency = c.getDouble(c.getColumnIndexOrThrow("thermal"));
        v.fuelGrade = c.getString(c.getColumnIndexOrThrow("fuel_grade"));
        v.isDefault = c.getInt(c.getColumnIndexOrThrow("is_default")) == 1;
        v.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        return v;
    }

    // ------------------------------------------------------------------ 记录

    public List<EnergyRecord> getRecords(long vehicleId) {
        List<EnergyRecord> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM records WHERE vehicle_id=? ORDER BY ts DESC, id DESC",
                new String[]{String.valueOf(vehicleId)});
        try {
            while (c.moveToNext()) list.add(readRecord(c));
        } finally {
            c.close();
        }
        return list;
    }

    public List<EnergyRecord> getRecords(long vehicleId, String type) {
        if (type == null || type.isEmpty()) return getRecords(vehicleId);
        List<EnergyRecord> list = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM records WHERE vehicle_id=? AND type=? ORDER BY ts DESC, id DESC",
                new String[]{String.valueOf(vehicleId), type});
        try {
            while (c.moveToNext()) list.add(readRecord(c));
        } finally {
            c.close();
        }
        return list;
    }

    public EnergyRecord getRecord(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM records WHERE id=?", new String[]{String.valueOf(id)});
        try {
            if (c.moveToFirst()) return readRecord(c);
        } finally {
            c.close();
        }
        return null;
    }

    public long saveRecord(EnergyRecord r) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = recordValues(r);
        if (r.id > 0) {
            db.update("records", cv, "id=?", new String[]{String.valueOf(r.id)});
            markChanged();
            return r.id;
        }
        long id = db.insert("records", null, cv);
        r.id = id;
        markChanged();
        return id;
    }

    public void insertRecords(List<EnergyRecord> records) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            for (EnergyRecord r : records) {
                ContentValues cv = recordValues(r);
                long id = db.insert("records", null, cv);
                r.id = id;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (!records.isEmpty()) markChanged();
    }

    public void deleteRecord(long id) {
        int rows = helper.getWritableDatabase().delete("records", "id=?", new String[]{String.valueOf(id)});
        if (rows > 0) markChanged();
    }

    public void clearRecords(long vehicleId) {
        int rows = helper.getWritableDatabase().delete("records", "vehicle_id=?",
                new String[]{String.valueOf(vehicleId)});
        if (rows > 0) markChanged();
    }

    /** 清空全部数据：所有车辆及其全部记录；下次读取会自动重建一辆空白默认车。 */
    public void clearAll() {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("records", null, null);
            db.delete("vehicles", null, null);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        prefs.edit().putLong(KEY_VEHICLE, -1).putBoolean(KEY_SEEDED, true).apply();
        markChanged();
    }

    public int recordCount(long vehicleId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM records WHERE vehicle_id=?",
                new String[]{String.valueOf(vehicleId)});
        try {
            if (c.moveToFirst()) return c.getInt(0);
        } finally {
            c.close();
        }
        return 0;
    }

    public Stats stats(Vehicle vehicle) {
        return StatsCalculator.compute(vehicle, getRecords(vehicle.id));
    }

    /** 按时间窗口统计（近一年 / 指定年 / 指定月 / 自定义区间）。 */
    public Stats stats(Vehicle vehicle, TimeRange range) {
        return StatsCalculator.compute(vehicle, getRecords(vehicle.id), range);
    }

    private ContentValues recordValues(EnergyRecord r) {
        ContentValues cv = new ContentValues();
        cv.put("vehicle_id", r.vehicleId);
        cv.put("type", r.type);
        cv.put("ts", r.timestamp);
        cv.put("odometer", r.odometer);
        cv.put("amount", r.amount);
        cv.put("quantity", r.quantity);
        cv.put("unit_price", r.unitPrice);
        cv.put("list_amount", r.listAmount);
        cv.put("discount", r.discount);
        cv.put("soc_start", r.socStart);
        cv.put("soc_end", r.socEnd);
        cv.put("oil_before", r.oilBefore);
        cv.put("oil_after", r.oilAfter);
        cv.put("remaining_oil", r.remainingOil);
        cv.put("remaining_soc", r.remainingSoc);
        cv.put("station", r.station == null ? "" : r.station);
        cv.put("fuel_type", r.fuelType == null ? "" : r.fuelType);
        cv.put("fuel_grade", r.fuelGrade == null ? "" : r.fuelGrade);
        cv.put("category", r.category == null ? "" : r.category);
        cv.put("note", r.note == null ? "" : r.note);
        cv.put("photo_path", r.photoPath == null ? "" : r.photoPath);
        cv.put("previous_recorded", r.previousRecorded ? 1 : 0);
        cv.put("created_at", r.createdAt > 0 ? r.createdAt : System.currentTimeMillis());
        return cv;
    }

    private EnergyRecord readRecord(Cursor c) {
        EnergyRecord r = new EnergyRecord();
        r.id = c.getLong(c.getColumnIndexOrThrow("id"));
        r.vehicleId = c.getLong(c.getColumnIndexOrThrow("vehicle_id"));
        r.type = c.getString(c.getColumnIndexOrThrow("type"));
        r.timestamp = c.getLong(c.getColumnIndexOrThrow("ts"));
        r.odometer = c.getDouble(c.getColumnIndexOrThrow("odometer"));
        r.amount = c.getDouble(c.getColumnIndexOrThrow("amount"));
        r.quantity = c.getDouble(c.getColumnIndexOrThrow("quantity"));
        r.unitPrice = c.getDouble(c.getColumnIndexOrThrow("unit_price"));
        r.listAmount = c.getDouble(c.getColumnIndexOrThrow("list_amount"));
        r.discount = c.getDouble(c.getColumnIndexOrThrow("discount"));
        r.socStart = c.getDouble(c.getColumnIndexOrThrow("soc_start"));
        r.socEnd = c.getDouble(c.getColumnIndexOrThrow("soc_end"));
        r.oilBefore = c.getDouble(c.getColumnIndexOrThrow("oil_before"));
        r.oilAfter = c.getDouble(c.getColumnIndexOrThrow("oil_after"));
        r.remainingOil = c.getDouble(c.getColumnIndexOrThrow("remaining_oil"));
        r.remainingSoc = c.getDouble(c.getColumnIndexOrThrow("remaining_soc"));
        r.station = c.getString(c.getColumnIndexOrThrow("station"));
        r.fuelType = c.getString(c.getColumnIndexOrThrow("fuel_type"));
        r.fuelGrade = c.getString(c.getColumnIndexOrThrow("fuel_grade"));
        r.category = c.getString(c.getColumnIndexOrThrow("category"));
        r.note = c.getString(c.getColumnIndexOrThrow("note"));
        r.photoPath = c.getString(c.getColumnIndexOrThrow("photo_path"));
        r.previousRecorded = c.getInt(c.getColumnIndexOrThrow("previous_recorded")) == 1;
        r.createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"));
        return r;
    }

    // ------------------------------------------------------------------ 初始化 / 备份

    public String backupDatabase() {
        File db = context.getDatabasePath(DbHelper.DB_NAME);
        File dir = new File(context.getFilesDir(), "backup");
        if (!dir.exists()) dir.mkdirs();
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        File out = new File(dir, "驭能管家_" + stamp + ".db");
        if (!db.exists() || !copy(db, out)) return null;
        prefs.edit().putInt(KEY_BACKUP_VERSION, prefs.getInt(KEY_CHANGE_VERSION, 0)).apply();
        return out.getAbsolutePath();
    }

    /** 把当前数据库写入用户通过系统文件选择器指定的位置（下载、文档、网盘等）。 */
    public boolean backupTo(Uri uri) {
        File db = context.getDatabasePath(DbHelper.DB_NAME);
        if (!db.exists() || uri == null) return false;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(db);
            out = context.getContentResolver().openOutputStream(uri, "w");
            if (out == null) return false;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
            prefs.edit().putInt(KEY_BACKUP_VERSION, prefs.getInt(KEY_CHANGE_VERSION, 0)).apply();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
            try {
                if (out != null) out.close();
            } catch (Exception ignored) {
            }
        }
    }

    /** 用用户选中的 .db 备份覆盖当前数据库，成功后返回 true。 */
    public boolean restoreFrom(Uri uri) {
        if (uri == null) return false;
        File db = context.getDatabasePath(DbHelper.DB_NAME);
        File dir = db.getParentFile();
        if (dir == null) return false;
        File tmp = new File(dir, DbHelper.DB_NAME + ".restoring");
        InputStream in = null;
        OutputStream out = null;
        try {
            // 先关掉连接，否则 Windows/部分设备上文件仍被占用。
            helper.close();
            if (tmp.exists() && !tmp.delete()) return false;
            in = context.getContentResolver().openInputStream(uri);
            if (in == null) return false;
            out = new FileOutputStream(tmp);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            out.flush();
            out.close();
            out = null;
            in.close();
            in = null;
            if (tmp.length() <= 0) return false;
            SQLiteDatabase.deleteDatabase(db);
            if (!tmp.renameTo(db)) return false;
            prefs.edit().putLong(KEY_VEHICLE, -1).putBoolean(KEY_SEEDED, true).apply();
            markChanged();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
            try {
                if (out != null) out.close();
            } catch (Exception ignored) {
            }
            if (tmp.exists()) {
                // rename 成功时这里已经是空操作；失败时清掉半成品，避免污染下次恢复。
                tmp.delete();
            }
        }
    }

    /** 某条记录相对上一条有里程记录之间的行程片段。 */
    public TripSlice tripSlice(EnergyRecord record) {
        if (record == null) return new TripSlice();
        return StatsCalculator.tripSlice(getRecords(record.vehicleId), record);
    }

    public String exportCsv() {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) dir = context.getFilesDir();
        File exportDir = new File(dir, "exports");
        if (!exportDir.exists()) exportDir.mkdirs();
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        File out = new File(exportDir, "驭能管家_" + stamp + ".csv");
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(CSV_HEADER);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT r.*, v.name AS vehicle_name FROM records r "
                + "JOIN vehicles v ON v.id=r.vehicle_id ORDER BY r.ts DESC", null);
        try {
            while (c.moveToNext()) {
                EnergyRecord r = readRecord(c);
                sb.append(csv(c.getString(c.getColumnIndexOrThrow("vehicle_name")))).append(',')
                        .append(csv(r.typeLabel())).append(',')
                        .append(csv(r.fuelLabel())).append(',')
                        .append(csv(com.yuneng.guanjia.util.Formats.dateTime(r.timestamp))).append(',')
                        .append(r.odometer).append(',')
                        .append(r.amount).append(',')
                        .append(r.quantity).append(',')
                        .append(r.unitPrice).append(',')
                        .append(r.socStart).append(',')
                        .append(r.socEnd).append(',')
                        .append(r.oilBefore).append(',')
                        .append(r.oilAfter).append(',')
                        .append(csv(r.station)).append(',')
                        .append(csv(r.note)).append(',')
                        .append(csv(r.category)).append(',')
                        .append(r.remainingOil).append(',')
                        .append(r.remainingSoc).append(',')
                        .append(r.amount).append(',')
                        .append(r.discount).append(',')
                        .append(r.unitPrice).append('\n');
            }
        } finally {
            c.close();
        }
        try (OutputStream os = new FileOutputStream(out)) {
            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
        return out.getAbsolutePath();
    }

    private String csv(String s) {
        if (s == null) return "";
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    /** 生成 CSV 导入模板（表头 + 三行示例），返回文件绝对路径。 */
    public String exportTemplate() {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) dir = context.getFilesDir();
        File exportDir = new File(dir, "exports");
        if (!exportDir.exists()) exportDir.mkdirs();
        File out = new File(exportDir, "驭能管家_导出模板.csv");
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append(CSV_HEADER);
        sb.append("\"我的爱车\",\"充电\",\"\",\"2026-01-05 18:30\",\"12000\",\"36.50\",\"50\",\"0.73\",\"20\",\"80\",\"\",\"\",\"示例充电站\",\"填写完可删除示例行\",\"\",\"55\",\"\",\"36.50\",\"0.00\",\"0.73\"\n");
        sb.append("\"我的爱车\",\"加油\",\"汽油 · 92#\",\"2026-01-08 09:15\",\"12300\",\"300.00\",\"40\",\"7.50\",\"\",\"\",\"25\",\"75\",\"示例加油站\",\"油品可留空，默认取车型油品\",\"\",\"\",\"45\",\"300.00\",\"0.00\",\"7.50\"\n");
        sb.append("\"我的爱车\",\"支出\",\"\",\"2026-01-10 12:00\",\"\",\"120.00\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"\",\"停车费\",\"\",\"\",\"120.00\",\"0.00\",\"\"\n");
        try (OutputStream os = new FileOutputStream(out)) {
            os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return null;
        }
        return out.getAbsolutePath();
    }

    /** 导入结果：success>0 时表示至少写入一条。 */
    public static class ImportResult {
        public int imported;
        public int skipped;
        public boolean failed;
        public String message = "";
    }

    /** 读取用户选择的 CSV 并追加到对应车辆，不删除已有记录。 */
    public ImportResult importCsv(Uri uri) {
        ImportResult result = new ImportResult();
        if (uri == null) {
            result.failed = true;
            result.message = "没有选择文件。";
            return result;
        }
        String text;
        try {
            text = readCsvText(uri);
        } catch (Exception e) {
            result.failed = true;
            result.message = "无法读取所选文件，请确认文件仍可访问。";
            return result;
        }
        if (text == null || text.trim().isEmpty()) {
            result.failed = true;
            result.message = "文件是空的。";
            return result;
        }
        List<String[]> rows = splitCsv(text);
        if (rows.size() < 2) {
            result.failed = true;
            result.message = "文件里没有数据行（至少需要 1 行表头和 1 行记录）。";
            return result;
        }
        int[] map = headerMap(rows.get(0));
        if (map == null) {
            result.failed = true;
            result.message = "表头无法识别，请使用「导入模板」填写后再导入。";
            return result;
        }
        List<EnergyRecord> records = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String type = normalizeType(cell(row, map, 1));
            long ts = parseTime(cell(row, map, 3));
            if (type == null || ts <= 0) {
                result.skipped++;
                continue;
            }
            EnergyRecord r = new EnergyRecord();
            r.vehicleId = vehicleIdFor(cell(row, map, 0));
            r.type = type;
            r.timestamp = ts;
            r.odometer = Formats.parse(cell(row, map, 4), 0);
            r.amount = Formats.parse(cell(row, map, 5), 0);
            r.quantity = Formats.parse(cell(row, map, 6), 0);
            r.unitPrice = Formats.parse(cell(row, map, 7), 0);
            r.socStart = Formats.parse(cell(row, map, 8), 0);
            r.socEnd = Formats.parse(cell(row, map, 9), 0);
            r.oilBefore = Formats.parse(cell(row, map, 10), 0);
            r.oilAfter = Formats.parse(cell(row, map, 11), 0);
            r.station = cell(row, map, 12);
            r.note = cell(row, map, 13);
            r.category = cell(row, map, 14);
            r.remainingOil = Formats.parse(cell(row, map, 15), 0);
            r.remainingSoc = Formats.parse(cell(row, map, 16), 0);
            // 新版本多导出的「实付金额 / 优惠金额 / 实付单价」三列，有值就覆盖前面的同名口径。
            double actualAmount = Formats.parse(cell(row, map, 17), -1);
            if (actualAmount >= 0) r.amount = actualAmount;
            r.discount = Formats.parse(cell(row, map, 18), 0);
            double actualPrice = Formats.parse(cell(row, map, 19), -1);
            if (actualPrice >= 0) r.unitPrice = actualPrice;
            applyFuelText(r, cell(row, map, 2));
            r.createdAt = now;
            records.add(r);
            result.imported++;
        }
        if (!records.isEmpty()) insertRecords(records);
        if (result.imported == 0) {
            result.failed = true;
            result.message = "没有导入任何记录：类型或时间无法识别，请对照模板检查「类型」和「时间」两列。";
        }
        return result;
    }

    /** 把外部文件目录下的文件转成可分享的 content:// 地址。 */
    public Uri shareUri(String path) {
        if (path == null) return null;
        File root = context.getExternalFilesDir(null);
        if (root == null) root = context.getFilesDir();
        String base = root.getAbsolutePath();
        String abs = new File(path).getAbsolutePath();
        String rel = abs.startsWith(base) ? abs.substring(base.length()) : "/" + new File(path).getName();
        if (!rel.startsWith("/")) rel = "/" + rel;
        return Uri.parse("content://" + CsvProvider.AUTHORITY + rel);
    }

    private String readCsvText(Uri uri) throws IOException {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("empty stream");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        try {
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
        } finally {
            in.close();
        }
        byte[] data = bos.toByteArray();
        String text = new String(data, StandardCharsets.UTF_8);
        if (text.indexOf('\uFFFD') >= 0) {
            try {
                text = new String(data, "GBK");
            } catch (Exception ignored) {
            }
        }
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        return text;
    }

    private List<String[]> splitCsv(String text) {
        List<String[]> rows = new ArrayList<>();
        String[] lines = text.split("\r\n|\n|\r", -1);
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            rows.add(splitCsvLine(line));
        }
        return rows;
    }

    private String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private int[] headerMap(String[] header) {
        int[] map = new int[CSV_COLUMNS.length];
        java.util.Arrays.fill(map, -1);
        for (int i = 0; i < header.length; i++) {
            String h = header[i].trim();
            if (h.isEmpty()) continue;
            for (int k = 0; k < CSV_COLUMNS.length; k++) {
                if (map[k] < 0 && h.startsWith(CSV_COLUMNS[k])) map[k] = i;
            }
        }
        int found = 0;
        for (int v : map) if (v >= 0) found++;
        return found >= 5 ? map : null;
    }

    private String cell(String[] row, int[] map, int key) {
        int i = map[key];
        return i >= 0 && i < row.length ? row[i].trim() : "";
    }

    private String normalizeType(String s) {
        String v = s == null ? "" : s.trim();
        if (v.isEmpty()) return null;
        String u = v.toUpperCase(Locale.CHINA);
        if (v.contains("充电") || u.equals("CHARGE") || u.equals("CHARGING")) return EnergyRecord.TYPE_CHARGE;
        if (v.contains("加油") || u.equals("FUEL")) return EnergyRecord.TYPE_FUEL;
        if (v.contains("收入") || u.equals("INCOME")) return EnergyRecord.TYPE_INCOME;
        if (v.contains("支出") || u.equals("EXPENSE")) return EnergyRecord.TYPE_EXPENSE;
        return null;
    }

    private long parseTime(String s) {
        String v = s == null ? "" : s.trim();
        if (v.isEmpty()) return -1;
        for (String p : TIME_PATTERNS) {
            SimpleDateFormat f = new SimpleDateFormat(p, Locale.CHINA);
            f.setLenient(false);
            try {
                Date d = f.parse(v);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    private long vehicleIdFor(String name) {
        String n = name == null ? "" : name.trim();
        if (!n.isEmpty()) {
            for (Vehicle v : getVehicles()) {
                if (n.equals(v.name)) return v.id;
            }
        }
        return getCurrentVehicle().id;
    }

    private void applyFuelText(EnergyRecord r, String text) {
        if (!EnergyRecord.TYPE_FUEL.equals(r.type)) return;
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            r.fuelType = FuelTypes.findType(null, r.fuelGrade);
            return;
        }
        int dot = t.indexOf('·');
        if (dot > 0) {
            r.fuelType = t.substring(0, dot).trim();
            r.fuelGrade = t.substring(dot + 1).trim();
            return;
        }
        for (String f : FuelTypes.TYPES) {
            if (f.equals(t)) {
                r.fuelType = f;
                r.fuelGrade = FuelTypes.bestGrade(f, "");
                return;
            }
        }
        r.fuelGrade = t;
        r.fuelType = FuelTypes.typeForGrade(t);
    }

    public String importPhoto(Uri uri) {
        if (uri == null) return null;
        File dir = new File(context.getFilesDir(), "photos");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, "photo_" + System.currentTimeMillis() + ".jpg");
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) return null;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
            return out.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private boolean copy(File in, File out) {
        try (FileInputStream fis = new FileInputStream(in);
             FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) > 0) fos.write(buf, 0, n);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
