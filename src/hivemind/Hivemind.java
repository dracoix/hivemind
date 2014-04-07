package hivemind;

import java.io.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.UnknownHostException;
import javax.swing.*;
import java.lang.System;
import static java.lang.System.nanoTime;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

public class Hivemind {

    static private hiveCore myHive;
    static private bridgeCore myBridge;

    static BufferedImage skin;
    static BufferedImage logo;

    static BufferedImage tpp;

    static BufferedImage clock;
    static BufferedImage metro;
    static BufferedImage halo;

    static window myFrame;
    static BufferedImage dpad;

    static Graphics main_graphics = null;
    //static Thread myGraph = null;
    //static int posX = 0, posY = 0;
    static Font pokefont = new Font(Font.MONOSPACED, Font.BOLD, 16);
    static Font pokefont24 = new Font(Font.MONOSPACED, Font.BOLD, 24);
    static Font pokefont20 = new Font(Font.MONOSPACED, Font.BOLD, 20);
    static Font pokefont16 = new Font(Font.MONOSPACED, Font.BOLD, 16);
    static Font pokefont12 = new Font(Font.MONOSPACED, Font.BOLD, 12);
    static Font pokefont8 = new Font(Font.MONOSPACED, Font.BOLD, 8);

    public static void main(String[] args) throws IOException, InterruptedException {

        try {
            skin = ImageIO.read(new File("hivemon.png"));
        } catch (IOException e) {
            //System.out.println(e.getMessage());
        }

        try {
            clock = ImageIO.read(new File("clock.png"));
        } catch (IOException e) {
            //System.out.println(e.getMessage());
        }

        try {
            metro = ImageIO.read(new File("metron.png"));
        } catch (IOException e) {
            //System.out.println(e.getMessage());
        }

        try {
            halo = ImageIO.read(new File("halo.png"));
        } catch (IOException e) {
            //System.out.println(e.getMessage());
        }

        try {
            pokefont = Font.createFont(Font.TRUETYPE_FONT, new File("pokefont.ttf"));
            pokefont24 = pokefont.deriveFont(24.0f);
            pokefont20 = pokefont.deriveFont(20.0f);
            pokefont16 = pokefont.deriveFont(16.0f);
            pokefont12 = pokefont.deriveFont(12.0f);
            pokefont8 = pokefont.deriveFont(8.0f);

        } catch (Exception ex) {
            Logger.getLogger(Hivemind.class.getName()).log(Level.SEVERE, null, ex);
        }

        myHive = new hiveCore();
        myBridge = new bridgeCore();

        myBridge.t_IRC = new threadIRC(myHive);
        myBridge.t_IRC.setDaemon(true);
        myBridge.t_IRC.start();

        myBridge.t_nTree = new nTree(myHive, myBridge);
        myBridge.t_nTree.setDaemon(true);
        myBridge.t_nTree.start();

        try {
            load();
        } catch (Exception e) {

        }

        myFrame = new window();

        myFrame.setSize(640, 360);
        myFrame.setBackground(Color.black);
        myFrame.setForeground(Color.white);
        myFrame.setUndecorated(true);
        myFrame.setVisible(true);

        BufferStrategy bf;

        sleep(3000);
        myFrame.createBufferStrategy(2);
        bf = myFrame.getBufferStrategy();

        //myPanel.set(true);
        while (true) {
            try {

                main_graphics = bf.getDrawGraphics();
                //myPanel.paint(g);

                mainLoop();

            } catch (Exception e) {
                //System.out.println(e.getMessage());
            } finally {
                if (main_graphics != null) {
                    main_graphics.dispose();
                }
            }
            if (bf != null) {

                bf.show();

            }

        }

    }

    //static long main_old_tick;
    //static long main_dif_tick;
    static long fps_tick;
    //static long ms3_tick;
    //static long sec30_tick;
    static long sec10_tick;
    //static long sec2_tick;
    //static long sec1_tick;

    static long old_left_count;
    static long old_right_count;
    static long old_up_count;
    static long old_down_count;

    static long sys_time;

