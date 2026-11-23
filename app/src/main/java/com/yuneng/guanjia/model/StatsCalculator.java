package com.yuneng.guanjia.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 纯 Java 统计引擎，便于在 JVM 上做单元测试。
 *
 * 计算口径：
 *  - 平均油耗 = 总加油量 / 总里程 × 100
 *  - 平均电耗 = 总充电量 / 总里程 × 100
 *  - 插混综合能耗 = 平均油耗 + 平均电耗 × 0.31（1 kWh 折合 0.31 L 汽油）
 *  - 第五代 DM-i 等效油耗 = 平均油耗 + 平均电耗 / (8.9 × 46.06% × 92%)
 *
 * 时间窗口：可传入 {@link TimeRange} 只统计某个自然年、自然月或自定义起止日期；
 * 不传时等价于全部记录（保持旧口径）。
 */
public final class StatsCalculator {
    public static final double KWH_TO_LITER = 0.31;
    public static final double GASOLINE_KWH_PER_LITER = 8.9;
    public static final double DEFAULT_THERMAL_EFFICIENCY = 0.4606;
    public static final double DEFAULT_DRIVE_EFFICIENCY = 0.92;

    private StatsCalculator() {
    }

    /**
     * 单条记录相对上一条有里程记录之间的行程片段。
     *
     * @param records 该车全部记录，需按时间倒序排列
     * @param record  目标记录
     */
    public static TripSlice tripSlice(List<EnergyRecord> records, EnergyRecord record) {
        TripSlice t = new TripSlice();
        if (record == null) return t;
        t.record = record;
        t.kwh = record.isCharge() ? Math.max(0, record.quantity) : 0;
        t.fuel = record.isFuel() ? Math.max(0, record.quantity) : 0;
        t.cost = Math.max(0, record.amount);
        EnergyRecord prev = null;
        if (records != null) {
            for (EnergyRecord o : records) {
                if (o == record) continue;
                if (record.id > 0 && o.id == record.id) continue;
                if (!o.isEnergy() || o.odometer <= 0) continue;
                if (o.timestamp >= record.timestamp) continue;
                prev = o;
                break;
            }
        }
        t.previous = prev;
        if (prev != null && record.odometer > prev.odometer) {
            t.distance = record.odometer - prev.odometer;
            long ms = Math.max(0, record.timestamp - prev.timestamp);
            t.days = (int) (ms / 86400000L) + 1;
            if (t.days < 1) t.days = 1;
            t.kwhPer100 = t.kwh / t.distance * 100.0;
            t.lPer100 = t.fuel / t.distance * 100.0;
            t.costPerKm = t.cost / t.distance;
        }
        return t;
    }

    public static Stats compute(Vehicle vehicle, List<EnergyRecord> source) {
        return compute(vehicle, source, TimeRange.all());
    }

