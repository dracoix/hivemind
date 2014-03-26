package hivemind;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javafx.application.ConditionalFeature.SWT;

public class threadIRC extends Thread {

    private String host;
    private int port;
    private String auth;
    private String nick;
    private String chan;

    private Thread t;
    private PrintStream out;
    private InputStream in;
    private BufferedReader bin;

    long last_pong;
    String str_last_pong = "";

    sendOut myChannel;

    boolean connected;

    private hiveCore outHive;

    public threadIRC(hiveCore hive) throws IOException {
        this.outHive = hive;
        load();
        connect();

    }

    public void load() {
        Properties props = new Properties();
        InputStream is = null;

        try {
            File f = new File("settings.txt");
            if (!f.exists()) {

                System.exit(0);

            }
            is = new FileInputStream(f);
        } catch (Exception e) {
            is = null;
            System.exit(0);
        }
        try {
            props.load(is);
        } catch (IOException ex) {
            Logger.getLogger(threadIRC.class.getName()).log(Level.SEVERE, null, ex);
        }

        host = props.getProperty("host");
        port = new Integer(props.getProperty("port"));
        auth = props.getProperty("auth");
        nick = props.getProperty("nick");
        chan = props.getProperty("chan");

    }

    private void connect() throws UnknownHostException, IOException {
        Socket socket = new Socket(host, port);
        out = new PrintStream(socket.getOutputStream());
        in = socket.getInputStream();
        bin = new BufferedReader(new InputStreamReader(in));
        last_pong = System.currentTimeMillis();
        register();
        login();
    }

    private void login() {
        myChannel = getChannel();
    }

    private void register() {
        String localhost = "localhost";
        out.println("PASS" + " " + auth);
        out.println("NICK" + " " + nick);
    }

    public sendOut getChannel() {
        return (new sendOut(chan, out));
    }

    public class sendOut {

        private String name;
        private PrintStream out;

        protected sendOut(String name, PrintStream out) {
            this.name = name;
            this.out = out;
            out.println("JOIN" + " " + chan);
        }

        public void println(String msg) {
            out.println(msg);
        }
    }

    public void ping() {
        myChannel.println("PING : " + last_pong);
    }

    public void run() {

        long last_in = 0;
        long stay_alive = 0;
        long snap_time;
        String msg = "";

        while (true) {
            try {
                snap_time = System.currentTimeMillis();
                if (readAll(msg)) {
                    last_in = snap_time;
                }
                if (snap_time - last_pong > 25000) {
                    last_pong = snap_time;
                    try {
                        connect();
                    } catch (Exception ex) {
                        Logger.getLogger(threadIRC.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (snap_time - stay_alive > 12000) {
                    stay_alive = snap_time;
                    ping();
                }
            } catch (Exception ex) {
                Logger.getLogger(threadIRC.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public boolean readAll(String msg) {
        try {
            msg = bin.readLine();

            if (msg != null) {
                //System.out.println(msg);
                if (msg.indexOf(str_last_pong) > -1) {
                    last_pong = System.currentTimeMillis();
                    str_last_pong = last_pong + "";
                } else {

                    if (msg.indexOf("PRIVMSG " + chan) > -1) {
                        outHive.proc(msg);
                        //System.out.println(msg);
                    }//else{
                    //System.out.println(msg);
                    //}
                }
            }
            return (msg != null);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
