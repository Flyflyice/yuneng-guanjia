package com.yuneng.guanjia.model;

import java.io.Serializable;

/** 车辆信息。纯电 / 燃油 / 插电混动 / 增程四种动力类型。 */
public class Vehicle implements Serializable {
    public static final String TYPE_UNSET = "";
    public static final String TYPE_EV = "EV";
    public static final String TYPE_ICE = "ICE";
    public static final String TYPE_PHEV = "PHEV";
    public static final String TYPE_EREV = "EREV";

    public long id;
    public String name = "";
    public String type = TYPE_UNSET;
    public double batteryCapacity = 15.87;
    public double tankCapacity = 65;
    public double thermalEfficiency = 0.4606;
    public String fuelGrade = "92#";
    public boolean isDefault;
    public long createdAt;

    public boolean isElectric() {
        return TYPE_EV.equals(type);
    }

    public boolean isFuelOnly() {
        return TYPE_ICE.equals(type);
    }

    /** 既有油箱又有电池的车型（插电混动、增程），会记录剩余油量 / 加油时剩余电量。 */
    public boolean isHybrid() {
        return TYPE_PHEV.equals(type) || TYPE_EREV.equals(type);
    }

    public String typeLabel() {
        if (TYPE_EV.equals(type)) return "纯电";
        if (TYPE_ICE.equals(type)) return "燃油";
        if (TYPE_PHEV.equals(type)) return "插电混动";
        if (TYPE_EREV.equals(type)) return "增程";
        return "";
    }

    public String energyLabel() {
        if (TYPE_EV.equals(type)) return "纯电";
        if (TYPE_ICE.equals(type)) return "燃油";
        if (TYPE_PHEV.equals(type)) return "第五代DM-i";
        if (TYPE_EREV.equals(type)) return "增程式";
        return "";
    }

    public String shortName() {
        if (name == null || name.trim().isEmpty()) return "我的爱车";
        return name.trim();
    }

    public String specLine() {
        if (TYPE_UNSET.equals(type) || type == null) return "";
        if (isElectric()) {
            return "纯电 · " + trim(batteryCapacity) + " 度电池";
        }
        if (isFuelOnly()) {
            return "燃油 · " + trim(tankCapacity) + " L 油箱";
        }
        if (TYPE_EREV.equals(type)) {
            return "增程 · " + trim(batteryCapacity) + " 度电池 · " + trim(tankCapacity) + " L 油箱";
        }
        return "插电混动 · " + trim(batteryCapacity) + " 度电池 · 热效率 "
                + Math.round(thermalEfficiency * 1000) / 10.0 + "%";
    }

    private String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.001) return String.valueOf((long) Math.rint(v));
        return String.valueOf(Math.round(v * 100) / 100.0);
    }
}
