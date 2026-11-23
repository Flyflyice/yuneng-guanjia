package com.yuneng.guanjia.model;

import java.util.ArrayList;
import java.util.List;

/** 一个统计周期内的汇总结果。 */
public class Stats {
    public double distance;
    public double totalFuel;
    public double totalKwh;
    public double fuelCost;
    public double chargeCost;
    public double otherExpense;
    public double income;
    public double energyCost;
    public double totalCost;

    public double avgFuel;
    public double avgKwh;
    public double combined;
    public double costPerKm;
    public double energyCostPerKm;
    public double totalCostPerKm;
    public double costPerDay;
    public double avgChargePrice;
    public double avgFuelPrice;
    public double latestCombined;
    public double latestFuelEconomy;
    public double latestElectricEconomy;
    public double latestCostPerKm;
    public double latestDistance;

    public double batteryCapacity;
    public double batteryHealth;
    public double thermalEfficiency;
    public double driveEfficiency;
    public double dmKwhPerL;
    public double dmEquivalent;

    /** 纯电里程：相邻两条补能记录之间，油量不变而电量下降的那段。 */
    public double electricDistance;
    /** 纯油里程：电量不变而油量下降的那段。 */
    public double fuelDistance;
    /** 混动里程：电量与油量同时下降的那段。 */
    public double hybridDistance;
    /** 纯电电耗 kWh/100km（只按纯电里程算）。 */
    public double pureElectricEconomy;
    /** 纯油油耗 L/100km（只按纯油里程算）。 */
    public double pureFuelEconomy;

    public long firstTs;
    public long lastTs;
    public int days;
    public int activeDays;
    public int recordCount;
    public int chargeCount;
    public int fuelCount;

    public final List<MonthPoint> months = new ArrayList<>();
    public final List<CapacityPoint> capacities = new ArrayList<>();
    public final List<String> anomalies = new ArrayList<>();

    public static class MonthPoint {
        public int year;
        public int month;
        public String label;
        public double fuel;
        public double kwh;
        public double energyCost;
        public double otherExpense;
        public double distance;
        public double combined;

        public double totalCost() {
            return energyCost + otherExpense;
        }
    }

    public static class CapacityPoint {
        public long ts;
        public double kwh;
    }
}
