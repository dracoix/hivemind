package hivemind;

import java.util.ArrayList;

public class dataPacket implements java.io.Serializable {

    private static final long serialVersionUID = 120L;
    public data_enum type;
    public long timestamp;
    public ArrayList<data_enum> combo = new ArrayList();

    dataPacket(data_enum type) {
        this.type = type;
        timestamp = System.currentTimeMillis();
    }

    dataPacket(ArrayList<data_enum> inputs) {
        this.type = data_enum.SEQUENCE;
        this.combo = inputs;
        timestamp = System.currentTimeMillis();
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