    public static Stats compute(Vehicle vehicle, List<EnergyRecord> source, TimeRange range) {
        if (range == null) range = TimeRange.all();
        Stats s = new Stats();
        if (vehicle == null) vehicle = new Vehicle();
        if (source == null) source = Collections.emptyList();

        List<EnergyRecord> sorted = new ArrayList<>(source);
        Collections.sort(sorted, new Comparator<EnergyRecord>() {
            @Override
            public int compare(EnergyRecord a, EnergyRecord b) {
                int c = Long.compare(a.timestamp, b.timestamp);
                if (c != 0) return c;
                return Long.compare(a.id, b.id);
            }
        });

        // 只保留时间窗口内的记录
        List<EnergyRecord> all = new ArrayList<>();
        for (EnergyRecord r : sorted) {
            if (r != null && range.contains(r.timestamp)) all.add(r);
        }

        // 窗口起点之前最近的能耗记录，作为里程基线：
        // 这样“2025 年”的里程 = 2024 年最后一条里程 → 2025 年最后一条里程。
        EnergyRecord baseline = null;
        for (EnergyRecord r : sorted) {
            if (r == null) continue;
            if (r.timestamp >= range.startMs) break;
            if (r.isEnergy() && r.odometer > 0) baseline = r;
        }

        List<EnergyRecord> energy = new ArrayList<>();
        for (EnergyRecord r : all) {
            if (r.isEnergy() && r.odometer > 0) energy.add(r);
        }
        if (energy.isEmpty()) {
            s.thermalEfficiency = vehicle.thermalEfficiency > 0
                    ? vehicle.thermalEfficiency : DEFAULT_THERMAL_EFFICIENCY;
            s.driveEfficiency = DEFAULT_DRIVE_EFFICIENCY;
            s.batteryCapacity = vehicle.batteryCapacity;
            s.batteryHealth = 100;
            s.days = emptyDays(range);
            s.activeDays = 1;
            s.dmKwhPerL = GASOLINE_KWH_PER_LITER * s.thermalEfficiency * s.driveEfficiency;
            buildMonths(s, all, energy, vehicle, range, baseline);
            return s;
        }

        List<EnergyRecord> byOdo = new ArrayList<>(energy);
        Collections.sort(byOdo, new Comparator<EnergyRecord>() {
            @Override
            public int compare(EnergyRecord a, EnergyRecord b) {
                return Double.compare(a.odometer, b.odometer);
            }
        });

        s.recordCount = energy.size();
        s.firstTs = all.get(0).timestamp;
        s.lastTs = all.get(all.size() - 1).timestamp;
        s.days = computeDays(range, s.firstTs, s.lastTs);
        java.util.HashSet<Long> active = new java.util.HashSet<>();
        for (EnergyRecord r : energy) {
            active.add(dayKey(r.timestamp));
        }
        s.activeDays = Math.max(1, active.size());

        double firstOdo = baseline != null ? baseline.odometer : byOdo.get(0).odometer;
        double lastOdo = byOdo.get(byOdo.size() - 1).odometer;
        s.distance = Math.max(0, lastOdo - firstOdo);

        for (EnergyRecord r : all) {
            if (r.isCharge()) {
                s.chargeCount++;
                s.totalKwh += Math.max(0, r.quantity);
                s.chargeCost += Math.max(0, r.amount);
            } else if (r.isFuel()) {
                s.fuelCount++;
                s.totalFuel += Math.max(0, r.quantity);
                s.fuelCost += Math.max(0, r.amount);
            } else if (r.isExpense()) {
                s.otherExpense += Math.max(0, r.amount);
            } else if (r.isIncome()) {
                s.income += Math.max(0, r.amount);
            }
        }

        s.energyCost = s.chargeCost + s.fuelCost;
        s.totalCost = s.energyCost + s.otherExpense;

        if (s.distance > 0.01) {
            s.avgFuel = s.totalFuel / s.distance * 100.0;
            s.avgKwh = s.totalKwh / s.distance * 100.0;
            s.costPerKm = s.energyCost / s.distance;
            s.energyCostPerKm = s.costPerKm;
            s.totalCostPerKm = s.totalCost / s.distance;
        }
        if (vehicle.isElectric()) {
            s.combined = s.avgKwh;
        } else if (vehicle.isFuelOnly()) {
            s.combined = s.avgFuel;
        } else {
            s.combined = s.avgFuel + s.avgKwh * KWH_TO_LITER;
        }
        s.costPerDay = s.totalCost / Math.max(1, s.days);
        s.avgChargePrice = s.totalKwh > 0.01 ? s.chargeCost / s.totalKwh : 0;
        s.avgFuelPrice = s.totalFuel > 0.01 ? s.fuelCost / s.totalFuel : 0;

        computeLatest(s, all, vehicle);
        computeBattery(s, all, vehicle);
        buildMonths(s, all, energy, vehicle, range, baseline);
        computeSplit(s, all, vehicle);
        detectAnomalies(s, all, vehicle);
        return s;
    }

    /**
     * 统计窗口覆盖多少天。
     *  - 全部记录：沿用“首条到最后一条记录”的跨度（历史口径）；
     *  - 时间窗口：用窗口起止与今天取交集，避免把未来月份算进“每天成本”。
     */
    private static int computeDays(TimeRange range, long firstTs, long lastTs) {
        if (!range.isAll()) {
            long to = Math.min(range.endMs, System.currentTimeMillis());
            if (to <= range.startMs) return Math.max(1, range.spanDays());
            return (int) Math.max(1, (to - range.startMs) / 86400000L);
        }
        if (firstTs <= 0 || lastTs < firstTs) return 1;
        return (int) Math.max(1, (lastTs - firstTs) / 86400000L + 1);
    }

    private static int emptyDays(TimeRange range) {
        if (!range.isAll()) {
            long to = Math.min(range.endMs, System.currentTimeMillis());
            if (to <= range.startMs) return Math.max(1, range.spanDays());
            return (int) Math.max(1, (to - range.startMs) / 86400000L);
        }
        return 1;
    }