    static void mainLoop() {
        sys_time = System.currentTimeMillis();

        myHive.tick();

        if ((sys_time - sec10_tick) > 10000) {

            sec10_tick = sys_time;
            save();
        }

        if ((sys_time - fps_tick) > 33) {
            fps_tick = sys_time;
            main_graphics.clearRect(0, 0, 640, 360);

            try {
                drawAll();
            } catch (Exception ex) {
                Logger.getLogger(Hivemind.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    static double oy = 0.0;
    static double ox = 0.0;

    static double ovy = 0.0;
    static double ovx = 0.0;

    static void load() {
        myHive.load();
    }

    static void save() {
        myHive.save();
    }

    static double osanity = 0.0;

    static int true_players;
    static int chat_bots;
    static int trolls;
    static int casuals;
    static int hardcores;
    static int bots;
    static int all_inputs;
    static int bot_inputs;

    static ArrayList<playerData> listBots = new ArrayList();

    static void drawAll() {
        Graphics g = main_graphics;
        myBridge.time_delay = window.newDelay;

        g.drawImage(skin, 0, 0, null);
        //g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        Timestamp timestamp;// = new Timestamp(System.currentTimeMillis() - myBridge.time_delay - myBridge.fuzzy);
        SimpleDateFormat fmt;// = new SimpleDateFormat("HH:mm:ss.SS");

//        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String st;// = (fmt.format(timestamp));
//        st = st.substring(st.indexOf(":") + 1);
//        g.drawString("utc" + st, 16, 28);
//
//        g.drawOval(88, 20, 4, 4);
//        g.drawOval(96, 20, 4, 4);
//        g.drawString("Delay: " + ((double) (int) ((double) (myBridge.time_delay) / 100) / 10) + "s", 112, 28);

        g.setFont(pokefont16);
        timestamp = new Timestamp(System.currentTimeMillis());
        fmt = new SimpleDateFormat("MMMM, dd   HH:mm:ss.SS ");
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        st = (fmt.format(timestamp));
        st = st.subSequence(0, st.lastIndexOf(".")).toString();
        g.drawString(st, 498, 27);

        true_players = 0;
        chat_bots = 0;
        bots = 0;
        trolls = 0;
        hardcores = 0;
        casuals = 0;
        all_inputs = 0;
        bot_inputs = 0;
        listBots = new ArrayList();
        try {
            for (playerData p : myHive.players.values()) {

                if (p.counts_info[0].fast_impactSum > 0) {
                    all_inputs += p.counts_info[0].fast_impactSum;
                    true_players++;
                }
                try {
                    //p.count_all();
                } catch (Exception e) {

                }

                if (p.isSpammer | p.isMetronome) {
                    bot_inputs += p.counts_info[0].fast_impactSum;
                }

                if (p.isBot) {
                    if (p.counts_info[0].fast_impactSum > 0) {
                        bots++;

                        if (p.isMetronome && p.counts_info[4].fast_impactSum > 3) {
                            listBots.add(p);
                        }
                    } else {
                        chat_bots++;
                    }
                    continue;
                }
                if ((p.isTroll) && p.counts_info[0].fast_impactSum > 0) {
                    trolls++;
                    //listBots.add(p);

                    continue;
                }

                if (p.counts_info[0].fast_impactSum > 90) {
                    hardcores++;
                    continue;
                }

                if (p.counts_info[0].fast_impactSum > 0) {
                    casuals++;
                }

            }
        } catch (Exception e) {

        }

        g.setFont(pokefont24);
        g.drawString(true_players + " ", 472, 55);

        g.setColor(new Color(192, 64, 64));
        g.drawString(bots + " ", 472, 87);

        g.setFont(pokefont16);
        g.setColor(new Color(224, 0, 255));
        g.drawString(hardcores + " ", 588, 46);

        g.setColor(new Color(0, 255, 128));
        g.drawString(casuals + " ", 588, 59);

        g.setColor(new Color(255, 128, 0));
        g.drawString(trolls + " ", 588, 76);

        g.setColor(Color.gray);
        //g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        //g.drawString("      ", 80, 352);

        if (myHive.no_fade > Math.sqrt(2)) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            g.setColor(Color.red);
            g.drawString("     NOOO!", randPos(80), randPos(352));
        } else if (myHive.yes_fade > Math.sqrt(2)) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            g.setColor(Color.cyan);
            g.drawString("     YES!!", randPos(80), randPos(352));
        } else {
            g.setFont(pokefont24);
            g.setColor(Color.lightGray);
            g.drawString("         " + myHive.chat_snippet + " ", 80, 352);
        }

        g.setFont(pokefont24);
        g.setColor(Color.white);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        drawBotUI(g, 552, 96, myHive.delta_bot_counts[0] * 3, myHive.delta_bot_counts[1] * 3, myHive.delta_bot_counts[2] * 3, myHive.delta_bot_counts[3] * 3, myHive.delta_bot_counts[8] * 3, myHive.delta_bot_counts[4] * 3, myHive.delta_bot_counts[5] * 3);
        drawRealUI(g, 392, 96, myHive.delta_counts[0] * 3, myHive.delta_counts[1] * 3, myHive.delta_counts[2] * 3, myHive.delta_counts[3] * 3, myHive.delta_counts[8] * 3, myHive.delta_counts[4] * 3, myHive.delta_counts[5] * 3);

        drawSanity(g, 256, 40);
        drawFeed(g, 400, 176);
        drawBotList(g, 400, 176);
        drawInfection(g, 464, 112);
        //drawTPP(g, 24, 312);

        //drawRiot(main_graphics, 720 - 144, 405 - 96);
        drawProb(main_graphics, 24, 56);
        g.setColor(Color.white);

        //ox = kx;
        //oy = ky;
    }

    //static int dpad_x = 32;
    //static int dpad_y = 132;
    //static int ab_x = 192;
    //static int ab_y = 192;
    private static double old_realx;
    private static double old_realy;
    private static double old_botx;
    private static double old_boty;

    private static Polygon arrow(int d, int x, int y) {

        if (d == 0) {
            int xPoly[] = {x, x + 6, x + 12};
            int yPoly[] = {y + 12, y, y + 12};
            return new Polygon(xPoly, yPoly, 3);
        }
        if (d == 1) {
            int xPoly[] = {x, x + 12, x};
            int yPoly[] = {y, y + 6, y + 12};
            return new Polygon(xPoly, yPoly, 3);
        }

        if (d == 2) {
            int xPoly[] = {x, x + 6, x + 12};
            int yPoly[] = {y, y + 12, y};
            return new Polygon(xPoly, yPoly, 3);
        }

        if (d == 3) {
            int xPoly[] = {x, x + 12, x + 12};
            int yPoly[] = {y + 6, y, y + 12};
            return new Polygon(xPoly, yPoly, 3);
        }

        return null;
    }

    private static void drawInfection(Graphics g, int x, int y) {
        String str = (softRound((double) bot_inputs * 100 / (double) all_inputs)) + "%";
        g.setFont(pokefont16);
        g.setColor(Color.red);
        g.drawString("- INFECTION - ", x + 6, y);
        g.setFont(pokefont24);
        int w = (int) g.getFontMetrics(pokefont24).getStringBounds(str, g).getWidth();

        g.drawString(str, x + 40 - w / 2, y + 16);

        str = bot_inputs + "";
        g.setFont(pokefont12);
        w = (int) g.getFontMetrics(pokefont12).getStringBounds(str, g).getWidth();
        g.drawString(str, x + 40 - w - 4, y + 26);

        g.setColor(Color.white);

        g.drawString("/", x + 40 - 2, y + 26);

        g.setColor(Color.cyan);
        str = all_inputs + "";
        //w = (int) g.getFontMetrics(pokefont12).getStringBounds(str, g).getWidth();
        g.drawString(str, x + 40 + 4, y + 26);

        str = softRound(myHive.inputs_per_second) + " ips";
        g.setColor(Color.white);
        w = (int) g.getFontMetrics(pokefont16).getStringBounds(str, g).getWidth();
        g.setFont(pokefont16);
        g.drawString(str, x + 40 - w / 2, y + 46);

    }

    private static void drawBotList(Graphics g, int x, int y) {

        if (listBots.isEmpty()) {
            return;
        }

        g.setFont(pokefont12);

        playerData p;
        double w = 0.0;
        double[] widths = new double[listBots.size()];
        for (int i = 0; i < listBots.size(); i++) {
            p = listBots.get(i);

            widths[i] = g.getFontMetrics(pokefont12).getStringBounds(pad(softRound(p.bot_info[4].mT) + "", 6) + "" + pad(softRound(p.bot_info[4].sT) + "", 6) + "" + p.name, g).getWidth() + 16;
            if (widths[i] > w) {
                w = widths[i];
            }
        }
        g.setColor(Color.black);
        g.fillRoundRect(x + 104 - ((int) w + 4) / 2, y, (int) w + 4, listBots.size() * 10 + 4, 8, 8);
        g.setColor(Color.red);
        g.drawRoundRect(x + 104 - ((int) w + 4) / 2, y, (int) w + 4, listBots.size() * 10 + 4, 8, 8);
        //g.setColor(Color.red);
        for (int i = 0; i < listBots.size(); i++) {
            p = listBots.get(i);
//            g.setColor(new Color(255, 128, 0));
//            if (p.isBot) {
//                g.setColor(Color.red);
//            }
            if (p.bot_info[4].isMetronome) {
                g.drawString("Δ " + pad(softRound(p.bot_info[4].mT) + "", 6) + "" + pad(softRound(p.bot_info[4].sT) + "", 6) + "" + p.name, x + (208 - (int) w) / 2 + 6, y + (i + 1) * 10);

//                g.drawImage(metro, x + (208 - (int) w) / 2, y + (i + 1) * 12 - 9, null);
            } else {
                g.drawString("Θ " + pad(softRound(p.bot_info[4].mT) + "", 6) + "" + pad(softRound(p.bot_info[4].sT) + "", 6) + "" + p.name, x + (208 - (int) w) / 2 + 6, y + (i + 1) * 10);

//                g.drawImage(clock, x + (208 - (int) w) / 2, y + (i + 1) * 12 - 9, null);
            }
//            g.drawString("Θ " + pad(softRound(p.bot_info[4].mT) +"",6) + "" + pad(softRound(p.bot_info[4].sT)+"",6) + "" + p.name, x + (208 - (int) w) / 2 + 16, y + (i + 1) * 12);
        }

    }

    static String pad(String str, int len) {

        return str + (new String(new char[len - str.length() + 1]).replace('\0', ' '));

    }

    private static void drawRealUI(Graphics g, int x, int y, double l, double r, double u, double d, double s, double a, double b) {
        int w = 64;
        int h = 64;

        double dsum = (l + r + u + d);
        double absum = (a + b + s);

        double px = ((r - l));
        double py = ((d - u));
        double ph = Math.sqrt(px * px + py * py);
        if (ph == 0) {
            ph = 1;
        }
        px /= ph;
        py /= ph;

        px *= w / 2;
        py *= h / 2;

        px = (old_realx + old_realx + px);
        px /= 3;

        py = (old_realy + old_realy + py);
        py /= 3;

        ph = Math.sqrt(px * px + py * py);
        if (ph == 0) {
            ph = 1;
        }
        px /= ph;
        py /= ph;

        px *= w / 2;
        py *= h / 2;

        int fpx = (int) px;
        int fpy = (int) py;
        if (!(fpx == 0 && fpy == 0)) {

            old_realx = px;
            old_realy = py;
        } else {
            fpx = (int) old_realx;
            fpy = (int) old_realy;

        }

        if (Double.isNaN(old_realy)) {
            old_realy = 0;
        }
        if (Double.isNaN(old_realx)) {
            old_realx = 0;
        }

        g.setColor(Color.GREEN);
        g.fillOval(x + fpx + w / 2 - 4, y + fpy + h / 2 - 4, 8, 8);
        g.setColor(Color.black);
        g.fillOval(x + fpx + w / 2 - 4 + 2, y + fpy + h / 2 - 4 + 2, 4, 4);

        int grad = (int) (15 + (u / dsum) * 240);
        g.setColor(new Color(0, grad / 2, grad));
        g.fillPolygon(arrow(0, x + w / 2 - 6, y + 2));
        //g.fillRoundRect(x + w / 2 - 4, y + 4, 8, 8, 4, 4);

        grad = (int) (15 + (r / dsum) * 240);
        g.setColor(new Color(0, grad / 2, grad));
        g.fillPolygon(arrow(1, x + w - 14, y + h / 2 - 6));
        //g.fillRoundRect(x + w - 12, y + h / 2 - 4, 8, 8, 4, 4);

        grad = (int) (15 + (l / dsum) * 240);
        g.setColor(new Color(0, grad / 2, grad));
        g.fillPolygon(arrow(3, x + 2, y + h / 2 - 6));
        //g.fillRoundRect(x + 4, y + h / 2 - 4, 8, 8, 4, 4);

        grad = (int) (15 + (d / dsum) * 240);
        g.setColor(new Color(0, grad / 2, grad));
        g.fillPolygon(arrow(2, x + w / 2 - 6, y + h - 14));
        //g.fillRoundRect(x + w / 2 - 4, y + h - 12, 8, 8, 4, 4);

        drawRealABS(g, x + w / 2 - 13, y + h / 2 - 12, grad, a, b, s, absum);
    }

    private static void drawBotUI(Graphics g, int x, int y, double l, double r, double u, double d, double s, double a, double b) {
        int w = 64;
        int h = 64;

        double dsum = (l + r + u + d);
        double absum = (a + b + s);

        double px = ((r - l));
        double py = ((d - u));
        double ph = Math.sqrt(px * px + py * py);
        if (ph == 0) {
            ph = 1;
        }
        px /= ph;
        py /= ph;

        px *= w / 2;
        py *= h / 2;

        px = (old_botx + old_botx + px);
        px /= 3;

        py = (old_boty + old_boty + py);
        py /= 3;

        ph = Math.sqrt(px * px + py * py);
        if (ph == 0) {
            ph = 1;
        }
        px /= ph;
        py /= ph;

        px *= w / 2;
        py *= h / 2;

        int fpx = (int) px;
        int fpy = (int) py;
        if (!(fpx == 0 && fpy == 0)) {

            old_botx = px;
            old_boty = py;
        } else {
            fpx = (int) old_botx;
            fpy = (int) old_boty;

        }

        if (Double.isNaN(old_boty)) {
            old_boty = 0;
        }
        if (Double.isNaN(old_botx)) {
            old_botx = 0;
        }

        g.setColor(Color.RED);
        g.fillOval(x + fpx + w / 2 - 4, y + fpy + h / 2 - 4, 8, 8);
        g.setColor(Color.black);
        g.fillOval(x + fpx + w / 2 - 4 + 2, y + fpy + h / 2 - 4 + 2, 4, 4);

        int grad = (int) (15 + (u / dsum) * 240);
        g.setColor(new Color(grad, grad / 2, 0));
        g.fillPolygon(arrow(0, x + w / 2 - 6, y + 2));
        //g.fillRoundRect(x + w / 2 - 4, y + 4, 8, 8, 4, 4);

        grad = (int) (15 + (r / dsum) * 240);
        g.setColor(new Color(grad, grad / 2, 0));
        g.fillPolygon(arrow(1, x + w - 14, y + h / 2 - 6));
        //g.fillRoundRect(x + w - 12, y + h / 2 - 4, 8, 8, 4, 4);

        grad = (int) (15 + (l / dsum) * 240);
        g.setColor(new Color(grad, grad / 2, 0));
        g.fillPolygon(arrow(3, x + 2, y + h / 2 - 6));
        //g.fillRoundRect(x + 4, y + h / 2 - 4, 8, 8, 4, 4);

        grad = (int) (15 + (d / dsum) * 240);
        g.setColor(new Color(grad, grad / 2, 0));
        g.fillPolygon(arrow(2, x + w / 2 - 6, y + h - 14));
        //g.fillRoundRect(x + w / 2 - 4, y + h - 12, 8, 8, 4, 4);

        drawBotABS(g, x + w / 2 - 13, y + h / 2 - 12, grad, a, b, s, absum);
    }

    static void drawRealABS(Graphics g, int x, int y, int grad, double a, double b, double s, double absum) {
        if ((int) absum == 0) {
            absum = 0;
        }
        g.setFont(pokefont12);
        grad = (int) (15 + (b / absum) * 240);
        g.setColor(new Color(0, grad, grad / 2));
        g.fillOval(x, y, 12, 12);
        g.setColor(Color.DARK_GRAY);
        g.drawString("B", x + 4, y + 10);

        grad = (int) (15 + (a / absum) * 240);
        g.setColor(new Color(0, grad, grad / 2));
        g.fillOval(x + 14, y, 12, 12);
        g.setColor(Color.DARK_GRAY);
        g.drawString("A", x + 18, y + 10);

        grad = (int) (15 + (s / absum) * 240);
        g.setColor(new Color(grad, 0, grad / 3));
        g.fillRoundRect(x, y + 14, 26, 12, 4, 4);
        g.setColor(Color.DARK_GRAY);
        g.drawString("START", x + 2, y + 24);
    }

    static void drawBotABS(Graphics g, int x, int y, int grad, double a, double b, double s, double absum) {
        if ((int) absum == 0) {
            absum = 0;
        }
        g.setFont(pokefont12);
        grad = (int) (15 + (b / absum) * 240);
        g.setColor(new Color(grad, grad / 2, 0));
        g.fillOval(x, y, 12, 12);
        g.setColor(Color.DARK_GRAY);
        g.drawString("B", x + 4, y + 10);

        grad = (int) (15 + (a / absum) * 240);
        g.setColor(new Color(grad, grad / 2, 0));
        g.fillOval(x + 14, y, 12, 12);
        g.setColor(Color.DARK_GRAY);
        g.drawString("A", x + 18, y + 10);

        grad = (int) (15 + (s / absum) * 240);
        g.setColor(new Color(grad, grad / 3, 0));
        g.fillRoundRect(x, y + 14, 26, 12, 4, 4);
        g.setColor(Color.DARK_GRAY);
        g.drawString("START", x + 2, y + 24);
    }

    private static void drawSanity(Graphics g, int x, int y) {

        if (Double.isInfinite(osanity) || Double.isNaN(osanity)) {
            osanity = 0;
        }
        g.setColor(Color.darkGray);
        g.drawRect(x, y, 128, 16);
        double avg = 1 - (Math.log(myBridge.cells_filled + 1) / Math.log(90.0));//helix * helix + (dome * dome);
        //avg = Math.sqrt(avg);

        //avg = (dome) / (avg + 1);
        avg *= 255;
        avg = osanity + avg;
        avg /= 2;
        //avg = (osanity * 29 + avg) / 30;
        g.setColor((new Color((int) ((255 - avg)), (int) (avg), 0)));
        g.fillRect(x + 1, y + 1, (int) ((avg * 125 / 255) + 1), 14);
        osanity = avg;
    }

    static double copyxy[][] = new double[15][10];
    static double oldxy[][] = new double[15][10];
    static double top_calc = 1;

    static void drawProb(Graphics g, int px, int py) {
        double c = 0;
        top_calc = 0;
        try {
            copyxy = myBridge.realxy.clone();
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 15; x++) {

                    if (copyxy[x][y] > top_calc) {
                        top_calc = copyxy[x][y];

                    }

                }

            }

        } catch (Exception e) {

        }
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 15; x++) {
                //System.out.print((int) xy[x][y] + ", ");
                //c = 254 * (myHive.xy[x][y] * myHive.xy[x][y]) / (myHive.top_calc * myHive.top_calc);
                if (Double.isNaN(oldxy[x][y])) {
                    oldxy[x][y] = 0;
                }
                c = 255 * (copyxy[x][y] / top_calc);
                if (c > 254) {
                    c = 255;
                }
                c += (oldxy[x][y] * 3);
                c /= 4;
                if (c > 254) {
                    c = 255;
                }
                oldxy[x][y] = c;
                g.setColor(new Color(255, 0, 255));
                oldxy[x][y] *= 0.97;
//                if ((int) c > 192){g.setColor(new Color(0, (int) c, (int) c));}else{
//                    g.setColor(new Color(0, 0, 0));
//                }
                g.setColor(new Color(0, (int) c, (int) c));
                if ((int) c == 0) {
                    g.setColor(new Color(96, 0, 0));
                } else {

                }
                g.fillRect(px + (x * 24), py + (y * 24), 23, 23);
                g.setColor(Color.darkGray);
                g.drawRect(px + (x * 24), py + (y * 24), 23, 23);

            }

        }

