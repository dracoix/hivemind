
package hivemind;

import java.io.BufferedInputStream;
import java.io.InputStream;


public class bridgeCore {

    double xy[][] = new double[15][10];
    double realxy[][] = new double[15][10];
    double cells_filled;
    double depth;
    double rez;
    double prob_moves;
    long time_delay;
    long fuzzy = 1000;
    public Thread tIRC;

    public long last_pong;
    public long last_input;
    public boolean con_fail = false;
    public boolean force_kill = false;
    
    public long incount;
}
