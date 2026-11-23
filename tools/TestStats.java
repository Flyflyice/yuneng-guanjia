/**
 * JVM 单元测试：验证演示数据与统计引擎的计算口径。
 * 同时验证新增的时间窗口（自然年 / 自然月 / 自定义区间）过滤是否正确。
 */
public class TestStats {
    public static void main(String[] args) {
        com.yuneng.guanjia.model.Vehicle v = new com.yuneng.guanjia.model.Vehicle();
        v.id = 1;
        v.name = "秦L 120KM 15.87度电池";
        v.type = com.yuneng.guanjia.model.Vehicle.TYPE_PHEV;
        v.batteryCapacity = 15.87;
        v.thermalEfficiency = 0.4606;

        java.util.List<com.yuneng.guanjia.model.EnergyRecord> list =
                com.yuneng.guanjia.data.DemoData.build(v);
        com.yuneng.guanjia.model.Stats s =
                com.yuneng.guanjia.model.StatsCalculator.compute(v, list);

        int energy = 0, charge = 0, fuel = 0, income = 0, expense = 0;
        for (com.yuneng.guanjia.model.EnergyRecord r : list) {
            if (r.isCharge()) charge++;
            else if (r.isFuel()) fuel++;
            else if (r.isIncome()) income++;
            else if (r.isExpense()) expense++;
            if (r.isEnergy()) energy++;
        }
        System.out.println("记录总数=" + list.size() + " 能耗=" + energy + " 充电=" + charge
                + " 加油=" + fuel + " 收入=" + income + " 支出=" + expense);
        System.out.println("里程=" + s.distance + " 总油=" + s.totalFuel + " 总电=" + s.totalKwh);
        System.out.println("平均油耗=" + s.avgFuel + " 平均电耗=" + s.avgKwh + " 综合=" + s.combined);
        System.out.println("油费=" + s.fuelCost + " 电费=" + s.chargeCost + " 其他=" + s.otherExpense
                + " 能耗花费=" + s.energyCost + " 总支出=" + s.totalCost + " 收入=" + s.income);
        System.out.println("最近综合=" + s.latestCombined + " 最近油耗=" + s.latestFuelEconomy
                + " 最近电耗=" + s.latestElectricEconomy + " 最近能费=" + s.latestCostPerKm);
        System.out.println("电池估算=" + s.batteryCapacity + " 健康度=" + s.batteryHealth
                + " 热效率=" + s.thermalEfficiency + " 每升发电=" + s.dmKwhPerL + " 等效油耗=" + s.dmEquivalent);
        System.out.println("月度点数=" + s.months.size() + " 容量点=" + s.capacities.size()
                + " 异常=" + s.anomalies.size() + " 天数=" + s.days);

        check("总里程", s.distance, 17664, 1.0);
        check("总油", s.totalFuel, 556.416, 0.5);
        check("总电", s.totalKwh, 2167.4, 2.0);
        check("平均油耗", s.avgFuel, 3.15, 0.03);
        check("平均电耗", s.avgKwh, 12.27, 0.10);
        check("综合能耗", s.combined, 6.95, 0.03);
        check("最近综合", s.latestCombined, 10.19, 0.03);
        // 全量统计口径：旧版“一年”只是视觉标签，现改为真正的时间窗口，
        // 不传 range 时仍保持全量累计口径。
        check("总支出", s.totalCost, 11783.12, 1.0);
        check("油费", s.fuelCost, 4123.60, 1.0);
        check("电费", s.chargeCost, 1107.12, 1.0);
        check("其他", s.otherExpense, 6552.4, 0.5);
        check("能耗花费", s.energyCost, 5230.72, 1.0);
        check("每升油发电", s.dmKwhPerL, 3.7714, 0.01);
        check("等效油耗", s.dmEquivalent, 6.4035, 0.05);
        if (energy != 197 || charge != 160 || fuel != 37 || income != 1 || expense != 9) {
            System.out.println("!! 记录构成不符合预期");
            failures++;
        }

        // ---------------------------------------------------------------
        // 时间窗口：自然年
        // ---------------------------------------------------------------
        com.yuneng.guanjia.model.TimeRange y2025 = com.yuneng.guanjia.model.TimeRange.year(2025);
        com.yuneng.guanjia.model.Stats sy =
                com.yuneng.guanjia.model.StatsCalculator.compute(v, list, y2025);
        boolean monthsOk = sy.months.size() == 12;
        for (com.yuneng.guanjia.model.Stats.MonthPoint m : sy.months) {
            if (m.year != 2025 || m.month < 1 || m.month > 12) monthsOk = false;
        }
        expect(monthsOk, "2025 年生成 12 个自然月");
        expect(Math.abs(sy.totalCost - sumCost(list, y2025)) < 0.01, "2025 年总支出与窗口内记录一致");
        expect(sy.distance >= 0 && sy.distance <= s.distance + 0.01, "2025 年里程不超过全量里程");
        expect(sy.recordCount == countEnergy(list, y2025), "2025 年能耗条数与窗口内一致");
        expect(sy.months.get(0).label.startsWith("1") || sy.months.get(0).month == 1, "2025 年首月为 1 月");

        // ---------------------------------------------------------------
        // 时间窗口：自然月
        // ---------------------------------------------------------------
        com.yuneng.guanjia.model.TimeRange may = com.yuneng.guanjia.model.TimeRange.month(2025, 5);
        com.yuneng.guanjia.model.Stats sm =
                com.yuneng.guanjia.model.StatsCalculator.compute(v, list, may);
        expect(sm.months.size() == 1 && sm.months.get(0).month == 5, "2025 年 5 月生成 1 个月份点");
        expect(Math.abs(sm.totalCost - sumCost(list, may)) < 0.01, "2025 年 5 月总支出与窗口内记录一致");
        expect(sm.recordCount == countEnergy(list, may), "2025 年 5 月能耗条数与窗口内一致");

        // ---------------------------------------------------------------
        // 时间窗口：自定义 2025-03-01 ~ 2025-05-31
        // ---------------------------------------------------------------
        com.yuneng.guanjia.model.TimeRange custom = com.yuneng.guanjia.model.TimeRange.custom(
                ts(2025, 3, 1), ts(2025, 5, 31));
        com.yuneng.guanjia.model.Stats sc =
                com.yuneng.guanjia.model.StatsCalculator.compute(v, list, custom);
        expect(sc.months.size() == 3, "自定义 3-5 月生成 3 个月份点");
        expect(Math.abs(sc.totalCost - sumCost(list, custom)) < 0.01, "自定义区间总支出与窗口内记录一致");
        expect(sc.recordCount == countEnergy(list, custom), "自定义区间能耗条数与窗口内一致");
        expect(custom.spanDays() == 92, "自定义区间天数按自然日计算");

        // ---------------------------------------------------------------
        // 时间窗口：近一年（默认范围）
        // ---------------------------------------------------------------
        com.yuneng.guanjia.model.TimeRange lastYear = com.yuneng.guanjia.model.TimeRange.lastYear();
        com.yuneng.guanjia.model.Stats sl =
                com.yuneng.guanjia.model.StatsCalculator.compute(v, list, lastYear);
        expect(sl.months.size() == 12, "近一年生成 12 个自然月");
        if (sl.months.get(0).year != sl.months.get(11).year) {
            expect(sl.months.get(0).label.contains("."), "跨年时横轴标签带年份");
        }
        expect(sl.totalCost > 0, "近一年总支出大于 0");

        System.out.println("测试" + (failures == 0 ? "全部通过" : "失败 " + failures + " 项"));
        if (failures > 0) System.exit(1);
    }

