import com.yuneng.guanjia.data.DemoData;
import com.yuneng.guanjia.model.EnergyRecord;
import com.yuneng.guanjia.model.StatsCalculator;
import com.yuneng.guanjia.model.TripSlice;
import com.yuneng.guanjia.model.Vehicle;

import java.util.ArrayList;
import java.util.List;

/** 单次行程片段（TripSlice）测试：验证相邻记录之间的里程、能耗与每公里花费。 */
public class TestTrip {
    public static void main(String[] args) {
        Vehicle v = new Vehicle();
        v.id = 1;
        v.type = Vehicle.TYPE_PHEV;
        v.batteryCapacity = 15.87;
        v.thermalEfficiency = 0.4606;

        List<EnergyRecord> all = new ArrayList<>(DemoData.build(v));
        all.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

        // 1. 最早一条能耗记录没有可比的上一条。
        EnergyRecord oldest = null;
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).isEnergy() && all.get(i).odometer > 0) {
                oldest = all.get(i);
                break;
            }
        }
        expect(oldest != null, "演示数据应包含能耗记录");
        TripSlice first = StatsCalculator.tripSlice(all, oldest);
        expect(first.previous == null, "最早一条没有上一条记录");
        expect(!first.hasDistance(), "最早一条无法计算距离");
        expect(first.costPerKm == 0, "无法计算时每公里花费为 0");

        // 2. 最新一条记录应能算出本段距离与每公里花费。
        EnergyRecord newest = all.get(0);
        TripSlice last = StatsCalculator.tripSlice(all, newest);
        expect(last.previous != null, "最新一条应有上一条记录");
        expect(last.hasDistance(), "最新一条应有本段距离");
        expect(last.costPerKm > 0, "最新一条应有每公里花费");
        expect(last.days >= 1, "本段天数至少 1 天");

        // 3. 取一条充电记录，手算校验电耗与每公里花费。
        EnergyRecord sample = null;
        for (int i = 1; i < all.size(); i++) {
            EnergyRecord r = all.get(i);
            if (r.isCharge() && r.odometer > 0 && r.quantity > 0) {
                sample = r;
                break;
            }
        }
        expect(sample != null, "演示数据应包含充电记录");
        TripSlice t = StatsCalculator.tripSlice(all, sample);
        if (t.hasDistance()) {
            double expectKwh = t.kwh / t.distance * 100.0;
            expect(Math.abs(t.kwhPer100 - expectKwh) < 1e-6, "电耗 = 本次电量 ÷ 本段距离 × 100");
            expect(Math.abs(t.costPerKm - t.cost / t.distance) < 1e-6, "每公里花费 = 本次花费 ÷ 本段距离");
            expect(Math.abs(t.distance - (sample.odometer - t.previous.odometer)) < 1e-6,
                    "距离 = 本条里程 − 上一条里程");
        } else {
            expect(t.previous == null, "无距离时应没有上一条记录");
        }

        System.out.println("行程片段测试全部通过");
    }

    private static void expect(boolean ok, String message) {
        if (ok) {
            System.out.println("OK   " + message);
        } else {
            System.out.println("FAIL " + message);
            throw new AssertionError(message);
        }
    }
}
