

package hivemind;

public class microdataStats implements java.io.Serializable {
    
    public int total;
    public int sdv;
    public int mean; 
    public int count;
    
    public microdataStats()
    {
        
    }
    
    public String toString()
    {
        return (int) total + "\t" + (float) sdv  /100 + "\t" + (float) mean /100 + "\t" + (float) Math.abs(sdv-mean)/100 + "\t" + count;
    }
}
