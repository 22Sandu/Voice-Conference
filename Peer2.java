

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Peer2 {

    private final static int port1 = 12345;
    private final static int port2 = 8888;
    public static boolean flag = false;
    public final static Object lock = new Object();

    public static void main(String args[]) {
        
        if (args.length != 1) {                                     // command line input for host(224.0.1.0)
            System.out.println("usage: java DatagramClient host <224.0.1.0>");
            return;
        }
        try {
            ControlSend c = new ControlSend(InetAddress.getByName(args[0]), port1, port2);          // control class to handle which user talks
            c.start();                      
            ControlReceive cc = new ControlReceive(InetAddress.getByName(args[0]), port1, port2);   // control class to allow certain user to talk 
            cc.start();
        } catch (Exception e) {
            Logger.getLogger(Peer2.class.getName()).log(Level.SEVERE, null, e);
        }
    }

}
