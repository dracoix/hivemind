
package hivemind;

import static java.lang.System.nanoTime;
import java.util.ArrayList;


public class nTree extends Thread {

    public ArrayList<vertex> nodes = new ArrayList();
    public hiveCore outHive;
    public bridgeCore outBridge;

    public nTree(hiveCore outHive, bridgeCore outBridge) {
        this.outHive = outHive;
        this.outBridge = outBridge;
    }

    long total_sum;

    public void tick(long left, long right, long up, long down, long start) {
        boolean proc = false;
        time_decay = outBridge.time_delay;
        if (left + right + up + down > 0) {
            vertex tmp = new vertex();
            tmp.left = left;
            tmp.right = right;
            tmp.up = up;
            tmp.down = down;
            tmp.start = start;
            tmp.timestamp = System.currentTimeMillis();

            total_sum += left;
            total_sum += right;
            total_sum += up;
            total_sum += down;
            nodes.add(tmp);
            proc = true;
        }

        if (nodes.size() > 0) {
            if (System.currentTimeMillis() - nodes.get(0).timestamp > time_decay) {
                double tl, tr, tu, td;
                tl = nodes.get(0).left;
                tr = nodes.get(0).right;
                tu = nodes.get(0).up;
                td = nodes.get(0).down;
                total_sum -= tl;
                total_sum -= tr;
                total_sum -= tu;
                total_sum -= td;
                nodes.remove(0);
                proc = true;
                //if (nodes.size() > 0) {
                //    nodes.get(0).left += tl;
                //    nodes.get(0).right += tr;
                //    nodes.get(0).up += tu;
                //    nodes.get(0).down += td;
                //    nodes.get(0).left /= 2;
                //    nodes.get(0).right /= 2;
                //    nodes.get(0).up /= 2;
                //    nodes.get(0).down /= 2;

                //}
            }
        }
        if (proc) {
            calc();
        }

    }

    static long old_left_count;
    static long old_right_count;
    static long old_up_count;
    static long old_down_count;
    static long old_start_count;

    long comp_count;

    long sprawl_tick;

    void calc() {
        double ofill = 0;

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 15; x++) {

                if (outBridge.xy[x][y] > (double) 0) {
                    ofill++;
                }
                outBridge.xy[x][y] = 0;//0.9;// 0.90;

            }

        }
        outBridge.cells_filled = (outBridge.cells_filled + ofill) / 2;

        jtick = System.currentTimeMillis();
        comp_count = 0;
        sprawl_tick = nanoTime();
        total_calcs = 0;

//        for (int i = 0; i < nodes.size(); i++) {
//            if (System.currentTimeMillis() - nodes.get(i).timestamp < time_decay -15000) {
//                comp_thresh = i;
//                break;
//            }
//        }
        int i = 0;
        int x = 7;
        int y = 5;
        comp_thresh = nodes.size();
        sprawl(i, x, y, outBridge.xy);

        outBridge.depth = comp_thresh;
        comp_thresh = comp_thresh + 1;
        if (comp_thresh > nodes.size()) {
            comp_thresh = nodes.size();
        }
        if (nanoTime() - sprawl_tick > 1000000000L) {
            comp_thresh -= 2;
        }
        if (comp_thresh < 1) {
            comp_thresh = 1;
        }

        outBridge.realxy = outBridge.xy.clone();

        return;
    }

    long comp_time;
    long comp_thresh = 2;
    long time_decay = 30000L;
    long total_calcs = 0;

    void sprawl(int i, int x, int y, double[][] array) {

        if (i > comp_thresh) {
            return;
        }

        if (i >= nodes.size()) {
            return;
        }

        if (System.currentTimeMillis() - nodes.get(i).timestamp < time_decay - 5000) {
            comp_thresh = i;
            return;
        }
        total_calcs++;
        double tmpTime = (double) (System.currentTimeMillis() - nodes.get(i).timestamp) / (time_decay);
        if (tmpTime > 1) {
            return;
        }
        tmpTime *= tmpTime;
        tmpTime *= tmpTime;

        if (y > 0 && y < 9 && x > 0 && x < 14) {

            array[x + 1][y] += (nodes.get(i).right + tmpTime) * tmpTime;

            array[x - 1][y] += (nodes.get(i).left + tmpTime) * tmpTime;

            array[x][y - 1] += (nodes.get(i).up + tmpTime) * tmpTime;

            array[x][y + 1] += (nodes.get(i).down + tmpTime) * tmpTime;

        }

        if ((nodes.get(i).left) > 0) {
            sprawl(i + 1, x - 1, y, array);
        }

        if ((nodes.get(i).right) > 0) {

            sprawl(i + 1, x + 1, y, array);
        }

        if ((nodes.get(i).up) > 0) {
            sprawl(i + 1, x, y - 1, array);
        }

        if ((nodes.get(i).down) > 0) {

            sprawl(i + 1, x, y + 1, array);
        }
    }
    long ptick = 0;
    long jtick = 0;

    public void run() {
        long tl, tr, tu, td, ts;

        while (true) {
            if (System.currentTimeMillis() - ptick > outBridge.fuzzy) {
                if (System.currentTimeMillis() - ptick > 1000L) {
                    outBridge.fuzzy *= 0.9;
                } else {
                    outBridge.fuzzy *= 1.01;
                }
                ptick = System.currentTimeMillis();
                tl = outHive.current_counts[0];
                tr = outHive.current_counts[1];
                tu = outHive.current_counts[2];

                td = outHive.current_counts[3];
                ts = outHive.current_counts[8];
                tick(tl - old_left_count, tr - old_right_count, tu - old_up_count, td - old_down_count, ts - old_start_count);
                outBridge.rez = (double) ((int) (Math.log((double) total_calcs) * 1000 / Math.log(rezSolver()))) / 10;
                old_left_count = tl;
                old_right_count = tr;
                old_down_count = td;
                old_up_count = tu;
                old_start_count = ts;
            }
        }
    }

    double rezSolver() {
        double sum = 1;
        double tmp;
        int p = (int) comp_thresh;
        if (p >= nodes.size()) {
            p = nodes.size() - 1;
        }
        for (int i = p; i >= 0; i--) {
            tmp = 0;
            if (nodes.get(i).left > 0) {
                tmp++;
            }
            if (nodes.get(i).right > 0) {
                tmp++;
            }
            if (nodes.get(i).up > 0) {
                tmp++;
            }
            if (nodes.get(i).down > 0) {
                tmp++;
            }
            sum += tmp * sum;
        }
        outBridge.prob_moves = sum;
        return sum;
    }

}
