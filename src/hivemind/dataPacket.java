

package hivemind;
  

public class dataPacket implements java.io.Serializable{
    private static final long serialVersionUID = 110L;
    public data_enum type;
    public long timestamp;
    dataPacket(data_enum type)
    {
        this.type = type;
        timestamp = System.currentTimeMillis();
    }
    
    public static long getVer()
    {
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
