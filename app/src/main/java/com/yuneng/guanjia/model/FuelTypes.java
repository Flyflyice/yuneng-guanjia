package com.yuneng.guanjia.model;

/**
 * 油品型号与标号。
 * 后续新增能源结构时，只需在对应分类的 grades 中加入标号即可。
 */
public final class FuelTypes {
    public static final String ETHANOL = "乙醇汽油";
    public static final String GASOLINE = "汽油";
    public static final String DIESEL = "柴油";
    public static final String OTHER = "其他";

    public static final String[] TYPES = {ETHANOL, GASOLINE, DIESEL, OTHER};

    private static final String[] ETHANOL_GRADES = {
            "E92#", "E93#", "E95#", "E98#", "E90#", "E97#"
    };
    private static final String[] GASOLINE_GRADES = {
            "89#", "90#", "92#", "93#", "95#", "97#", "98#", "101#"
    };
    private static final String[] DIESEL_GRADES = {
            "0#", "-10#", "-20#", "-35#", "-50#"
    };
    private static final String[] OTHER_GRADES = {
            "天然气 CNG", "天然气 LNG", "液化石油气 LPG",
            "甲醇 M100", "乙醇 E100", "氢能 H2",
            "生物柴油 B5", "生物柴油 B20", "其他"
    };

    private FuelTypes() {
    }

    public static String[] grades(String type) {
        if (ETHANOL.equals(type)) return ETHANOL_GRADES.clone();
        if (DIESEL.equals(type)) return DIESEL_GRADES.clone();
        if (OTHER.equals(type)) return OTHER_GRADES.clone();
        return GASOLINE_GRADES.clone();
    }

    /**
     * 切换分类时尽量保留原标号数字：92# ↔ E92#。
     * 没有对应标号时给一个常用默认值。
     */
    public static String bestGrade(String type, String current) {
        String[] list = grades(type);
        if (current != null) {
            String bare = current.trim();
            if (bare.startsWith("E") || bare.startsWith("e")) bare = bare.substring(1);
            String target = ETHANOL.equals(type) ? "E" + bare : bare;
            for (String g : list) {
                if (g.equalsIgnoreCase(target)) return g;
            }
        }
        if (ETHANOL.equals(type)) return "E92#";
        if (DIESEL.equals(type)) return "0#";
        return GASOLINE.equals(type) ? "92#" : list[0];
    }

    /** 根据标号推断分类，兼容旧版本只保存 fuelGrade 的记录。 */
    public static String typeForGrade(String grade) {
        if (grade == null || grade.trim().isEmpty()) return GASOLINE;
        String g = grade.trim();
        if (g.contains("天然气") || g.contains("液化石油气") || g.contains("甲醇")
                || g.contains("氢") || g.contains("生物柴油") || g.contains("乙醇 E")) return OTHER;
        if (g.startsWith("E") || g.startsWith("e")) return ETHANOL;
        if (g.endsWith("#") && (g.startsWith("-") || g.startsWith("0"))) return DIESEL;
        return GASOLINE;
    }

    public static String findType(String fuelType, String grade) {
        if (fuelType != null) {
            for (String t : TYPES) {
                if (t.equals(fuelType)) return t;
            }
        }
        return typeForGrade(grade);
    }

    public static String display(String fuelType, String grade) {
        String type = findType(fuelType, grade);
        if (grade == null || grade.trim().isEmpty()) return type;
        return type + " · " + grade.trim();
    }
}
