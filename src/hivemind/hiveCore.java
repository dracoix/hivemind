
package hivemind;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.System.nanoTime;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import hivemind.data_enum.*;

public class hiveCore {

    static final int dump_limit = 10;

    boolean failLoad = true;
    public Hashtable<String, playerData> players = new Hashtable();
    public Hashtable<String, playerData> clone_players = new Hashtable();
    public ArrayList<String> official = new ArrayList();

    public statDump massDump = new statDump();

    data_enum chat_type;
    String chat_snippet;

    long[] current_counts = new long[20];
    long[] delta_counts = new long[20];
    long[] old_counts = new long[20];

    long[] current_bot_counts = new long[20];
    long[] delta_bot_counts = new long[20];
    long[] old_bot_counts = new long[20];

    ArrayList<feedPacket> tempFeed = new ArrayList();
    long otick, ntick;

    double no_fade;
    double yes_fade;
    double old_no_fade;
    double old_yes_fade;

    public void tick() {
        long dtick;
        ntick = nanoTime();
        dtick = ntick - otick;

        if (dtick < 333333333) {
            return;
        }

        for (int i = 0; i < 20; i++) {
            delta_bot_counts[i] = current_bot_counts[i] - old_bot_counts[i];
            delta_counts[i] = current_counts[i] - old_counts[i];
            old_bot_counts[i] = current_bot_counts[i];
            old_counts[i] = current_counts[i];
        }

        no_fade += delta_counts[19] + 1;
        no_fade /= 2;

        yes_fade += delta_counts[18] + 1;
        yes_fade /= 2;

        if (no_fade < 1) {
            no_fade = 1;
        }
        if (yes_fade < 1) {
            yes_fade = 1;
        }

        otick = nanoTime();
    }

    public void clean() {
        long active = 20 * 60 * 1000;
        Iterator<playerData> i = players.values().iterator();
        playerData p;
        while (i.hasNext()) {
            p = i.next();
            if (System.currentTimeMillis() - p.last_tick > active) {
                i.remove();
            }
        }

    }

    long last_core_save;

