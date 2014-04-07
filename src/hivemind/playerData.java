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
    public dataCounts[] counts_info = new dataCounts[7];
    public microbotData[] bot_info = new microbotData[7];
    public ArrayList<Double> times = new ArrayList();
    public statsData[] time_stats = new statsData[7];

    double[] player_weights = new double[20];

    public long last_tick;

    static final long _60min = 60 * 60 * 1000;

    public long last_count;

    public String last_msg = new String();

    public boolean isTroll;
    public boolean isBot;
    public boolean isMetronome;
    public boolean isSpammer;
    public boolean isPermaTroll;

    public playerData(String name) {

        this.name = name;
        for (int i = 0; i < counts_info.length; i++) {
            counts_info[i] = new dataCounts();
            bot_info[i] = new microbotData();
        }
    }

//    public void tick(dataPacket in, double[] player_weights) {
//        this.player_weights = player_weights.clone();
//        last_tick = System.currentTimeMillis();
//        inputs.add(in);
//        count_all();
//    }
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
        counts_info[5] = new dataCounts();
        counts_info[6] = new dataCounts();

        Iterator<dataPacket> i = inputs.iterator();
        dataPacket d;
        long stamp;
        times.clear();

        int _30m = 0, _15m = 0, _8m = 0, _4m = 0, _2m = 0, _1m = 0;

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
                            if (stamp < (_60min >> 5)) {
                                proc_count(d, counts_info[5]);
                                if (stamp < (_60min >> 6)) {
                                    proc_count(d, counts_info[6], player_weights);
                                } else {
                                    _1m++;
                                }
                            } else {
                                _2m++;
                            }
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

        // I goofed up and reversed the analyzation windows, here's the correction.
        _15m += _30m;
        _8m += _15m;
        _4m += _8m;
        _2m += _4m;
        _1m += _2m;

        time_stats[0] = new statsData(times, 0, times.size() - 1);
        time_stats[1] = new statsData(times, _30m, times.size() - 1);
        time_stats[2] = new statsData(times, _15m, times.size() - 1);
        time_stats[3] = new statsData(times, _8m, times.size() - 1);
        time_stats[4] = new statsData(times, _4m, times.size() - 1);
        time_stats[5] = new statsData(times, _2m, times.size() - 1);
        time_stats[6] = new statsData(times, _1m, times.size() - 1);

        // Removed from the while loop
        for (int n = 0; n < counts_info.length; n++) {      // Analyze sub-sets
            counts_info[n].runAnalyze();
            fastAnalyzeBot(counts_info[n], time_stats[n], n);
        }

        isTroll = fastAnalyzeTroll(counts_info[6]);
        //| fastAnalyzeTroll(counts_info[4]);

        isMetronome = bot_info[0].isMetronome
                | bot_info[1].isMetronome
                | bot_info[2].isMetronome
                | bot_info[3].isMetronome
                | bot_info[4].isMetronome
                | bot_info[5].isMetronome;

        isSpammer = bot_info[0].isSpammer
                | bot_info[1].isSpammer
                | bot_info[2].isSpammer
                | bot_info[3].isSpammer
                | bot_info[4].isSpammer
                | bot_info[5].isSpammer;

        isBot = isMetronome | isSpammer;

    }

    private boolean fastAnalyzeTroll(dataCounts count) {
        // TO-DO: None, port went okay.
        if (isPermaTroll) {
            return true;
        }
        if (count.fast_impactSum < 1) {
            isPermaTroll = false;
            return false;
        }
        boolean tmp = false;
        double k;

        //1
        k = Math.abs(count.stat_info_impact.std_dev_p) / (Math.sqrt(14) * count.fast_impactSum / 15);
        tmp = tmp | (k > 0.98 && k < 1.02);

        //2
        k = Math.abs(count.stat_info_impact.std_dev_p) / (((count.fast_impactSum * Math.sqrt(26)) / 30));
        tmp = tmp | (k > 0.98 && k < 1.02);
        return tmp;
    }

    private boolean fastAnalyzeBot(dataCounts count, statsData stats, int index) {

        if (inputs.size() < 3) {
            return false;
        }

        if (count.stat_info_impact.sum < 3) {
            return false;
        }

        bot_info[index].sT = stats.deltaN_std_p;
        bot_info[index].mT = stats.deltaN_mean;
        bot_info[index].tocks = stats.kappaN_g;

        bot_info[index].isMetronome = (bot_info[index].sT < 0.42 && bot_info[index].tocks > 3);
        bot_info[index].isSpammer = (Math.abs(bot_info[index].tocks / (double) count.stat_info_all.sum) > Math.pow((((double) count.stat_info_all.sum - 1) / count.stat_info_all.sum), count.stat_info_all.sum / 4));

        return bot_info[index].isMetronome;

    }

    public void addPacket(data_enum type, String msg, double[] player_weights) {
        this.player_weights = player_weights.clone();
        inputs.add(new dataPacket(type));
        last_msg = msg;
        last_tick = System.currentTimeMillis();
        count_all();
    }

    public void addPacket(data_enum type, String msg, ArrayList<data_enum> builder, double[] player_weights) {

        this.player_weights = player_weights.clone();
        if (type == data_enum.SEQUENCE) {
            inputs.add(new dataPacket(builder));
        } else {
            inputs.add(new dataPacket(type));
        }
        last_msg = msg;
        last_tick = System.currentTimeMillis();
        count_all();
    }

    private void proc_count(dataPacket in, dataCounts count, double[] player_weights) {

        if (in.type == data_enum.SEQUENCE) {

            for (int i = 0; i < in.combo.size(); i++) {
                if (in.combo.get(i) == data_enum.SELECT) {
                    if (isSpammer) {
                        isPermaTroll = true;
                    }
                    count.counts[in.combo.get(i).getCode()] += 200;
                } else {
                    count.counts[in.combo.get(i).getCode()] += ((double) 1 / (double) in.combo.size()) * player_weights[in.combo.get(i).getCode()] * 3;
                }
            }

            return;
        }
        if (in.type == data_enum.SELECT) {
            if (isSpammer) {
                isPermaTroll = true;
            }
            count.counts[in.type.getCode()] += 200;
            return;
        }
        count.counts[in.type.getCode()] += player_weights[in.type.getCode()] * 3;

    }

    private void proc_count(dataPacket in, dataCounts count) {

        if (in.type == data_enum.SEQUENCE) {

            for (int i = 0; i < in.combo.size(); i++) {
                count.counts[in.combo.get(i).getCode()] += (double) 1 / (double) in.combo.size();
            }

            return;
        }
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