    /**
     * 用“剩余油量 / 剩余电量”把行驶里程拆成纯电、纯油、混动三段。
     *  - 充电记录补能后的状态：电量 = socEnd，油量 = remainingOil；
     *  - 加油记录补能前的状态：电量 = remainingSoc，油量 = oilBefore；
     *  相邻两条补能记录之间，电量下降而油量基本不变 → 纯电里程；油量下降而电量基本不变 → 纯油里程；
     *  两者都下降 → 混动里程。纯电里程用于“纯电电耗”，纯油里程用于“纯油油耗”。
     */
    private static void computeSplit(Stats s, List<EnergyRecord> all, Vehicle vehicle) {
        if (s.distance <= 0.01) return;
        if (vehicle.isElectric()) {
            s.electricDistance = s.distance;
            s.pureElectricEconomy = s.totalKwh / s.distance * 100.0;
            return;
        }
        if (vehicle.isFuelOnly()) {
            s.fuelDistance = s.distance;
            s.pureFuelEconomy = s.totalFuel / s.distance * 100.0;
            return;
        }
        double elecDist = 0;
        double fuelDist = 0;
        double hybridDist = 0;
        EnergyRecord prev = null;
        for (EnergyRecord r : all) {
            if (!r.isEnergy()) continue;
            double socBefore = r.isCharge() ? r.socStart : r.remainingSoc;
            double oilBefore = r.isCharge() ? r.remainingOil : r.oilBefore;
            if (prev != null) {
                double d = r.odometer - prev.odometer;
                if (d > 1 && d < 5000) {
                    double socAfter = prev.isCharge() ? prev.socEnd : prev.remainingSoc;
                    double oilAfter = prev.isCharge() ? prev.remainingOil : prev.oilAfter;
                    if (socAfter > 0 && socBefore > 0 && oilAfter > 0 && oilBefore > 0) {
                        double dSoc = socBefore - socAfter;
                        double dOil = oilBefore - oilAfter;
                        boolean elecDown = dSoc <= -2;
                        boolean oilDown = dOil <= -2;
                        if (elecDown && !oilDown) elecDist += d;
                        else if (oilDown && !elecDown) fuelDist += d;
                        else if (elecDown && oilDown) hybridDist += d;
                    }
                }
            }
            prev = r;
        }
        s.electricDistance = elecDist;
        s.fuelDistance = fuelDist;
        s.hybridDistance = hybridDist;
        if (elecDist > 50) s.pureElectricEconomy = s.totalKwh / elecDist * 100.0;
        if (fuelDist > 50) s.pureFuelEconomy = s.totalFuel / fuelDist * 100.0;
    }

    private static void computeLatest(Stats s, List<EnergyRecord> all, Vehicle vehicle) {
        EnergyRecord latestCharge = null;
        EnergyRecord prevCharge = null;
        EnergyRecord latestFuel = null;
        EnergyRecord prevFuel = null;

        for (EnergyRecord r : all) {
            if (r.isCharge()) {
                prevCharge = latestCharge;
                latestCharge = r;
            } else if (r.isFuel()) {
                prevFuel = latestFuel;
                latestFuel = r;
            }
        }

        if (latestCharge != null && prevCharge != null) {
            double d = latestCharge.odometer - prevCharge.odometer;
            if (d > 1) {
                s.latestElectricEconomy = latestCharge.quantity / d * 100.0;
            }
        }
        if (latestFuel != null && prevFuel != null) {
            double d = latestFuel.odometer - prevFuel.odometer;
            if (d > 1) {
                s.latestFuelEconomy = latestFuel.quantity / d * 100.0;
            }
        }

        EnergyRecord latestEnergy = null;
        EnergyRecord prevEnergy = null;
        for (EnergyRecord r : all) {
            if (!r.isEnergy()) continue;
            prevEnergy = latestEnergy;
            latestEnergy = r;
        }
        if (latestEnergy != null) {
            if (prevEnergy != null) {
                double d = latestEnergy.odometer - prevEnergy.odometer;
                if (d > 1) {
                    s.latestDistance = d;
                    s.latestCostPerKm = latestEnergy.amount / d;
                }
            }
            if (vehicle.isElectric()) {
                s.latestCombined = s.latestElectricEconomy > 0
                        ? s.latestElectricEconomy : s.latestFuelEconomy;
            } else if (vehicle.isFuelOnly()) {
                s.latestCombined = s.latestFuelEconomy > 0
                        ? s.latestFuelEconomy : s.avgFuel;
            } else {
                if (s.latestFuelEconomy > 0 && isRecent(latestFuel, 60L * 86400000L)) {
                    s.latestCombined = s.latestFuelEconomy;
                } else if (s.latestElectricEconomy > 0) {
                    s.latestCombined = s.latestElectricEconomy * KWH_TO_LITER;
                } else {
                    s.latestCombined = s.combined;
                }
            }
        }
        if (s.latestCombined <= 0) s.latestCombined = s.combined;
        if (s.latestCostPerKm <= 0) s.latestCostPerKm = s.costPerKm;
    }

