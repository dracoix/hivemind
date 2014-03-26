
package hivemind;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


public class statDump {

    public ArrayList<microdataStats> data = new ArrayList();

    public statDump() {

    }

    boolean failLoad = true;

    public void add(microdataStats d) {

        for (microdataStats s : data) {
            if (d.mean == s.mean) {
                if (d.total == s.total) {
                    if (d.sdv == s.sdv) {
                        s.count++;
                        return;
                    }
                }
            }
        }

        d.count++;
        if (d.mean > 0.0) {
            data.add(d);
        }

    }

    public void save() {
        if (failLoad) {
            load();
            return;
        }
        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream("stat_v3.dat");
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(data);
            oos.close();
        } catch (IOException ex) {
            Logger.getLogger(Hivemind.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fout.close();
            } catch (IOException ex) {
                Logger.getLogger(Hivemind.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        dump();
    }

    public void load() {
        FileInputStream fin = null;
        failLoad = true;
        try {
            File f = new File("stat_v3.dat");
            if (f.exists()) {
                fin = new FileInputStream(f);
                ObjectInputStream ois = new ObjectInputStream(fin);
                data = (ArrayList<microdataStats>) ois.readObject();
                ois.close();
                
            }
            failLoad = false;
        } catch (Exception ex) {
            Logger.getLogger(Hivemind.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Logger.getLogger(Hivemind.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void dump() {
        FileWriter fr = null;
        try {
            Comparator c = new Comparator<microdataStats>() {

                @Override
                public int compare(microdataStats a, microdataStats b) {

                    return ((Integer) ((int) b.total)).compareTo((Integer) ((int) a.total));
                }
            };
            try {
                Collections.sort(data, c);
            } catch (Exception e) {

            }
            String outWrite = "";
            String n = "";
            File f = new java.io.File("mass_stat_dump.txt");
            fr = new FileWriter(f);
            BufferedWriter writer = new BufferedWriter(fr);
            writer.write(""
                    + "IN\tsImp\tsMean\tsDif\tcount\n");
            for (int i = 0; i < data.size(); i++) {
                writer.write(data.get(i).toString() + "\n");
            }
            writer.close();
        } catch (IOException ex) {
            Logger.getLogger(statDump.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fr.close();
            } catch (IOException ex) {
                Logger.getLogger(statDump.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
