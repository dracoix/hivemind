
package hivemind;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class dataCounts implements java.io.Serializable, Cloneable {

    private static final long serialVersionUID = 130L;

    statsData stat_info_impact = new statsData();
    statsData stat_info_all =new statsData();

    int[] counts = new int[20];

    boolean isTroll;

    int fast_impactSum;
    int fast_allSum;
    
    long last_fast_count;

    private int impact_total() {

        last_fast_count = System.currentTimeMillis();
        int res_impactSum = 0;
        for (int i = 0; i < 16; i++) {
            res_impactSum += counts[i];
        }
        fast_impactSum = res_impactSum;
        return res_impactSum;
    }

    private int all_total() {

        last_fast_count = System.currentTimeMillis();
        int res_allSum = 0;
        for (int i = 0; i < 20; i++) {
            res_allSum += counts[i];
        }
        fast_allSum = res_allSum;
        return res_allSum;
    }

    public void runAnalyze() {

        stat_info_impact = new statsData(counts, 0, 15);
        fast_impactSum = (int) stat_info_impact.sum;
        stat_info_all = new statsData(counts, 0, 19);
        fast_allSum = (int) stat_info_all.sum;
    }

    public dataCounts() {
        for (int i = 0; i < counts.length; i++) {
            counts[i] = 0;
        }
    }

    public Object softclone() {
        try {
            return this.clone();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(dataCounts.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
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
