package com.yuneng.guanjia.model;

import java.io.Serializable;

/** 一条补能、收入或支出记录。 */
public class EnergyRecord implements Serializable {
    public static final String TYPE_CHARGE = "CHARGE";
    public static final String TYPE_FUEL = "FUEL";
    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_EXPENSE = "EXPENSE";

    public long id;
    public long vehicleId;
    public String type = TYPE_CHARGE;
    public long timestamp;
    public double odometer;

    /** 实付金额（充电/加油/收入/支出均使用该字段）。 */
    public double amount;
    /** 充电电量 kWh，或加油量 L。 */
    public double quantity;
    /** 折合电价 元/度，或机显单价 元/升。 */
    public double unitPrice;
    /** 加油机显金额。 */
    public double listAmount;
    /** 加油优惠金额。 */
    public double discount;

    public double socStart;
    public double socEnd;
    public double oilBefore;
    public double oilAfter;
    /** 充电时记录的剩余油量。 */
    public double remainingOil;
    /** 加油时记录的剩余电量。 */
    public double remainingSoc;

    public String station = "";
    /** 油品型号：乙醇汽油 / 汽油 / 柴油 / 其他。 */
    public String fuelType = "";
    /** 具体标号，例如 E92#、95#、0#、天然气 CNG。 */
    public String fuelGrade = "";
    public String category = "";
    public String note = "";
    public String photoPath = "";
    public boolean previousRecorded = true;
    public long createdAt;

    public boolean isCharge() {
        return TYPE_CHARGE.equals(type);
    }

    public boolean isFuel() {
        return TYPE_FUEL.equals(type);
    }

    public boolean isEnergy() {
        return isCharge() || isFuel();
    }

    public boolean isIncome() {
        return TYPE_INCOME.equals(type);
    }

    public boolean isExpense() {
        return TYPE_EXPENSE.equals(type);
    }

    public String typeLabel() {
        if (isCharge()) return "充电";
        if (isFuel()) return "加油";
        if (isIncome()) return "收入";
        return "支出";
    }

    public String quantityUnit() {
        if (isCharge()) return "度";
        if (isFuel()) return "升";
        return "元";
    }

    /** 加油记录的“油品型号 · 标号”，充电/收支返回空串。 */
    public String fuelLabel() {
        return isFuel() ? FuelTypes.display(fuelType, fuelGrade) : "";
    }

    public String amountUnit() {
        return "元";
    }

    public double effectiveUnitPrice() {
        if (quantity > 0.0001) return amount / quantity;
        return unitPrice;
    }

    public double batteryCapacityEstimate() {
        double delta = socEnd - socStart;
        if (!isCharge() || delta < 3 || quantity <= 0) return 0;
        return quantity / delta * 100.0;
    }

    public EnergyRecord copy() {
        EnergyRecord r = new EnergyRecord();
        r.id = id;
        r.vehicleId = vehicleId;
        r.type = type;
        r.timestamp = timestamp;
        r.odometer = odometer;
        r.amount = amount;
        r.quantity = quantity;
        r.unitPrice = unitPrice;
        r.listAmount = listAmount;
        r.discount = discount;
        r.socStart = socStart;
        r.socEnd = socEnd;
        r.oilBefore = oilBefore;
        r.oilAfter = oilAfter;
        r.remainingOil = remainingOil;
        r.remainingSoc = remainingSoc;
        r.station = station;
        r.fuelType = fuelType;
        r.fuelGrade = fuelGrade;
        r.category = category;
        r.note = note;
        r.photoPath = photoPath;
        r.previousRecorded = previousRecorded;
        r.createdAt = createdAt;
        return r;
    }
}
