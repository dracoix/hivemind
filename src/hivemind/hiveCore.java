package hivemind;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

public class hiveCore implements bridge {

    // Main butchery of code is contained in this class.
    static final int dump_limit = 1;

    boolean failLoad = true;
    public Hashtable<String, playerData> players = new Hashtable();
    public Hashtable<String, playerData> clone_players = new Hashtable();
    public ArrayList<String> official = new ArrayList();

    //public statDump massDump = new statDump();
    data_enum chat_type;
    String chat_snippet;

    long[] current_counts = new long[20];
    long[] delta_counts = new long[20];
    long[] old_counts = new long[20];

    double inputs_per_second;

    long[] current_bot_counts = new long[20];
    long[] delta_bot_counts = new long[20];
    long[] old_bot_counts = new long[20];

    double[] repaid_old_delta_count = new double[20];
    double[] rapid_delta_count = new double[20];
    double[] rapid_delta_weight = new double[20];

    double player_weight[] = new double[20];

    ArrayList<feedPacket> tempFeed = new ArrayList();
    long otick, ntick, rapid_tick, ips_tick;

    double no_fade;
    double yes_fade;
    double old_no_fade;
    double old_yes_fade;

    double _ips;

    public void tick() {

        if (System.currentTimeMillis() - otick < 333) {
            return;
        }
        otick = System.currentTimeMillis();

        for (int i = 0; i < 20; i++) {
            delta_bot_counts[i] = current_bot_counts[i] - old_bot_counts[i];
            delta_counts[i] = current_counts[i] - old_counts[i];

            _ips += delta_counts[i];

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

        if (System.currentTimeMillis() - ips_tick < 1000) {
            return;
        }
        ips_tick = System.currentTimeMillis();

        inputs_per_second = (inputs_per_second + _ips) / 2;
        _ips = 0;

        if (System.currentTimeMillis() - rapid_tick < 14062) {
            return;
        }
        rapid_tick = System.currentTimeMillis();

        for (int i = 0; i < 15; i++) {

            rapid_delta_count[i] = (current_counts[i] - current_bot_counts[i]) - repaid_old_delta_count[i];

            repaid_old_delta_count[i] = (current_counts[i] - current_bot_counts[i]);
        }

        double tmp = 0;

        for (int i = 0; i < 15; i++) {
            tmp = tmp + rapid_delta_count[i] * rapid_delta_count[i];
        }

        tmp = Math.sqrt(tmp);

        if (tmp == 0) {
            return;
        }

        for (int i = 0; i < 15; i++) {
            rapid_delta_weight[i] = 1.5 - (rapid_delta_count[i] / tmp);
            rapid_delta_weight[i] *= rapid_delta_weight[i];
            rapid_delta_weight[i] *= rapid_delta_weight[i];
            rapid_delta_weight[i] *= rapid_delta_weight[i];
        }

        player_weight = rapid_delta_weight.clone();

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
            //massDump.save();
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

        //massDump.load();
    }

    public void dump() {
        ArrayList<playerData> full_dumps = new ArrayList();

        Iterator<playerData> i = players.values().iterator();
        //playerData p;
        long min20 = 20 * 60 * 1000;

        clone_players = (Hashtable<String, playerData>) players.clone();

        //microdataStats stattmp = new microdataStats();
        try {
            //statsData tmpStats;
            for (playerData p : clone_players.values()) {
                try {
                    //tmpStats = new statsData(p.counts_info[0].counts, 0, 16);
                    //p.count_all();
                    if ((System.currentTimeMillis() - p.last_tick) < min20) {
                        if ((p.counts_info[0].fast_impactSum >= dump_limit)) {
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

        try {

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
                    + "OF THE " + (int) players.size() + " USERS... " + (int) full_dumps.size() + " HAVE AT LEAST " + dump_limit + " INPUTS\n\n"
                    + "IN\tΘ60\tΘ15\tΘ2\tLEFT\tRIGHT\tUP\tDOWN\tA\tB\tSt\tSel\tL\tR\tWait\tDemo\tAnarch\tRiot\tChat\tYes\tNo\tσ\t(σ-μ)\tμT\tσT\tUSERNAME\n");
            String buildit;
            statsData tmpStats;
            float tmp;
            for (playerData e : full_dumps) {
                count = e.counts_info[0];
                buildit = count.fast_impactSum + ""
                        + "\t" + (int) e.bot_info[0].tocks
                        + "\t" + (int) e.bot_info[2].tocks
                        + "\t" + (int) e.bot_info[5].tocks;
                for (int n = 0; n < 6; n++) {
                    tmp = (float) Math.round(count.counts[n] * 10) / 10;
                    if (tmp > 0) {
                        buildit += "\t" + tmp;
                    } else {
                        buildit += "\t    ";
                    }
                }

                for (int n = 8; n < 12; n++) {
                    tmp = (float) Math.round(count.counts[n] * 10) / 10;
                    if (tmp > 0) {
                        buildit += "\t" + tmp;
                    } else {
                        buildit += "\t    ";
                    }
                }

                for (int n = 12; n < 15; n++) {
                    buildit += "\t" + (int) Math.round(count.counts[n] * 10) / 10;
                }

                for (int n = 16; n < 20; n++) {
                    buildit += "\t" + (int) Math.round(count.counts[n] * 10) / 10;
                }
                tmpStats = new statsData(e.counts_info[0].counts, 0, 16);
                buildit
                        += "\t" + (float) Math.round(tmpStats.std_dev_p * 10) / 10
                        + "\t" + (float) Math.round(Math.abs((float) Math.round(tmpStats.std_dev_p * 10) / 10 - (float) Math.round(tmpStats.mean * 10) / 10) * 10) / 10
                        + "\t" + (float) Math.round(e.bot_info[0].mT * 10) / 10
                        + "\t" + (float) Math.round(e.bot_info[0].sT * 10) / 10;

                writer.write(buildit
                        + "\t" + e.name + "\n");

            }
            writer.close();
        } catch (Exception e) {

        }

        try {

            File f = new java.io.File("tpp_track.txt");
            FileWriter fr = new FileWriter(f);
            BufferedWriter writer = new BufferedWriter(fr);

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
            data_enum.START, data_enum.LB, data_enum.RB, data_enum.ANARCHY, data_enum.DEMOCRACY,
            data_enum.SEQUENCE, data_enum.WAIT);

    public void proc(String full) {

        int itmp;
        itmp = full.indexOf("!");
        if (itmp < 0) {
            return;
        }
        String name = full.substring(1, itmp).trim().toLowerCase();

        itmp = full.indexOf(" :");
        if (itmp < 0) {
            return;
        }
        String msg = full.substring(itmp + 2).trim().toLowerCase();

        // NEVER USE TOLOWERCASE() AGAIN!
        if (name.equals("twitchplayspokemon")) {
            official.add(System.currentTimeMillis() + "> " + name + ": " + msg);
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
            if (!usr.isTroll) {
                fp.type = player_enum.HELPER;
            }
        }

        usr.last_msg = "";

        if (isValidMessage(msg, usr)) {

            players.put(name, usr);

            if (impactFeed.contains(usr.inputs.get(usr.inputs.size() - 1).type)) {
                fp.cmd_type = usr.inputs.get(usr.inputs.size() - 1).type;
                fp.command = convSeq(usr.last_msg.trim());
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

    public boolean isTroll(playerData p) {
        return (p.isTroll | p.isBot);
    }

    public boolean isValidMessage(String msg, playerData player) {

        String full = msg.trim();
        int itmp = full.indexOf(" ");
        if (itmp > -1) {
            msg = full.substring(0, itmp);
        }
        //msg = msg.trim();
        if (msg.isEmpty()) {
            return false;
        }

//        if (msg.startsWith(player.name)) {
//            return false;
//        }
        // SINGLES
        if (msg.equals("left")) {
            player.addPacket(data_enum.LEFT, msg, player_weight);
            current_counts[0]++;
            if (isTroll(player)) {
                current_bot_counts[0]++;
            }
            return true;
        }

        if (msg.equals("right")) {
            player.addPacket(data_enum.RIGHT, msg, player_weight);
            current_counts[1]++;
            if (isTroll(player)) {
                current_bot_counts[1]++;
            }
            return true;
        }

        if (msg.equals("up")) {
            player.addPacket(data_enum.UP, msg, player_weight);
            current_counts[2]++;
            if (isTroll(player)) {
                current_bot_counts[2]++;
            }
            return true;
        }

        if (msg.equals("down")) {
            player.addPacket(data_enum.DOWN, msg, player_weight);
            current_counts[3]++;
            if (isTroll(player)) {
                current_bot_counts[3]++;
            }
            return true;
        }

        if (msg.equals("a")) {
            player.addPacket(data_enum.A, msg, player_weight);
            current_counts[4]++;
            if (isTroll(player)) {
                current_bot_counts[4]++;
            }
            return true;
        }

        if (msg.equals("b")) {
            player.addPacket(data_enum.B, msg, player_weight);
            current_counts[5]++;
            if (isTroll(player)) {
                current_bot_counts[5]++;
            }
            return true;
        }

        if (msg.equals("l")) {
            player.addPacket(data_enum.LB, msg, player_weight);
            current_counts[10]++;
            if (isTroll(player)) {
                current_bot_counts[10]++;
            }
            return true;
        }

        if (msg.equals("r")) {
            player.addPacket(data_enum.RB, msg, player_weight);
            current_counts[11]++;
            if (isTroll(player)) {
                current_bot_counts[11]++;
            }
            return true;
        }
        if (msg.equals("start")) {
            player.addPacket(data_enum.START, msg, player_weight);
            current_counts[8]++;
            if (isTroll(player)) {
                current_bot_counts[8]++;
            }
            return true;
        }

        if (msg.equals("select")) {
            player.addPacket(data_enum.SELECT, msg, player_weight);
            current_counts[9]++;
            if (isTroll(player)) {
                current_bot_counts[9]++;
            }
            return true;
        }

        if (msg.equals("wait")) {
            player.addPacket(data_enum.WAIT, msg, player_weight);
            current_counts[12]++;
            if (isTroll(player)) {
                current_bot_counts[12]++;
            }
            return true;
        }

        if (msg.equals("start9")) {
            player.addPacket(data_enum.RIOT, msg, player_weight);
            current_counts[16]++;
            if (isTroll(player)) {
                current_bot_counts[16]++;
            }
            return true;
        }

        if (msg.equals("anarchy")) {
            player.addPacket(data_enum.ANARCHY, msg, player_weight);
            current_counts[14]++;
            if (isTroll(player)) {
                current_bot_counts[14]++;
            }
            return true;
        }

        if (msg.equals("democracy")) {
            player.addPacket(data_enum.DEMOCRACY, msg, player_weight);
            current_counts[13]++;
            if (isTroll(player)) {
                current_bot_counts[13]++;
            }
            return true;
        }

        //SEQUENCES
        ArrayList<data_enum> build = new ArrayList();

        if (!msg.startsWith("action")) {
            if (msg.contains("+") || msg.contains(",")) {
                if (isValidSequence(msg, build)) {
                    player.addPacket(data_enum.SEQUENCE, SeqToString(build), build, player_weight);
                    current_counts[15]++;
                    if (isTroll(player)) {
                        current_bot_counts[15]++;
                    }
                    return true;
                }
            }
        }
        //CHAT
        if (full.contains("riot")) {
            player.addPacket(data_enum.RIOT, "", player_weight);
            current_counts[16]++;
            if (isTroll(player)) {
                current_bot_counts[16]++;
            }
            return true;
        }

        if (isValidYes(full)) {
            player.addPacket(data_enum.YES, "yes", player_weight);
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
            player.addPacket(data_enum.NO, "no", player_weight);
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

        player.addPacket(data_enum.CHAT, "", player_weight);
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

    public boolean isValidSequence(String msg, ArrayList<data_enum> build) {
        msg = msg + " ";
        if (msg.trim().equals("")) {
            return true;
        }

        if (msg.startsWith("+") && build.size() > 0) {
            return isValidSequence(msg.substring(1), build);
        }

        if (msg.startsWith(",") && build.size() > 0) {
            return isValidSequence(msg.substring(1), build);
        }

        if (msg.startsWith("left")) {

            build.add(data_enum.LEFT);

            return isValidSequence(msg.substring(4), build);
        }

        if (msg.startsWith("right")) {

            build.add(data_enum.RIGHT);

            return isValidSequence(msg.substring(5), build);
        }

        if (msg.startsWith("up")) {

            build.add(data_enum.UP);

            return isValidSequence(msg.substring(2), build);
        }

        if (msg.startsWith("down")) {

            build.add(data_enum.DOWN);

            return isValidSequence(msg.substring(4), build);
        }

        if (msg.startsWith("a")) {
            if (!build.contains(data_enum.A)) {
                build.add(data_enum.A);
            } else {
                return true;
            }
            return isValidSequence(msg.substring(1), build);
        }

        if (msg.startsWith("b")) {
            if (!build.contains(data_enum.B)) {
                build.add(data_enum.B);
            } else {
                return true;
            }
            return isValidSequence(msg.substring(1), build);
        }

        if (msg.startsWith("l")) {
            if (!build.contains(data_enum.LB)) {
                build.add(data_enum.LB);
            } else {
                return true;
            }
            return isValidSequence(msg.substring(1), build);
        }

        if (msg.startsWith("r")) {
            if (!build.contains(data_enum.RB)) {
                build.add(data_enum.RB);
            } else {
                return true;
            }
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
            if (!build.contains(data_enum.START)) {
                build.add(data_enum.START);
            } else {
                return true;
            }
            return isValidSequence(msg.substring(5), build);
        }

        if (msg.startsWith("select")) {
            if (!build.contains(data_enum.SELECT)) {
                build.add(data_enum.SELECT);
            } else {
                return true;
            }
            return isValidSequence(msg.substring(6), build);
        }

//        try {
//            if (build.length() != 0) {
//                if (isNumeric(build.charAt(build.length() - 1) + "")) {
//                } else {
//
//                    if (isNumeric(msg.charAt(0) + "")) {
//                        build.append(msg.charAt(0) + "");
//                        return isValidSequence(msg.substring(1), build);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            return true;
//        }
        if (build.size() > 0) {
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

    private String SeqToString(ArrayList<data_enum> s) {
        String k = "";
        for (data_enum e : s) {
            k += e.getString();
        }
        return k;
    }

    public boolean isValidNo(String s) {

        String str = cleanStr(s);

        // Needs Cleanup
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

    public boolean isValidYes(String s) {

        String str = cleanStr(s);

        // Needs Cleanup
        boolean b = false;
        Pattern p;
        p = Pattern.compile("\\by+(a+|e+)[sahy]+");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("^\\s*w+e+\\s*d+i+d+\\s+i+t+(!|.|1)*");

        b = b | p.matcher(str).matches();
        p = Pattern.compile("\\bwe did it\\b");
        b = b | p.matcher(str).matches();
        p = Pattern.compile("\\b(get)*\\s*rekt\\b");
        b = b | p.matcher(str).matches();
        return b;
    }

    private static String convSeq(String msg) {

        return msg.replaceAll("left", "←").replaceAll("right", "→").replaceAll("up", "↑").replaceAll("down", "↓");
    }

    private String cleanStr(String s) {
        char a = 0, b = 0;
        String o = "";
        for (char c : s.toCharArray()) {
            if ((c != a) || (c != b)) {
                o += c;
            }
            a = b;
            b = c;
        }
        return o;
    }

}
