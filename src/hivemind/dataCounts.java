
package hivemind;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class dataCounts implements java.io.Serializable, Cloneable {

    private static final long serialVersionUID = 130L;

    statsData stat_info_impact = new statsData();
    statsData stat_info_all =new statsData();

    int[] counts = new int[20];

    int fast_impactSum;
    int fast_allSum;

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