    private static boolean isRecent(EnergyRecord r, long windowMs) {
        return r != null && r.timestamp > 0
                && Math.abs(System.currentTimeMillis() - r.timestamp) < windowMs;
    }

    private static void computeBattery(Stats s, List<EnergyRecord> all, Vehicle vehicle) {
        s.thermalEfficiency = vehicle.thermalEfficiency > 0
                ? vehicle.thermalEfficiency : DEFAULT_THERMAL_EFFICIENCY;
        s.driveEfficiency = DEFAULT_DRIVE_EFFICIENCY;
        s.dmKwhPerL = GASOLINE_KWH_PER_LITER * s.thermalEfficiency * s.driveEfficiency;
        s.dmEquivalent = s.avgFuel + (s.dmKwhPerL > 0.01 ? s.avgKwh / s.dmKwhPerL : 0);

        List<Double> estimates = new ArrayList<>();
        for (EnergyRecord r : all) {
            double cap = r.batteryCapacityEstimate();
            if (cap < 2) continue;
            if (vehicle.batteryCapacity > 0) {
                if (cap > vehicle.batteryCapacity * 1.45) continue;
                if (cap < vehicle.batteryCapacity * 0.55) continue;
            }
            estimates.add(cap);
            Stats.CapacityPoint p = new Stats.CapacityPoint();
            p.ts = r.timestamp;
            p.kwh = cap;
            s.capacities.add(p);
        }
        if (s.capacities.size() > 24) {
            s.capacities.subList(0, s.capacities.size() - 24).clear();
        }
        if (!estimates.isEmpty()) {
            int from = Math.max(0, estimates.size() - 8);
            double sum = 0;
            int n = 0;
            for (int i = from; i < estimates.size(); i++) {
                sum += estimates.get(i);
                n++;
            }
            s.batteryCapacity = n > 0 ? sum / n : vehicle.batteryCapacity;
        } else {
            s.batteryCapacity = vehicle.batteryCapacity;
        }
        if (vehicle.batteryCapacity > 0.01 && s.batteryCapacity > 0) {
            s.batteryHealth = s.batteryCapacity / vehicle.batteryCapacity * 100.0;
            if (s.batteryHealth > 120) s.batteryHealth = 120;
        } else {
            s.batteryHealth = 0;
        }
    }

    private static void buildMonths(Stats s, List<EnergyRecord> all, List<EnergyRecord> energy,
                                    Vehicle vehicle, TimeRange range, EnergyRecord baseline) {
        Map<Integer, Stats.MonthPoint> map = new LinkedHashMap<>();
        List<Stats.MonthPoint> points = new ArrayList<>();

        if (range.isAll()) {
            // 历史口径：最近 11 个月 + 本月，共 12 个自然月
            Calendar end = Calendar.getInstance();
            end.setTimeInMillis(System.currentTimeMillis());
            if (s.lastTs > 0 && s.lastTs < System.currentTimeMillis() - 45L * 86400000L) {
                end.setTimeInMillis(s.lastTs);
            }
            end.set(Calendar.DAY_OF_MONTH, 1);
            end.set(Calendar.HOUR_OF_DAY, 0);
            end.set(Calendar.MINUTE, 0);
            end.set(Calendar.SECOND, 0);
            end.set(Calendar.MILLISECOND, 0);
            for (int i = 11; i >= 0; i--) {
                Calendar c = (Calendar) end.clone();
                c.add(Calendar.MONTH, -i);
                addMonthPoint(points, map, c);
            }
        } else {
            // 时间窗口：按窗口首月到末月逐月展开
            Calendar cur = Calendar.getInstance();
            cur.setTimeInMillis(range.startMs);
            cur.set(Calendar.DAY_OF_MONTH, 1);
            cur.set(Calendar.HOUR_OF_DAY, 0);
            cur.set(Calendar.MINUTE, 0);
            cur.set(Calendar.SECOND, 0);
            cur.set(Calendar.MILLISECOND, 0);
            int guard = 0;
            while (cur.getTimeInMillis() < range.endMs && guard < 120) {
                addMonthPoint(points, map, cur);
                cur.add(Calendar.MONTH, 1);
                guard++;
            }
        }

        // 横跨两个年份时给横轴补上年份，避免出现两个“1月”无法区分
        if (points.size() > 1 && points.get(0).year != points.get(points.size() - 1).year) {
            for (Stats.MonthPoint p : points) {
                p.label = String.format(Locale.CHINA, "%02d.%02d", p.year % 100, p.month);
            }
        }

        List<EnergyRecord> byTime = new ArrayList<>(energy);
        Collections.sort(byTime, new Comparator<EnergyRecord>() {
            @Override
            public int compare(EnergyRecord a, EnergyRecord b) {
                return Long.compare(a.timestamp, b.timestamp);
            }
        });

        double prevOdo = baseline != null ? baseline.odometer : -1;
        for (EnergyRecord r : byTime) {
            Stats.MonthPoint p = map.get(monthKey(r.timestamp));
            if (p == null) {
                if (prevOdo < 0) prevOdo = r.odometer;
                continue;
            }
            double d = prevOdo > 0 ? r.odometer - prevOdo : 0;
            if (d > 0 && d < 5000) p.distance += d;
            prevOdo = r.odometer;
            if (r.isCharge()) {
                p.kwh += Math.max(0, r.quantity);
                p.energyCost += Math.max(0, r.amount);
            } else if (r.isFuel()) {
                p.fuel += Math.max(0, r.quantity);
                p.energyCost += Math.max(0, r.amount);
            }
        }

        for (EnergyRecord r : all) {
            if (!r.isExpense()) continue;
            Stats.MonthPoint p = map.get(monthKey(r.timestamp));
            if (p != null) p.otherExpense += Math.max(0, r.amount);
        }

        for (Stats.MonthPoint p : points) {
            if (p.distance > 0.01) {
                if (vehicle.isElectric()) {
                    p.combined = p.kwh / p.distance * 100.0;
                } else if (vehicle.isFuelOnly()) {
                    p.combined = p.fuel / p.distance * 100.0;
                } else {
                    p.combined = (p.fuel + p.kwh * KWH_TO_LITER) / p.distance * 100.0;
                }
            }
            s.months.add(p);
        }
    }

