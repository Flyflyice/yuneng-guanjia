public class TestRange {
    public static void main(String[] args) {
        com.yuneng.guanjia.model.Vehicle v = new com.yuneng.guanjia.model.Vehicle();
        v.id = 1;
        v.type = com.yuneng.guanjia.model.Vehicle.TYPE_PHEV;
        java.util.List<com.yuneng.guanjia.model.EnergyRecord> list = com.yuneng.guanjia.data.DemoData.build(v);
        long since = 0;
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(2025, java.util.Calendar.NOVEMBER, 17, 0, 0, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        since = c.getTimeInMillis();
        double fuel = 0, kwh = 0, fuelCost = 0, chargeCost = 0, other = 0, income = 0;
        int n = 0;
        double minOdo = Double.MAX_VALUE, maxOdo = 0;
        for (com.yuneng.guanjia.model.EnergyRecord r : list) {
            if (r.timestamp < since) continue;
            if (r.isEnergy()) { n++; minOdo = Math.min(minOdo, r.odometer); maxOdo = Math.max(maxOdo, r.odometer); }
            if (r.isFuel()) { fuel += r.quantity; fuelCost += r.amount; }
            else if (r.isCharge()) { kwh += r.quantity; chargeCost += r.amount; }
            else if (r.isExpense()) other += r.amount;
            else if (r.isIncome()) income += r.amount;
        }
        System.out.println("since=" + new java.util.Date(since));
        System.out.println("energy=" + n + " dist=" + (maxOdo - minOdo) + " fuel=" + fuel + " kwh=" + kwh);
        System.out.println("fuelCost=" + fuelCost + " chargeCost=" + chargeCost + " other=" + other + " income=" + income);
        System.out.println("total=" + (fuelCost + chargeCost + other));
    }
}
