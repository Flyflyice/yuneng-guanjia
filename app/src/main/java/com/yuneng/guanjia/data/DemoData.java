package com.yuneng.guanjia.data;

import com.yuneng.guanjia.model.EnergyRecord;
import com.yuneng.guanjia.model.FuelTypes;
import com.yuneng.guanjia.model.Vehicle;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

/**
 * 首次启动的演示数据。刻意与参考截图保持一致：
 * 总里程 17664 km、平均油耗 3.15 L/100km、平均电耗 12.27 kWh/100km、
 * 综合能耗 6.95 L/100km、最近一次充电 11.00 度 / 21%→83%。
 */
public final class DemoData {
    private static final double TARGET_FUEL = 556.416;
    private static final double TARGET_KWH = 2167.4;
    private static final double TARGET_FUEL_COST = 4123.6;
    private static final double TARGET_CHARGE_COST = 1107.12;
    private static final double ANCHOR_KWH = 48.0;

    private DemoData() {
    }

    public static List<EnergyRecord> build(Vehicle vehicle) {
        List<EnergyRecord> out = new ArrayList<>();
        Random rnd = new Random(20261117L);

        final int olderCount = 193;
        long start = at(2025, 6, 18, 8, 0);
        long end = at(2026, 11, 7, 19, 0);
        long span = end - start;

        long[] times = new long[olderCount];
        for (int i = 0; i < olderCount; i++) {
            long base = start + span * i / (olderCount - 1);
            long jitter = i == 0 || i == olderCount - 1 ? 0 : (rnd.nextInt(9) - 4) * 3600000L;
            times[i] = base + jitter;
            if (i > 0 && times[i] <= times[i - 1] + 5 * 3600000L) {
                times[i] = times[i - 1] + 5 * 3600000L;
            }
        }
        times[olderCount - 1] = end;

        double[] odometers = new double[olderCount];
        odometers[0] = 12;
        double[] raw = new double[olderCount];
        double sum = 0;
        for (int i = 1; i < olderCount; i++) {
            raw[i] = 48 + rnd.nextInt(92);
            if (i % 19 == 0) raw[i] += 190;
            if (i % 41 == 0) raw[i] += 120;
            sum += raw[i];
        }
        double scale = (17546 - 12) / sum;
        for (int i = 1; i < olderCount; i++) {
            odometers[i] = odometers[i - 1] + Math.max(22, raw[i] * scale);
        }
        // 消除累计误差，让最后一辆老记录稳稳落在 17546。
        double diff = 17546 - odometers[olderCount - 1];
        for (int i = 1; i < olderCount; i++) {
            odometers[i] += diff * i / (olderCount - 1);
        }

        boolean[] fuel = new boolean[olderCount];
        int fuelCount = 0;
        for (int i = 0; i < olderCount; i++) {
            int a = i * 37 / olderCount;
            int b = (i + 1) * 37 / olderCount;
            fuel[i] = b > a;
            if (fuel[i]) fuelCount++;
        }
        // 最后一条老记录固定为加油，用来展示“最近油耗 10.19”。
        if (!fuel[olderCount - 1]) {
            fuel[olderCount - 1] = true;
            for (int i = olderCount - 2; i >= 0; i--) {
                if (fuel[i]) {
                    fuel[i] = false;
                    break;
                }
            }
        }

        List<EnergyRecord> charges = new ArrayList<>();
        List<EnergyRecord> fuels = new ArrayList<>();
        String[] chargeStations = {"车库充", "国家电网", "特来电", "星星充电", "小桔充电"};
        String[] fuelStations = {"中石化", "中石油", "壳牌", "中海油"};

        // 用连续的电量/油量状态机生成剩余油量、剩余电量，便于演示“纯电/燃油里程”拆分：
        // 每段行驶电量都会下降；油量只在“长途段”（每 23 条一次）下降，其余算纯电行驶。
        double curSoc = 40;
        double curOil = 60;
        for (int i = 0; i < olderCount; i++) {
            EnergyRecord r = new EnergyRecord();
            r.vehicleId = vehicle.id;
            r.timestamp = times[i];
            r.odometer = odometers[i];
            r.previousRecorded = true;
            r.createdAt = times[i];
            boolean longTrip = i % 23 == 0;
            if (longTrip) {
                // 长途高速：只烧油、电量基本不动 → 形成纯油段。
                curOil = Math.max(8, curOil - (8 + rnd.nextInt(12)));
            } else {
                // 日常通勤：只用电 → 形成纯电段。
                curSoc = Math.max(6, curSoc - (20 + rnd.nextInt(18)));
            }
            if (fuel[i]) {
                r.type = EnergyRecord.TYPE_FUEL;
                r.station = fuelStations[rnd.nextInt(fuelStations.length)];
                r.fuelType = FuelTypes.GASOLINE;
                r.fuelGrade = "92#";
                r.oilBefore = Math.round(curOil);
                r.oilAfter = 92 + rnd.nextInt(9);
                r.remainingSoc = Math.round(curSoc);
                r.unitPrice = 7.37 + rnd.nextInt(9) / 100.0;
                r.quantity = 15.0;
                r.discount = 0;
                curOil = r.oilAfter;
                fuels.add(r);
            } else {
                r.type = EnergyRecord.TYPE_CHARGE;
                // 电池还接近满电时不会马上充电：先消耗到中段电量，避免出现 0 或负的充电量。
                if (curSoc > 88) curSoc = 55 + rnd.nextInt(25);
                r.station = chargeStations[rnd.nextInt(chargeStations.length)];
                r.socStart = Math.round(curSoc);
                r.socEnd = 95 + rnd.nextInt(6);
                r.remainingOil = Math.round(curOil);
                r.unitPrice = 0.46 + rnd.nextInt(12) / 100.0;
                double delta = r.socEnd - r.socStart;
                r.quantity = vehicle.batteryCapacity * delta / 100.0 * (0.96 + rnd.nextInt(9) / 100.0);
                r.amount = r.quantity * r.unitPrice;
                r.note = String.valueOf(1600 + i);
                curSoc = r.socEnd;
                charges.add(r);
            }
        }

        // 最后一条加油：按“上次加油到本次加油”的里程反推，保证最近油耗为 10.19。
        EnergyRecord latestFuel = fuels.get(fuels.size() - 1);
        latestFuel.odometer = odometers[olderCount - 1];
        latestFuel.unitPrice = 7.41;
        EnergyRecord prevFuel = fuels.size() >= 2 ? fuels.get(fuels.size() - 2) : null;
        double latestFuelDistance = prevFuel == null ? 140 : Math.max(20,
                latestFuel.odometer - prevFuel.odometer);
        latestFuel.quantity = 10.19 * latestFuelDistance / 100.0;
        latestFuel.amount = latestFuel.quantity * latestFuel.unitPrice;
        latestFuel.listAmount = latestFuel.amount;
        latestFuel.note = "通勤";

        // 校准总油量（保留最后一条的 10.19 展示值）。
        double otherFuel = 0;
        for (int i = 0; i < fuels.size() - 1; i++) otherFuel += fuels.get(i).quantity;
        double fuelScale = Math.max(0.5, (TARGET_FUEL - latestFuel.quantity) / Math.max(1, otherFuel));
        for (int i = 0; i < fuels.size() - 1; i++) {
            EnergyRecord r = fuels.get(i);
            r.quantity *= fuelScale;
            r.amount = r.quantity * r.unitPrice;
            r.listAmount = r.amount;
        }

        // 校准总电量（保留 4 条截图锚点）。
        double anchorKwh = 12 + 12 + 13 + 11;
        double olderKwh = 0;
        for (EnergyRecord r : charges) olderKwh += r.quantity;
        double chargeScale = Math.max(0.5, (TARGET_KWH - anchorKwh) / Math.max(1, olderKwh));
        for (EnergyRecord r : charges) {
            r.quantity *= chargeScale;
            r.amount = r.quantity * r.unitPrice;
        }

        out.addAll(charges);
        out.addAll(fuels);

        // 四条与截图一致的充电记录。
        out.add(anchorCharge(vehicle, at(2026, 11, 8, 10, 20), 17576, 6.24, 12.00,
                0.52, 28, 100, 18, "车库充", "1602"));
        out.add(anchorCharge(vehicle, at(2026, 11, 9, 18, 40), 17606, 6.24, 12.00,
                0.52, 28, 100, 18, "车库充", "1614"));
        out.add(anchorCharge(vehicle, at(2026, 11, 11, 19, 5), 17632, 6.76, 13.00,
                0.52, 29, 100, 18, "车库充", "1627"));
        out.add(anchorCharge(vehicle, at(2026, 11, 16, 12, 22), 17676, 5.72, 11.00,
                0.52, 21, 83, 18, "车库充", "1638"));

        // 金额校准：油费、电费对齐截图口径（4 条锚点充电保持原金额）。
        double anchorAmount = 6.24 + 6.24 + 6.76 + 5.72;
        double fuelSum = 0;
        for (EnergyRecord r : fuels) fuelSum += r.amount;
        if (fuelSum > 0.01) {
            double k = TARGET_FUEL_COST / fuelSum;
            for (EnergyRecord r : fuels) {
                r.unitPrice *= k;
                r.amount = r.quantity * r.unitPrice;
                r.listAmount = r.amount;
            }
        }
        double chargeSum = 0;
        for (EnergyRecord r : charges) chargeSum += r.amount;
        if (chargeSum > 0.01) {
            double k = (TARGET_CHARGE_COST - anchorAmount) / chargeSum;
            for (EnergyRecord r : charges) {
                r.unitPrice *= k;
                r.amount = r.quantity * r.unitPrice;
            }
        }

        // 其他支出，合计 6552.4 元。
        out.add(expense(vehicle, at(2025, 9, 12, 10, 0), "保险和交通规费", 3200, "全年保险"));
        out.add(expense(vehicle, at(2025, 10, 3, 16, 30), "高速费", 620.4, "国庆自驾"));
        out.add(expense(vehicle, at(2025, 10, 18, 9, 10), "停车费", 260, "小区月卡"));
        out.add(expense(vehicle, at(2026, 1, 15, 14, 0), "维修", 880, "更换雨刮与补漆"));
        out.add(expense(vehicle, at(2026, 2, 8, 11, 20), "洗车", 168, "精洗套餐"));
        out.add(expense(vehicle, at(2026, 3, 22, 15, 30), "维修", 680, "常规保养"));
        out.add(expense(vehicle, at(2026, 5, 30, 8, 40), "高速费", 420, "往返北戴河"));
        out.add(expense(vehicle, at(2026, 7, 25, 18, 0), "停车费", 125, "商场停车"));
        out.add(expense(vehicle, at(2026, 8, 19, 12, 0), "其他", 199, "车载用品"));
        EnergyRecord income = new EnergyRecord();
        income.vehicleId = vehicle.id;
        income.type = EnergyRecord.TYPE_INCOME;
        income.timestamp = at(2026, 9, 6, 13, 0);
        income.amount = 300;
        income.category = "报销";
        income.note = "公司油费补贴";
        income.createdAt = income.timestamp;
        out.add(income);

        return out;
    }

    private static EnergyRecord anchorCharge(Vehicle vehicle, long ts, double odo,
                                             double amount, double qty, double price,
                                             double socStart, double socEnd, double oil,
                                             String station, String note) {
        EnergyRecord r = new EnergyRecord();
        r.vehicleId = vehicle.id;
        r.type = EnergyRecord.TYPE_CHARGE;
        r.timestamp = ts;
        r.odometer = odo;
        r.amount = amount;
        r.quantity = qty;
        r.unitPrice = price;
        r.socStart = socStart;
        r.socEnd = socEnd;
        r.remainingOil = oil;
        r.station = station;
        r.note = note;
        r.previousRecorded = true;
        r.createdAt = ts;
        return r;
    }

    private static EnergyRecord expense(Vehicle vehicle, long ts, String category,
                                        double amount, String note) {
        EnergyRecord r = new EnergyRecord();
        r.vehicleId = vehicle.id;
        r.type = EnergyRecord.TYPE_EXPENSE;
        r.timestamp = ts;
        r.amount = amount;
        r.category = category;
        r.note = note;
        r.createdAt = ts;
        return r;
    }

    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month - 1);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
