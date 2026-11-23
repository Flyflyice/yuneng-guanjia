package com.yuneng.guanjia.model;

/** 一次补能记录相对上一条记录之间的行程片段：距离、能耗与花费。 */
public class TripSlice {
    /** 本次补能记录。 */
    public EnergyRecord record;
    /** 上一条有总里程的能耗记录，可能为空。 */
    public EnergyRecord previous;
    /** 两次记录之间的行驶距离（公里），无法计算时为 0。 */
    public double distance;
    /** 两次记录之间的天数，至少 1 天。 */
    public int days = 1;
    /** 本次充入的电量（度）。 */
    public double kwh;
    /** 本次加入的油量（升）。 */
    public double fuel;
    /** 本次补能花费（元）。 */
    public double cost;
    /** 本段平均电耗（度/百公里）。 */
    public double kwhPer100;
    /** 本段平均油耗（升/百公里）。 */
    public double lPer100;
    /** 本段每公里花费（元/公里）。 */
    public double costPerKm;

    /** 是否有可用的里程差，用于计算能耗与每公里花费。 */
    public boolean hasDistance() {
        return distance > 0.5;
    }

    /** 本段日均行驶距离。 */
    public double dailyDistance() {
        return distance / Math.max(1, days);
    }
}