    static int failures = 0;

    static void check(String name, double actual, double expect, double tol) {
        boolean ok = Math.abs(actual - expect) <= tol;
        if (!ok) failures++;
        System.out.println((ok ? "OK  " : "FAIL") + " " + name + " 实际=" + actual + " 期望=" + expect);
    }

    static void expect(boolean ok, String name) {
        if (!ok) failures++;
        System.out.println((ok ? "OK  " : "FAIL") + " " + name);
    }

    /** 窗口内（充电 + 加油 + 支出）金额之和，与 Stats.totalCost 口径一致。 */
    static double sumCost(java.util.List<com.yuneng.guanjia.model.EnergyRecord> list,
                          com.yuneng.guanjia.model.TimeRange range) {
        double sum = 0;
        for (com.yuneng.guanjia.model.EnergyRecord r : list) {
            if (!range.contains(r.timestamp)) continue;
            if (r.isCharge() || r.isFuel() || r.isExpense()) sum += Math.max(0, r.amount);
        }
        return sum;
    }

    static int countEnergy(java.util.List<com.yuneng.guanjia.model.EnergyRecord> list,
                           com.yuneng.guanjia.model.TimeRange range) {
        int n = 0;
        for (com.yuneng.guanjia.model.EnergyRecord r : list) {
            if (range.contains(r.timestamp) && r.isEnergy() && r.odometer > 0) n++;
        }
        return n;
    }

    static long ts(int y, int m, int d) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.clear();
        c.set(y, m - 1, d, 0, 0, 0);
        return c.getTimeInMillis();
    }
}