    public void save() {

        if (failLoad) {
            load();
            return;
        }
        clean();
        if (System.currentTimeMillis() - last_core_save > 30000) {
            last_core_save = System.currentTimeMillis();
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream("core_v4.dat");
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeObject(players);
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

            fout = null;
            try {
                fout = new FileOutputStream("official.dat");
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeObject(official);
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
            massDump.save();
        }
        dump();

    }

    public void load() {
        FileInputStream fin = null;
        failLoad = true;
        try {
            File f = new File("core_v4.dat");
            if (f.exists()) {
                fin = new FileInputStream(f);
                ObjectInputStream ois = new ObjectInputStream(fin);
                players = (Hashtable<String, playerData>) ois.readObject();
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

        failLoad = true;
        fin = null;
        try {
            File f = new File("official.dat");
            if (f.exists()) {
                fin = new FileInputStream(f);
                ObjectInputStream ois = new ObjectInputStream(fin);
                official = (ArrayList<String>) ois.readObject();
                ois.close();
                failLoad = false;
            }
        } catch (Exception ex) {
            Logger.getLogger(Hivemind.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fin.close();
            } catch (IOException ex) {
                Logger.getLogger(Hivemind.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        massDump.load();
    }

    public void dump() {
        ArrayList<playerData> full_dumps = new ArrayList();

        Iterator<playerData> i = players.values().iterator();
        //playerData p;
        long min20 = 20 * 60 * 1000;
        microdataStats stattmp = new microdataStats();
        clone_players = (Hashtable<String, playerData>) players.clone();
        try {
            statsData tmpStats;
            for (playerData p : clone_players.values()) {
                try {
                    tmpStats = new statsData(p.counts_info[0].counts, 0, 16);
                    //p.count_all();
                    if ((System.currentTimeMillis() - p.last_tick) < 12000) {
                        stattmp = new microdataStats();
                        stattmp.mean = (int) Math.round(tmpStats.mean * 100);
                        stattmp.sdv = (int) Math.round(tmpStats.std_dev_p * 100);
                        stattmp.total = (int) p.counts_info[0].fast_impactSum;
                        massDump.add(stattmp);
                    }
                    if ((System.currentTimeMillis() - p.last_tick) < min20) {
                        if ((p.counts_info[0].fast_impactSum > dump_limit)) {
                            full_dumps.add(p);
                        }
                    }
                } catch (Exception ex) {

                }
            }
        } catch (Exception ex) {
            Logger.getLogger(hiveCore.class.getName()).log(Level.SEVERE, null, ex);
        }

        Comparator c = new Comparator<playerData>() {

            @Override
            public int compare(playerData a, playerData b) {

                return ((Integer) (b.counts_info[0].fast_impactSum)).compareTo((Integer) a.counts_info[0].fast_impactSum);
            }
        };

        Collections.sort(full_dumps, c);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss.SS");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String st = (fmt.format(timestamp));
        playerData fluff = new playerData(System.currentTimeMillis() + "");
        try {
            String outWrite = "";
            File f = new java.io.File("past_hour.txt");
            FileWriter fr = new FileWriter(f);
            BufferedWriter writer = new BufferedWriter(fr);
            dataCounts count;
            writer.write("\nWATCH THE HIVEMIND HERE: http://www.twitch.tv/angrydraco \n\n"
                    + "-- FULL LIST AUTOMATICALLY GENERATED FROM HIVEMIND -- \n"
                    + "Names listed are those ACTIVE for the past 20 minutes as of (NOW) - UTC: " + st + "\n"
                    + "\nCORE VERSIONING - playerData: b" + playerData.getDev() + " | dataPacket: b" + dataPacket.getDev()
                    + " | dataCounts: b" + dataCounts.getDev()
                    + " | Analysis is now implimented as playerData.fastAnalyze<Troll|Bot>() | " + "\n\n"
                    + "-- INPUT COUNTS ARE FROM THE PAST HOUR -- \n\n"
                    + "OF THE " + (int) players.size() + " USERS... " + (int) full_dumps.size() + " ARE ACTIVE AND HAVE AT LEAST " + dump_limit + " INPUT\n\n"
                    + "IN\tLEFT\tRIGHT\tUP\tDOWN\tA\tB\tX\tY\tSt\tSel\tL\tR\tWait\tDemo\tAnarch\tSeq\tRiot\tChat\tYes\tNo\tσ\t(σ-μ)\tμT\tσT\tTocks60\tTocks15\tUSERNAME\n");
            String buildit = "";
            statsData tmpStats;
            for (playerData e : full_dumps) {
                count = e.counts_info[0];
                buildit = count.fast_impactSum + "";

                for (int n = 0; n < 20; n++) {
                    buildit += "\t" + count.counts[n];
                }
                tmpStats = new statsData(e.counts_info[0].counts, 0, 16);
                buildit
                        += "\t" + (float) Math.round(tmpStats.std_dev_p * 10) / 10
                        + "\t" + (float) Math.round(Math.abs((float) Math.round(tmpStats.std_dev_p * 10) / 10 - (float) Math.round(tmpStats.mean * 10) / 10) * 10) / 10
                        + "\t" + (float) Math.round(e.bot_info[0].mT * 10) / 10
                        + "\t" + (float) Math.round(e.bot_info[0].sT * 10) / 10
                        + "\t" + (int) Math.round(e.bot_info[0].tocks * 10) / 10
                        + "\t" + (int) Math.round(e.bot_info[2].tocks * 10) / 10;
                writer.write(buildit
                        + "\t" + e.name + "\n");

            }
            writer.close();
        } catch (Exception e) {

        }

        try {
            String outWrite = "";
            File f = new java.io.File("tpp_track.txt");
            FileWriter fr = new FileWriter(f);
            BufferedWriter writer = new BufferedWriter(fr);
            dataCounts count;

            for (String e : official) {

                writer.write(e + "\n");
            }
            writer.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

    EnumSet impactFeed = EnumSet.of(data_enum.A, data_enum.B, data_enum.X, data_enum.Y,
            data_enum.LEFT, data_enum.RIGHT, data_enum.UP, data_enum.DOWN,
            data_enum.START, data_enum.LB, data_enum.RB, data_enum.ANARCHY,
            data_enum.SEQUENCE, data_enum.WAIT);

    public void proc(String full) {

        full = full.trim();
        if (!full.contains("!")) {
            return;
        }
        if (!full.contains(":")) {
            return;
        }
        String name = full.substring(1, full.indexOf("!"));

        String msg = full.substring(full.lastIndexOf(":") + 1);

        if (name.trim().toLowerCase().equals("twitchplayspokemon")) {
            official.add(System.currentTimeMillis() + "> " + name + ": " + msg.trim());
        }

        playerData usr;
        feedPacket fp = new feedPacket();
        fp.name = name;
        fp.type = player_enum.CASUAL;
        if (!players.containsKey(name)) {
            usr = new playerData(name);

        } else {
            usr = players.get(name);
        }
        if (usr.counts_info[0].fast_impactSum > 90) {
            fp.type = player_enum.HARDCORE;
        }
        if (usr.isTroll) {
            fp.type = player_enum.TROLL;
        }
        if (usr.isMetronome) {
            fp.type = player_enum.METRONOME;

        }

        if (usr.isSpammer) {
            fp.type = player_enum.SPAMMER;
        }

        usr.last_msg = "";

        if (isValidMessage(msg, usr)) {

            players.put(name, usr);

            if (impactFeed.contains(usr.inputs.get(usr.inputs.size() - 1).type)) {
                fp.cmd_type = usr.inputs.get(usr.inputs.size() - 1).type;
                fp.command = usr.last_msg.trim().toLowerCase();
                if (!fp.command.isEmpty()) {
                    fp.inputs = usr.counts_info[4].fast_impactSum;

                    fp.sMet = usr.bot_info[4].sT;
                    tempFeed.add(fp);
                }
                if (tempFeed.size() > 15) {
                    tempFeed.remove(0);
                }
            }
        }

    }

    public void updateFeed() {

    }

    public boolean isTroll(playerData p) {
        return (p.isTroll | p.isBot);
    }

    public boolean isValidMessage(String msg, playerData player) {

        String full = msg.trim().toLowerCase();
        if (full.contains(" ")) {
            msg = full.substring(0, full.indexOf(" "));
        }
        msg = msg.trim();
        if (msg.equals("") || msg.isEmpty()) {
            return false;
        }
        if (msg.startsWith(player.name.toLowerCase())) {
            return false;
        }

        // SINGLES
        if (msg.equals("left")) {
            player.addPacket(data_enum.LEFT, msg);
            current_counts[0]++;
            if (isTroll(player)) {
                current_bot_counts[0]++;
            }
            return true;
        }

        if (msg.equals("right")) {
            player.addPacket(data_enum.RIGHT, msg);
            current_counts[1]++;
            if (isTroll(player)) {
                current_bot_counts[1]++;
            }
            return true;
        }

        if (msg.equals("up")) {
            player.addPacket(data_enum.UP, msg);
            current_counts[2]++;
            if (isTroll(player)) {
                current_bot_counts[2]++;
            }
            return true;
        }

        if (msg.equals("down")) {
            player.addPacket(data_enum.DOWN, msg);
            current_counts[3]++;
            if (isTroll(player)) {
                current_bot_counts[3]++;
            }
            return true;
        }

        if (msg.equals("a")) {
            player.addPacket(data_enum.A, msg);
            current_counts[4]++;
            if (isTroll(player)) {
                current_bot_counts[4]++;
            }
            return true;
        }

        if (msg.equals("b")) {
            player.addPacket(data_enum.B, msg);
            current_counts[5]++;
            if (isTroll(player)) {
                current_bot_counts[5]++;
            }
            return true;
        }

        if (msg.equals("l")) {
            player.addPacket(data_enum.LB, msg);
            current_counts[10]++;
            if (isTroll(player)) {
                current_bot_counts[10]++;
            }
            return true;
        }

        if (msg.equals("r")) {
            player.addPacket(data_enum.RB, msg);
            current_counts[11]++;
            if (isTroll(player)) {
                current_bot_counts[11]++;
            }
            return true;
        }
        if (msg.equals("start")) {
            player.addPacket(data_enum.START, msg);
            current_counts[8]++;
            if (isTroll(player)) {
                current_bot_counts[8]++;
            }
            return true;
        }

        if (msg.equals("select")) {
            player.addPacket(data_enum.SELECT, msg);
            current_counts[9]++;
            if (isTroll(player)) {
                current_bot_counts[9]++;
            }
            return true;
        }

        if (msg.equals("wait")) {
            player.addPacket(data_enum.WAIT, msg);
            current_counts[12]++;
            if (isTroll(player)) {
                current_bot_counts[12]++;
            }
            return true;
        }

        if (msg.equals("start9")) {
            player.addPacket(data_enum.RIOT, msg);
            current_counts[16]++;
            if (isTroll(player)) {
                current_bot_counts[16]++;
            }
            return true;
        }

        if (msg.equals("anarchy")) {
            player.addPacket(data_enum.ANARCHY, msg);
            current_counts[14]++;
            if (isTroll(player)) {
                current_bot_counts[14]++;
            }
            return true;
        }

        if (msg.equals("democracy")) {
            player.addPacket(data_enum.DEMOCRACY, msg);
            current_counts[13]++;
            if (isTroll(player)) {
                current_bot_counts[13]++;
            }
            return true;
        }

        //SEQUENCES
        StringBuilder build = new StringBuilder();

        if (!msg.startsWith("action")) {
            if (isValidSequence(msg, build)) {
                player.addPacket(data_enum.SEQUENCE, build.toString());
                current_counts[15]++;
                if (isTroll(player)) {
                    current_bot_counts[15]++;
                }
                return true;
            }
        }
        //CHAT
        if (full.contains("riot")) {
            player.addPacket(data_enum.RIOT, "");
            current_counts[16]++;
            if (isTroll(player)) {
                current_bot_counts[16]++;
            }
            return true;
        }

        if (isValidYes(full)) {
            player.addPacket(data_enum.YES, "yes");
            chat_type = data_enum.YES;
            if (isValidChat(full)) {
                chat_snippet = full;
            }
            current_counts[18]++;
            if (isTroll(player)) {
                current_bot_counts[18]++;
            }
            return true;
        }

        if (isValidNo(full)) {
            player.addPacket(data_enum.NO, "no");
            chat_type = data_enum.NO;
            if (isValidChat(full)) {
                chat_snippet = full;
            }
            current_counts[19]++;
            if (isTroll(player)) {
                current_bot_counts[19]++;
            }
            return true;
        }

        if (msg.contains("/")) {
            return false;
        }

        player.addPacket(data_enum.CHAT, "");
        if (isValidChat(full)) {
            chat_type = data_enum.CHAT;
            chat_snippet = full;
            if (chat_snippet.startsWith("action")) {
                chat_snippet = chat_snippet.replaceFirst("action", "").trim();
            }
            current_counts[17]++;
            if (isTroll(player)) {
                current_bot_counts[17]++;
            }
            return true;
        }

        return false;
    }

    public boolean isValidChat(String str) {

        if (str.length() > 50) {
            return false;
        }

        for (char c : str.toCharArray()) {
            if (c > 127) {
                return false;
            }
        }

        return str.contains(" ");
    }

    public boolean isValidSequence(String msg, StringBuilder build) {

        if (msg.equals("")) {
            return true;
        }

        if (msg.startsWith("left")) {
            build.append("left");
            return isValidSequence(msg.substring(4), build);
        }

        if (msg.startsWith("right")) {
            build.append("right");
            return isValidSequence(msg.substring(5), build);
        }

        if (msg.startsWith("up")) {
            build.append("up");
            return isValidSequence(msg.substring(2), build);
        }

        if (msg.startsWith("down")) {
            build.append("down");
            return isValidSequence(msg.substring(4), build);
        }

        if (msg.startsWith("a")) {
            build.append("a");
            return isValidSequence(msg.substring(1), build);
        }

        if (msg.startsWith("b")) {
            build.append("b");
            return isValidSequence(msg.substring(1), build);
        }

//        if (msg.startsWith("x")) {
//            build += "x";
//            return isValidSequence(msg.substring(1), build);
//        }
//
//        if (msg.startsWith("y")) {
//            build += "y";
//            return isValidSequence(msg.substring(1), build);
//        }
        if (msg.startsWith("start")) {
            build.append("start");
            return isValidSequence(msg.substring(5), build);
        }

        if (msg.startsWith("select")) {
            build.append("select");
            return isValidSequence(msg.substring(6), build);
        }

        try {
            if (build.length() != 0) {
                if (isNumeric(build.charAt(build.length() - 1) + "")) {
                } else {

                    if (isNumeric(msg.charAt(0) + "")) {
                        build.append(msg.charAt(0) + "");
                        return isValidSequence(msg.substring(1), build);
                    }
                }
            }
        } catch (Exception e) {
            return true;
        }

        if (build.length() > 1) {
            return true;
        }
        return false;

    }

    public boolean isNumeric(String in) {
        try {

            int i = Integer.parseInt(in);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public boolean isValidNo(String str) {
        boolean b = false;
        Pattern p = Pattern.compile("^\\s*n+o+(!|1)*\\s*$");
        b = p.matcher(str).matches();
        p = Pattern.compile("\\bn+o+\\b");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("\\bn+o+\\!*(e+s*)*\\b");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("^d+a+m+n*(i+t+)*(!|1|\\.)*");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("^\\s*d+a+m+n*\\s*(i+t+)*(!+|1+|\\.+)*$");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("\\s*s+o+\\s*c+l+o+s+e+\\s*");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("\\bs+o+\\s+c+l+o+s+e+\\b");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("\\s*c+r+a+p+\\s*");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("\\s*s+h+o+t+\\s*");
        b = b | p.matcher(str).matches();
        return b;
    }

    public boolean isValidYes(String str) {
        boolean b = false;
        Pattern p = Pattern.compile("\\s*y+e+s+\\s*");
        b = p.matcher(str).matches();
        p = Pattern.compile("\\by+(a+|e+)+(s+|a+|h+|y+)+\\b");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("^\\s*w+e+\\s*d+i+d+\\s+i+t+(!|.|1)*");

        b = b | p.matcher(str).matches();
        p = Pattern.compile("\\bwe did it\\b");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("\\b(get)*\\s*rekt\\b");
        b = b | p.matcher(str).matches();
        return b;
    }

}
