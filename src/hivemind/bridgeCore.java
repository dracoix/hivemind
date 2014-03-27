
package hivemind;

public class bridgeCore {

    // Bridges Main with threadIRC and nTree
    // Any prototype or gem code goes here.
    // All Threads belong here.
    // Anything that threads need belong here.
    
    double xy[][] = new double[15][10];
    double realxy[][] = new double[15][10];
    double cells_filled;
    double depth;
    double rez;
    double prob_moves;
    long time_delay;
    long fuzzy = 1000;
    
    public Thread t_IRC;
    public Thread t_nTree;
    
}
