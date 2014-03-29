package hivemind;

import java.util.ArrayList;
import java.util.Iterator;

public class playerData implements java.io.Serializable {

    private static final long serialVersionUID = 130L;
    public String name;
    //public player_enum type = player_enum.CASUAL;

    public ArrayList<dataPacket> inputs = new ArrayList();

    // Divide and Analyze format
    // Synchonized to seconds, not index. 
    //
    // 60 |................|
    // 30         |........|
    // 15             |....|
    // ~8               |..|
    // ~4                |.|
    public dataCounts[] counts_info = new dataCounts[5];
    public microbotData[] bot_info = new microbotData[5];
    public ArrayList<Double> times = new ArrayList();
    public statsData[] time_stats = new statsData[5];

    public long last_tick;

    static final long _60min = 60 * 60 * 1000;

    public long last_count;

    public String last_msg = new String();

    public boolean isTroll;
    public boolean isBot;
    public boolean isMetronome;
    public boolean isSpammer;

    public playerData(String name) {

        this.name = name;
        for (int i = 0; i < counts_info.length; i++) {
            counts_info[i] = new dataCounts();
            bot_info[i] = new microbotData();
        }
    }

    public void tick(dataPacket in) {
        last_tick = System.currentTimeMillis();
        inputs.add(in);
        count_all();
    }

    private void count_all() {

        // Used as optimization, but lossy. 
        if (System.currentTimeMillis() - last_count < 1000) {
            return;
        }
        last_count = System.currentTimeMillis();

        // Unwrapped loop
        counts_info[0] = new dataCounts();
        counts_info[1] = new dataCounts();
        counts_info[2] = new dataCounts();
        counts_info[3] = new dataCounts();
        counts_info[4] = new dataCounts();

        Iterator<dataPacket> i = inputs.iterator();
        dataPacket d;
        long stamp;
        times.clear();

        int _30m = 0, _15m = 0, _8m = 0, _4m = 0;

        while (i.hasNext()) {
            d = i.next();
            stamp = last_count - d.timestamp;
            if (stamp > _60min) {                           // Pre-Process Check
                i.remove();
                continue;
            }
            times.add((double) d.timestamp / 1000);
            // Unwrapped optimized
            // Create sub-sets to analyze
            proc_count(d, counts_info[0]); // Lossy pitfall asuming under 60min
            if (stamp < (_60min >> 1)) {
                proc_count(d, counts_info[1]);
                if (stamp < (_60min >> 2)) {
                    proc_count(d, counts_info[2]);
                    if (stamp < (_60min >> 3)) {
                        proc_count(d, counts_info[3]);
                        if (stamp < (_60min >> 4)) {
                            proc_count(d, counts_info[4]);
                        } else {
                            _4m++;
                        }
                    } else {
                        _8m++;
                    }
                } else {
                    _15m++;
                }
            } else {
                _30m++;
            }

        }

        time_stats[0] = new statsData(times, 0, times.size() - 1);
        time_stats[1] = new statsData(times, _30m, times.size() - 1);
        time_stats[2] = new statsData(times, _15m, times.size() - 1);
        time_stats[3] = new statsData(times, _8m, times.size() - 1);
        time_stats[4] = new statsData(times, _4m, times.size() - 1);

        // Removed from the while loop
        for (int n = 0; n < counts_info.length; n++) {      // Analyze sub-sets
            counts_info[n].runAnalyze();
            fastAnalyzeBot(counts_info[n], time_stats[n], n);
        }

        isTroll = fastAnalyzeTroll(counts_info[0])
                & fastAnalyzeTroll(counts_info[4]);

        isMetronome = bot_info[0].isMetronome
                | bot_info[1].isMetronome
                | bot_info[2].isMetronome
                | bot_info[3].isMetronome
                | bot_info[4].isMetronome;

        isSpammer = bot_info[0].isSpammer
                | bot_info[1].isSpammer
                | bot_info[2].isSpammer
                | bot_info[3].isSpammer
                | bot_info[4].isSpammer;

        isBot = isMetronome | isSpammer;

    }

    private boolean fastAnalyzeTroll(dataCounts count) {
        // TO-DO: None, port went okay.
        if (count.fast_impactSum < 9) {
            return false;
        }
        boolean tmp = false;
        double k;
        //1
        k = Math.abs(count.stat_info_impact.std_dev_p) / (Math.sqrt(15) * count.fast_impactSum / 16);
        tmp = tmp | (k > 0.98 && k < 1.02);

        //2
        k = Math.abs(count.stat_info_impact.std_dev_p) / (((count.fast_impactSum * Math.sqrt(7)) / 16));
        tmp = tmp | (k > 0.98 && k < 1.02);
        return tmp;
    }

    private boolean fastAnalyzeBot(dataCounts count, statsData stats, int index) {
        // SLOW PORT FROM MONOLITHIC-CLASS
        // TO-DO:
        // 1) Fix array initialization, extremely slow method used

        if (inputs.size() < 5) {
            return false;
        }

        //double impact_inputs = count.stat_info_impact.sum;
        if (count.stat_info_impact.sum < 6) {
            return false;
        }
//        ArrayList<Double> tmp_delta_times = new ArrayList();
//        //double all_inputs = count.stat_info_all.sum;
//
//        long last = 0;
//        tmp_delta_times.clear();
//        dataPacket p;
//        Iterator<dataPacket> i = inputs.iterator();
//
//        while (i.hasNext()) {
//            p = i.next();
//
//            if (System.currentTimeMillis() - p.timestamp < span) {
//                tmp_delta_times.add((double) (p.timestamp - last) / 1000);
//                last = p.timestamp;
//
//            }
//        }
//        Iterator<Double> d = tmp_delta_times.iterator();
//        tmp_delta_times.remove(0);
//
//        statsData stat_times = new statsData((Double[]) tmp_delta_times.toArray(), 0, tmp_delta_times.size() - 1);

        bot_info[index].sT = stats.deltaN_std_p;
        bot_info[index].mT = stats.deltaN_mean;
        bot_info[index].tocks = stats.kappaN_g;

        bot_info[index].isMetronome = (bot_info[index].sT < 0.4 && bot_info[index].tocks > 9);
        bot_info[index].isSpammer = (Math.abs(bot_info[index].tocks / (double) count.stat_info_all.sum) > Math.pow(((double) 1799 / 1800), count.stat_info_all.sum * 4));

        return bot_info[index].isMetronome;

    }

    public void addPacket(data_enum type, String msg) {
        inputs.add(new dataPacket(type));
        last_msg = msg;
        last_tick = System.currentTimeMillis();
        count_all();
    }

    private void proc_count(dataPacket in, dataCounts count) {

        count.counts[in.type.getCode()]++;

    }

    public static long getVer() {
        return serialVersionUID;
    }

    public static String getDev() {
        switch ((int) (serialVersionUID % 5L)) {
            case 0:
                return serialVersionUID + " DEV";
            case 1:
                return serialVersionUID + " FIX";
            case 2:
                return serialVersionUID + " FINE";
            case 3:
                return serialVersionUID + " GOOD";
            case 4:
                return serialVersionUID + " BEST";

        }
        return serialVersionUID + " DEV";
    }
}