    private static void addMonthPoint(List<Stats.MonthPoint> points,
                                      Map<Integer, Stats.MonthPoint> map, Calendar c) {
        Stats.MonthPoint p = new Stats.MonthPoint();
        p.year = c.get(Calendar.YEAR);
        p.month = c.get(Calendar.MONTH) + 1;
        p.label = monthLabel(p.month);
        points.add(p);
        map.put(p.year * 100 + p.month, p);
    }

    private static void detectAnomalies(Stats s, List<EnergyRecord> all, Vehicle vehicle) {
        List<EnergyRecord> energy = new ArrayList<>();
        for (EnergyRecord r : all) if (r.isEnergy()) energy.add(r);
        if (energy.isEmpty()) return;
        EnergyRecord latest = energy.get(energy.size() - 1);
        if (latest.isCharge()) {
            if (latest.socEnd > 0 && latest.socEnd < 95) {
                s.anomalies.add("最近一次充电未充满（" + fmt0(latest.socEnd)
                        + "%），电耗统计可能略高");
            }
            if (latest.socEnd > 0 && latest.socEnd <= latest.socStart) {
                s.anomalies.add("充电前后电量异常，请检查开始/结束电量");
            }
        } else {
            if (latest.oilAfter > 0 && latest.oilAfter < 90) {
                s.anomalies.add("加油后剩余油量未记录完整（" + fmt0(latest.oilAfter) + "%）");
            }
        }
        if (energy.size() >= 2) {
            EnergyRecord prev = energy.get(energy.size() - 2);
            if (latest.odometer <= prev.odometer) {
                s.anomalies.add("最新里程表读数没有递增，请检查总里程");
            }
            double d = latest.odometer - prev.odometer;
            if (d > 1200) {
                s.anomalies.add("距上次记录已行驶 " + fmt0(d) + " 公里，中间可能漏记");
            }
        }
        if (latest.amount <= 0) {
            s.anomalies.add("最新记录金额为 0，费用统计会偏低");
        }
        if (latest.isCharge() && latest.effectiveUnitPrice() > 2.5) {
            s.anomalies.add("充电单价超过 2.5 元/度，请确认金额与电量");
        }
        if (latest.isFuel() && latest.effectiveUnitPrice() > 12) {
            s.anomalies.add("燃油单价超过 12 元/升，请确认金额与加油量");
        }
        while (s.anomalies.size() > 3) s.anomalies.remove(s.anomalies.size() - 1);
    }

    private static int monthKey(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return c.get(Calendar.YEAR) * 100 + c.get(Calendar.MONTH) + 1;
    }

    /** 本地自然日零点，用于统计“有记录的天数”。 */
    private static long dayKey(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static String monthLabel(int month) {
        return month == 1 ? "1月" : month + "月";
    }

    private static String fmt0(double v) {
        return String.format(Locale.CHINA, "%.0f", v);
    }
}
