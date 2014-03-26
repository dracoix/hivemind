
package hivemind;

import java.util.ArrayList;

import hivemind.data_enum;
import java.util.Iterator;


public class playerData implements java.io.Serializable {

    private static final long serialVersionUID = 130L;
    public String name;
    public player_enum type = player_enum.CASUAL;

    public ArrayList<dataPacket> inputs = new ArrayList<dataPacket>();

    public dataCounts[] counts_info = new dataCounts[5];
    public microbotData[] bot_info = new microbotData[5];

    public long last_tick;

    public final long _60min = 60 * 60 * 1000;

    public long last_count;

    public String last_msg = new String();

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

        if (System.currentTimeMillis() - last_count < 1000) {
            return;
        }
        last_count = System.currentTimeMillis();

        long sys = System.currentTimeMillis();
        counts_info[0] = new dataCounts();
        counts_info[1] = new dataCounts();
        counts_info[2] = new dataCounts();
        counts_info[3] = new dataCounts();
        counts_info[4] = new dataCounts();
        Iterator<dataPacket> i = inputs.iterator();
        dataPacket d;
        long stamp = 0;
        while (i.hasNext()) {
            d = i.next();
            stamp = sys - d.timestamp;
            for (int n = 0; n < counts_info.length; n++) {
                if (stamp < _60min / (Math.pow(2, n))) {
                    proc_count(d, counts_info[n]);
                    counts_info[n].runAnalyze();
                    fastAnalyzeBot(counts_info[n], (long) (_60min / (double) Math.pow(2, n)), n);
                }
            }
            if (stamp > _60min) {
                i.remove();
            }

        }

        isTroll = fastAnalyzeTroll(counts_info[0])
                & fastAnalyzeTroll(counts_info[4]);

        boolean a=false, b = false, c = false;
        for (int n = 0; n < bot_info.length; n++) {
            a = a | bot_info[n].isMetronome;
            b = b | bot_info[n].isSpammer;

        }

        isMetronome = a;

        isSpammer = b;
        
        isBot = a|b;

    }

    public boolean isTroll;

    public boolean isBot;

    private boolean fastAnalyzeTroll(dataCounts count) {
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

    private boolean fastAnalyzeBot(dataCounts count, long span, int index) {
        if (inputs.size() < 5) {
            return false;
        }

        double impact_inputs = count.stat_info_impact.sum;
        if ((double) impact_inputs < 6) {
            return false;
        }
        double all_inputs = count.stat_info_all.sum;

        double span_seconds = (double) span / 1000;

        double max_input_limit = span_seconds / 4;
        double impact_std_dev = count.stat_info_impact.std_dev_p;

        long last = 0;
        ArrayList<Double> d_times = new ArrayList();
        last = 0;
        dataPacket p = null;
        Iterator<dataPacket> i = inputs.iterator();

        while (i.hasNext()) {
            p = i.next();

            if (System.currentTimeMillis() - p.timestamp < span) {
                d_times.add((double) (p.timestamp - last) / 1000);
                last = p.timestamp;

            }
        }
        Iterator<Double> d = d_times.iterator();
        if (d.hasNext()) {
            double dd = d.next();
            d.remove();
        }

        Double[] array_times = new Double[d_times.size()];
        d_times.toArray(array_times);
        statsData stat_times = new statsData(array_times, 0, d_times.size() - 1);

        bot_info[index].sT = stat_times.std_dev_p;
        bot_info[index].mT = stat_times.mean;
        bot_info[index].tocks = stat_times.kappa_g;

        bot_info[index].isMetronome = (bot_info[index].sT < 0.4 && bot_info[index].tocks > 9);
        bot_info[index].isSpammer = (Math.abs(bot_info[index].tocks / (double) all_inputs) > Math.pow(((double) 1799 / 1800), span_seconds));

        return bot_info[index].isMetronome;

    }

    public boolean isMetronome;

    public boolean isSpammer;

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
