package hivemind;

import java.io.BufferedReader;
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

public class threadIRC extends Thread {

    /*
    
     THIS IS MEANT FOR JUSTIN.TV OR TWITCH.TV IRC
    
     THIS IS HIGH-INTENSIVE AND MUST BE THREADED
    
     Class has been condensed to be set-and-forget from its
     previous multi-class form. No other class has access to
     the internals, nor should it.
    
     A bridge must be used and contain the method: proc(String s)
    
     */
    private final bridge myBridge;

    /*
    
     This is still thread non-safe, but collisions are so low it
     should be fine. The intensive time comes from the wait period
     for the next new message to be available and pre-processed.
    
     */
    private String host;
    private int port;
    private String auth;
    private String nick;
    private String chan;

    private PrintStream out;
    private InputStream in;
    private BufferedReader bin;

    private long last_pong;
    private String str_last_pong = "";

    public threadIRC(bridge b) throws IOException {
        this.myBridge = b;
        load();
        connect();
    }

    private void load() {
        Properties props = new Properties();
        InputStream is = null;

        try {
            File f = new File("settings.txt");
            if (!f.exists()) {
                System.exit(0);     //No excuses, just close.
            }
            is = new FileInputStream(f);
        } catch (Exception e) {
            System.exit(0);
        }

        try {
            props.load(is);
        } catch (IOException ex) {
            Logger.getLogger(threadIRC.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(0);
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
        out.println("JOIN" + " " + chan);
    }

    private void register() {
        out.println("PASS" + " " + auth);
        out.println("NICK" + " " + nick);
    }

    private void send(String msg) {
        out.println(msg);
    }

    private void ping() {
        try {
            send("PING : " + last_pong);
        } catch (Exception ex) {
            Logger.getLogger(threadIRC.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {

        //long last_in = 0;
        long stay_alive = 0;
        long snap_time;
        String msg = ""; // Pre-Buffer

        // Thread is persistant, stays active during entire life.
        while (true) {

            // Wrapped to prevent passive unrecoverable crash.
            try {

                // Snap to JVM UTC time
                snap_time = System.currentTimeMillis();

                // Read from IRC
                receive(msg);

                //  Stay-Alive Fail-Safe
                //  Allows miss up to two PONGS
                if (snap_time - last_pong > 25000) {
                    last_pong = snap_time;
                    try {
                        connect();
                    } catch (Exception ex) {
                        Logger.getLogger(threadIRC.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                // Sary-Alive PING
                if (snap_time - stay_alive > 12000) {
                    stay_alive = snap_time;
                    ping();
                }
            } catch (Exception ex) {
                Logger.getLogger(threadIRC.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private boolean receive(String msg) {
        // Optimized to grab dozens of inputs per second.
        try {

            // Magic starts here.
            msg = bin.readLine();

            if (msg != null) {
                // Ignore garbage and get the goods.
                // indexOf uses a char array scanner, O(n)
                if (msg.indexOf("PRIVMSG " + chan) > -1) {

                    //Send to Hive for processing
                    myBridge.proc(msg);

                    //Done
                    return true;
                }

                // Check if PONG is part of garbage.
                if (msg.indexOf(str_last_pong) > -1) {
                    last_pong = System.currentTimeMillis();
                    str_last_pong = last_pong + "";
                }

                // It was garbage, but it was read...
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