//        g.setColor(Color.white);
//        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 10));
//        g.drawString("Depth: " + (int) myBridge.depth, px, py + 9 * 36 + 10);
//        if (myBridge.rez < 0) {
//            myBridge.rez = 100.0;
//        }
//        g.drawString("Rez: " + (float) Math.round(myBridge.rez * 100f) / 100.0f + "%", px + 2 * 36, py + 9 * 36 + 10);
//        g.drawString("Possible Moves: " + myBridge.prob_moves, px + 4 * 36, py + 9 * 36 + 10);
        //g.setColor(new Color(255, 0, 255));
        //c = 254 * (myHive.xy[4][4]) / myHive.top_calc;
        g.setColor(new Color(0, 127 + ((int) c / 2), 127 + ((int) c / 2)));
        g.fillRect(px + (7 * 24), py + (5 * 24), 23, 23);
        g.setColor(new Color(0, 191 + ((int) c / 4), 191 + ((int) c / 4)));
        g.drawRect(px + (7 * 24), py + (5 * 24), 23, 23);

    }

    public static void drawFeed(Graphics g, int x, int y) {
        y = y + 8;
        feedPacket fp;
        g.setFont(pokefont16);
        double w;
        for (int i = 0; i < myHive.tempFeed.size(); i++) {
            fp = myHive.tempFeed.get(i);
            switch (fp.type) {
                case CASUAL:
                    if (myHive.inputs_per_second > 12) {
                        g.setColor(new Color(32, 144, 255));
                    } else {
                        g.setColor(new Color(0, 255, 128));
                    }
                    break;
                case HARDCORE:
                    if (myHive.inputs_per_second > 12) {
                        g.setColor(new Color(32, 160, 255));
                    } else {
                        g.setColor(new Color(224, 0, 255));
                    }
                    break;
                case METRONOME:
                    g.setColor(new Color(192, 64, 64));
                    g.drawImage(metro, x - 4, y + i * 10 - 8, null);
                    break;
                case SPAMMER:
                    g.setColor(new Color(255, 112, 0));
                    g.drawImage(clock, x - 4, y + i * 10 - 8, null);
                    break;
                case HELPER:
                    g.setColor(new Color(224, 224, 224));
                    g.drawImage(halo, x - 4, y + i * 10 - 8, null);
                    break;
                case TROLL:
                    if (myHive.inputs_per_second > 12) {
                        g.setColor(new Color(144, 144, 160));
                    } else {
                        g.setColor(new Color(255, 128, 0));
                    }
                    break;
            }

            g.drawString(fp.name, x + 10, y + i * 10);

            w = g.getFontMetrics(pokefont16).getStringBounds(fp.command.toUpperCase(), g).getWidth();
            g.drawString(fp.command.toUpperCase(), (x + 208) - (int) w, y + i * 10);

        }
    }

    static double oldriot;
    static double riot_dif;
    static double oriot_dif;
    static long rtick;

    public static double softRound(double i) {
        return (double) Math.round(i * 10) / 10;
    }

    private static void drawTPP(Graphics g, int x, int y) {
        if (myHive.official.isEmpty()) {
            return;
        }
        String msg = myHive.official.get(myHive.official.size() - 1);
        msg = msg.substring(msg.indexOf(":") + 2);
        String words[] = msg.split(" ");

        String lines[] = new String[3];
        int i = 0;
        g.drawImage(tpp, x, y - 7, null);
        g.setFont(new Font(Font.SERIF, Font.ITALIC, 12));
        g.setColor(Color.yellow);
        lines[0] = "";
        lines[1] = "";
        lines[2] = "";
        for (String w : words) {
            lines[i] = lines[i] + w + " ";
            if (lines[i].length() > 60) {

                i++;
            }
        }

        for (i = 0; i < 3; i++) {
            g.drawString(lines[i], x + 34, y + i * 12);
        }

    }

    static int randPos(int x) {
        return x + (int) ((Math.random() * 6.0) - 3);
    }

}
